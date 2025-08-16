use crate::server::sequence::{Sequence, Trig};
use midir::MidiOutput;
use midir::MidiOutputConnection;
use std::collections::HashMap;
use std::sync::{Arc, Mutex, mpsc};
use std::thread;
use std::time::{Duration, Instant};

// Define a trait for step handling
pub enum StepEvent {
    NOTE_ON,
    NOTE_OFF,
}

pub trait StepHandler: Send + Sync + 'static {
    fn handle_steps(&self, trigs: Vec<&Trig>, event: StepEvent);
}

// Generic sequencer that accepts any step handler
#[derive(Debug)]
pub struct Sequencer<H: StepHandler> {
    state: Arc<Mutex<SequencerState>>,
    playback_control: Option<mpsc::Sender<PlaybackCommand>>,
    playback_handle: Option<thread::JoinHandle<()>>,
    step_handler: Arc<H>,
}

#[derive(Debug, Default)]
struct SequencerState {
    current_sequence: Option<Sequence>,
    cued_sequence: Option<Sequence>,
    playing: bool,
    current_step: u32,
}

#[derive(Debug)]
enum PlaybackCommand {
    Start(Sequence),
    Stop,
    Swap(Sequence),
    Shutdown,
}

// Your existing result types remain the same
#[derive(Debug, Clone)]
pub struct OperationResult<T = ()> {
    pub success: bool,
    pub data: Option<T>,
}

#[derive(Debug, Clone)]
pub struct CueMetadata {
    pub replaced_existing: bool,
    pub remaining_steps: u32,
}

#[derive(Debug, Clone)]
pub struct SwapMetadata {
    pub replaced_existing: bool,
}

#[derive(Debug, Clone)]
pub struct StopMetadata {
    pub trig_count: Option<usize>,
}

pub type CueResult = OperationResult<CueMetadata>;
pub type StartResult = OperationResult<()>;
pub type StopResult = OperationResult<StopMetadata>;
pub type SwapResult = OperationResult<SwapMetadata>;

impl<T> OperationResult<T> {
    pub fn success(data: T) -> Self {
        Self {
            success: true,
            data: Some(data),
        }
    }

    pub fn success_empty() -> OperationResult<()> {
        OperationResult {
            success: true,
            data: Some(()),
        }
    }

    pub fn failure() -> Self {
        Self {
            success: false,
            data: None,
        }
    }

    pub fn is_success(&self) -> bool {
        self.success
    }

    pub fn is_failure(&self) -> bool {
        !self.success
    }
}

impl<H: StepHandler> Sequencer<H> {
    pub fn new(step_handler: H) -> Self {
        Self {
            state: Arc::new(Mutex::new(SequencerState::default())),
            playback_control: None,
            playback_handle: None,
            step_handler: Arc::new(step_handler),
        }
    }

    /// Initialize the high-priority playback system
    pub fn initialize_playback(&mut self) {
        let (tx, rx) = mpsc::channel();
        self.playback_control = Some(tx);

        let state = Arc::clone(&self.state);
        let step_handler = Arc::clone(&self.step_handler);

        // Spawn dedicated OS thread for timing-critical playback
        let handle = thread::Builder::new()
            .name("sequencer-playback".to_string())
            .spawn(move || {
                #[cfg(target_os = "linux")]
                {
                    // You could use thread_priority crate here for cross-platform priority setting
                }

                Self::playback_loop(state, rx, step_handler);
            })
            .expect("Failed to spawn playback thread");

        self.playback_handle = Some(handle);
    }

    /// High-precision playback loop running on dedicated thread
    fn playback_loop(
        state: Arc<Mutex<SequencerState>>,
        command_rx: mpsc::Receiver<PlaybackCommand>,
        step_handler: Arc<H>,
    ) {
        let mut current_sequence: Option<Sequence> = None;
        let mut current_step = 0u32;
        let mut playing = false;
        let mut active_note_off_events = HashMap::new();
        let mut last_step_time = Instant::now();

        // Timing configuration
        let step_duration = Duration::from_millis(250); // 240 BPM at 16th notes
        let command_check_interval = Duration::from_micros(100); // Very fast command response

        println!("🎵 High-priority sequencer thread started");

        loop {
            let loop_start = Instant::now();

            // Handle commands with minimal latency - check multiple times per step
            match command_rx.try_recv() {
                Ok(command) => {
                    match command {
                        PlaybackCommand::Start(sequence) => {
                            println!("Starting playback");
                            current_sequence = Some(sequence);
                            current_step = 0;
                            playing = true;
                            last_step_time = Instant::now(); // Reset timing

                            // Update shared state
                            if let Ok(mut state_guard) = state.lock() {
                                state_guard.playing = true;
                                state_guard.current_step = 0;
                                state_guard.current_sequence = current_sequence.clone();
                            }
                        }
                        PlaybackCommand::Stop => {
                            println!("Stopping playback");
                            playing = false;

                            active_note_off_events.clear();

                            // Update shared state
                            if let Ok(mut state_guard) = state.lock() {
                                state_guard.playing = false;
                            }
                        }
                        PlaybackCommand::Swap(sequence) => {
                            println!("Swapping sequence");
                            current_sequence = Some(sequence);
                            // Keep current step position, but clamp to new sequence length
                            if let Some(ref seq) = current_sequence {
                                if current_step >= seq.sequence_length {
                                    current_step = 0;
                                    last_step_time = Instant::now(); // Reset timing on wrap
                                }
                            }

                            // Update shared state
                            if let Ok(mut state_guard) = state.lock() {
                                state_guard.current_sequence = current_sequence.clone();
                                state_guard.current_step = current_step;
                            }
                        }
                        PlaybackCommand::Shutdown => {
                            println!("Shutting down playback thread");
                            return;
                        }
                    }
                }
                Err(mpsc::TryRecvError::Empty) => {
                    // No command, continue with timing loop
                }
                Err(mpsc::TryRecvError::Disconnected) => {
                    println!("Command channel disconnected");
                    return;
                }
            }

            if playing {
                if let Some(ref sequence) = current_sequence {
                    let elapsed = last_step_time.elapsed();

                    // Calculate actual BPM timing from sequence
                    let actual_step_duration = Self::calculate_step_duration(sequence);

                    if elapsed >= actual_step_duration {
                        Self::process_note_off_events(
                            &mut active_note_off_events,
                            &step_handler,
                            loop_start,
                        );
                        Self::play_sequence_step(
                            sequence,
                            current_step,
                            &step_handler,
                            &mut active_note_off_events,
                        );

                        // Advance to next step
                        current_step = (current_step + 1) % sequence.sequence_length;
                        last_step_time = Instant::now();

                        // Update shared state with new step
                        if let Ok(mut state_guard) = state.lock() {
                            state_guard.current_step = current_step;
                        }
                    }
                }
            }

            // Minimal sleep to prevent excessive CPU usage while maintaining responsiveness
            let loop_duration = loop_start.elapsed();
            if loop_duration < command_check_interval {
                thread::sleep(command_check_interval - loop_duration);
            }
        }
    }

    /// Calculate step duration based on BPM and subdivision
    fn calculate_step_duration(sequence: &Sequence) -> Duration {
        let bpm = sequence.bpm.max(60).min(300); // Clamp BPM to reasonable range

        // Default to 16th notes if no subdivision specified
        let subdivision = sequence
            .trig_subdivision
            .as_ref()
            .map(|s| s.denominator as f64)
            .unwrap_or(16.0);

        // Calculate milliseconds per step
        let beats_per_minute = bpm as f64;
        let beats_per_second = beats_per_minute / 60.0;
        let steps_per_beat = subdivision / 4.0; // Assuming quarter note = 1 beat
        let steps_per_second = beats_per_second * steps_per_beat;
        let milliseconds_per_step = 1000.0 / steps_per_second;

        Duration::from_millis(milliseconds_per_step as u64)
    }

    /// Print the events for a given step (now uses injected handler)
    fn play_sequence_step(
        sequence: &Sequence,
        step: u32,
        step_handler: &Arc<H>,
        active_note_off_events: &mut HashMap<Instant, Vec<Trig>>,
    ) {
        println!("🎵 Step {} of {}:", step, sequence.sequence_length);

        // Find all trigs for this step
        let step_trigs: Vec<&Trig> = sequence
            .trigs
            .iter()
            .filter(|trig| trig.step == step)
            .collect();

        let now = Instant::now();
        // TODO - We should really statically calculate this once.
        let step_duration = Self::calculate_step_duration(sequence);

        for trig in &step_trigs {
            if let Some(_note) = &trig.note {
                let note_off_time = now + (step_duration * trig.length.ceil() as u32);

                active_note_off_events
                    .entry(note_off_time)
                    .or_insert_with(Vec::new)
                    .push((*trig).clone());
            }
        }

        step_handler.handle_steps(step_trigs, StepEvent::NOTE_ON);
    }

    /// New helper function to process note off events
    fn process_note_off_events(
        active_note_off_events: &mut HashMap<Instant, Vec<Trig>>,
        step_handler: &Arc<H>,
        current_time: Instant,
    ) {
        // Collect all note-off events that should trigger now or earlier
        let mut events_to_remove = Vec::new();
        let mut trigs_to_turn_off = Vec::new();

        for (event_time, trigs) in active_note_off_events.iter() {
            if *event_time <= current_time {
                // TODO Weird. Just use a data structure.
                events_to_remove.push(*event_time);
                trigs_to_turn_off.extend(trigs.iter().cloned());
            }
        }

        // Remove processed events
        for time in events_to_remove {
            active_note_off_events.remove(&time);
        }

        step_handler.handle_steps(trigs_to_turn_off.iter().collect(), StepEvent::NOTE_OFF);
    }

    // All your existing methods remain the same...
    pub fn cue_sequence(&self, sequence: Sequence) -> CueResult {
        println!("Cueing sequence: {}", sequence);

        let mut state = self.state.lock().unwrap();
        let replaced_existing = state.cued_sequence.is_some();

        let remaining_steps = if state.playing {
            if let Some(current_seq) = &state.current_sequence {
                current_seq.sequence_length - state.current_step
            } else {
                0
            }
        } else {
            sequence.sequence_length
        };

        state.cued_sequence = Some(sequence);

        if replaced_existing {
            println!("Replaced existing cued sequence");
        } else {
            println!("Cued new sequence");
        }

        OperationResult::success(CueMetadata {
            replaced_existing,
            remaining_steps,
        })
    }

    pub fn start_sequence(&self) -> StartResult {
        let mut state = self.state.lock().unwrap();

        if let Some(cued_sequence) = state.cued_sequence.take() {
            drop(state); // Release lock before sending command

            if let Some(ref tx) = self.playback_control {
                if tx.send(PlaybackCommand::Start(cued_sequence)).is_ok() {
                    OperationResult::<StartResult>::success_empty()
                } else {
                    println!("❌ Failed to send start command");
                    OperationResult::failure()
                }
            } else {
                println!("❌ Playback system not initialized");
                OperationResult::failure()
            }
        } else {
            println!("❌ No sequence cued - cannot start");
            OperationResult::failure()
        }
    }

    pub fn stop_sequence(&self) -> StopResult {
        let trig_count = {
            let state = self.state.lock().unwrap();
            state.current_sequence.as_ref().map(|seq| seq.trigs.len())
        };

        if let Some(ref tx) = self.playback_control {
            if tx.send(PlaybackCommand::Stop).is_ok() {
                if let Some(count) = trig_count {
                    println!("Stopped sequence had {} trigs", count);
                }
                OperationResult::success(StopMetadata { trig_count })
            } else {
                println!("❌ Failed to send stop command");
                OperationResult::failure()
            }
        } else {
            println!("❌ Playback system not initialized");
            OperationResult::failure()
        }
    }

    pub fn swap_sequence(&self, sequence: Sequence) -> SwapResult {
        println!("Swapping sequence: {}", sequence);

        let replaced_existing = {
            let state = self.state.lock().unwrap();
            state.current_sequence.is_some()
        };

        if let Some(ref tx) = self.playback_control {
            if tx.send(PlaybackCommand::Swap(sequence)).is_ok() {
                OperationResult::success(SwapMetadata { replaced_existing })
            } else {
                println!("❌ Failed to send swap command");
                OperationResult::failure()
            }
        } else {
            println!("❌ Playback system not initialized");
            OperationResult::failure()
        }
    }

    pub fn is_playing(&self) -> bool {
        let state = self.state.lock().unwrap();
        state.playing
    }

    pub fn current_step(&self) -> u32 {
        let state = self.state.lock().unwrap();
        state.current_step
    }

    pub fn current_sequence_info(&self) -> Option<(u32, usize)> {
        let state = self.state.lock().unwrap();
        state
            .current_sequence
            .as_ref()
            .map(|seq| (seq.sequence_length, seq.trigs.len()))
    }

    pub fn shutdown(&mut self) {
        if let Some(ref tx) = self.playback_control {
            let _ = tx.send(PlaybackCommand::Shutdown);
        }

        if let Some(handle) = self.playback_handle.take() {
            let _ = handle.join();
        }
    }
}

impl<H: StepHandler> Drop for Sequencer<H> {
    fn drop(&mut self) {
        self.shutdown();
    }
}

pub struct ConsoleStepHandler {
    midi_connection: Mutex<MidiOutputConnection>
}

impl ConsoleStepHandler {
    pub fn new(midi_connection: MidiOutputConnection) -> Self {
        Self {
            midi_connection: Mutex::new(midi_connection)
        }
    }
}

impl StepHandler for ConsoleStepHandler {
    fn handle_steps(&self, trigs: Vec<&Trig>, event: StepEvent) {
        let mut connection = self.midi_connection.lock().unwrap();
        if trigs.is_empty() {
            println!("   (silence)");
        } else {
            for trig in trigs {
                match &trig.note {
                    Some(note) => {
                        println!("   Track {}: Play {}", trig.track, note);
                    }
                    None => {
                        println!("   Track {}: REST", trig.track);
                    }
                }
            }
        }
    }
}

// Mock handler for testing
pub struct MockStepHandler {
    pub calls: Arc<Mutex<Vec<Vec<(u32, Option<String>)>>>>,
}

impl MockStepHandler {
    pub fn new() -> Self {
        Self {
            calls: Arc::new(Mutex::new(Vec::new())),
        }
    }

    pub fn get_calls(&self) -> Vec<Vec<(u32, Option<String>)>> {
        self.calls.lock().unwrap().clone()
    }
}

impl StepHandler for MockStepHandler {
    fn handle_steps(&self, trigs: Vec<&Trig>, event: StepEvent) {
        let mut calls = self.calls.lock().unwrap();
        let call_data: Vec<(u32, Option<String>)> = trigs
            .iter()
            .map(|trig| (trig.track, trig.note.as_ref().map(|note| note.to_string())))
            .collect();
        calls.push(call_data);
    }
}

// MIDI handler (placeholder for your actual MIDI implementation)
pub struct MidiStepHandler {
    midi_output_connection: Mutex<MidiOutputConnection>,
}

impl MidiStepHandler {
    pub fn new() -> Self {
        let midi_out = MidiOutput::new("Sequencer").unwrap();
        let ports = midi_out.ports();
        let last_port = ports.last().unwrap();

        Self {
            midi_output_connection: Mutex::new(midi_out.connect(last_port, "seq").unwrap()),
        }
    }
}

impl StepHandler for MidiStepHandler {
    fn handle_steps(&self, trigs: Vec<&Trig>, event: StepEvent) {
        let mut connection = self.midi_output_connection.lock().unwrap();

        for trig in trigs {
            if let Some(note) = &trig.note {
                // Send MIDI note on for the track/channel

                println!("MIDI: Track {} -> Note {}", trig.track, note);
                // let _ = connection.send(&note_on_msg);
                // Your actual MIDI implementation here
                // Still need to implement note off functionality, which essentially requires
                // a transform as data is queued up...
            }
        }
    }
}

// Type aliases for convenience
pub type ConsoleSequencer = Sequencer<ConsoleStepHandler>;
pub type MockSequencer = Sequencer<MockStepHandler>;
pub type MidiSequencer = Sequencer<MidiStepHandler>;
