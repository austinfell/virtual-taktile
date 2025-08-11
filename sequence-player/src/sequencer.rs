use crate::server::sequence::{Sequence, Trig};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::{mpsc};
use tokio::time::interval;

#[derive(Debug)]
pub struct Sequencer {
    state: Arc<Mutex<SequencerState>>,
    playback_control: Option<mpsc::UnboundedSender<PlaybackCommand>>,
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

// Generic operation result with optional metadata
#[derive(Debug, Clone)]
pub struct OperationResult<T = ()> {
    pub success: bool,
    pub data: Option<T>,
}

// Specific metadata types for different operations
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

// Type aliases for cleaner API
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

impl Sequencer {
    pub fn new() -> Self {
        Self {
            state: Arc::new(Mutex::new(SequencerState::default())),
            playback_control: None,
        }
    }

    /// Initialize the playback system (call this once at startup)
    pub fn initialize_playback(&mut self) {
        let (tx, rx) = mpsc::unbounded_channel();
        self.playback_control = Some(tx);
        
        let state = Arc::clone(&self.state);
        
        tokio::spawn(async move {
            Self::playback_loop(state, rx).await;
        });
    }

    /// The main playback loop that runs in a separate task
    async fn playback_loop(
        state: Arc<Mutex<SequencerState>>,
        mut command_rx: mpsc::UnboundedReceiver<PlaybackCommand>,
    ) {
        let mut ticker = interval(Duration::from_millis(250)); // Faster tick for responsiveness
        let mut current_sequence: Option<Sequence> = None;
        let mut current_step = 0u32;
        let mut playing = false;

        loop {
            tokio::select! {
                // Handle commands with highest priority
                Some(command) = command_rx.recv() => {
                    match command {
                        PlaybackCommand::Start(sequence) => {
                            println!("▶️  Starting playback immediately!");
                            current_sequence = Some(sequence);
                            current_step = 0;
                            playing = true;
                            
                            // Update shared state
                            {
                                let mut state_guard = state.lock().unwrap();
                                state_guard.playing = true;
                                state_guard.current_step = 0;
                                state_guard.current_sequence = current_sequence.clone();
                            }
                        }
                        PlaybackCommand::Stop => {
                            println!("⏹️  Stopping playback immediately!");
                            playing = false;
                            
                            // Update shared state
                            {
                                let mut state_guard = state.lock().unwrap();
                                state_guard.playing = false;
                            }
                        }
                        PlaybackCommand::Swap(sequence) => {
                            println!("🔄 Swapping sequence immediately!");
                            current_sequence = Some(sequence);
                            // Keep current step position, but clamp to new sequence length
                            if let Some(ref seq) = current_sequence {
                                if current_step >= seq.sequence_length {
                                    current_step = 0;
                                }
                            }
                            
                            // Update shared state
                            {
                                let mut state_guard = state.lock().unwrap();
                                state_guard.current_sequence = current_sequence.clone();
                                state_guard.current_step = current_step;
                            }
                        }
                        PlaybackCommand::Shutdown => {
                            println!("🛑 Shutting down playback loop");
                            break;
                        }
                    }
                }
                
                // Handle timing tick only if playing
                _ = ticker.tick(), if playing => {
                    if let Some(ref sequence) = current_sequence {
                        Self::print_step_events(sequence, current_step);
                        
                        // Advance to next step
                        current_step = (current_step + 1) % sequence.sequence_length;
                        
                        // Update shared state with new step
                        {
                            let mut state_guard = state.lock().unwrap();
                            state_guard.current_step = current_step;
                        }
                    }
                }
            }
        }
    }

    /// Cue a sequence for later playback
    pub fn cue_sequence(&self, sequence: Sequence) -> CueResult {
        println!("Cueing sequence: {}", sequence);

        let mut state = self.state.lock().unwrap();
        let replaced_existing = state.cued_sequence.is_some();

        // Calculate remaining steps based on current playback position
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

    /// Start playing the cued sequence
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

    /// Stop the currently playing sequence
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

    /// Immediately swap the currently playing sequence
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

    /// Get the current playback status
    pub fn is_playing(&self) -> bool {
        let state = self.state.lock().unwrap();
        state.playing
    }

    /// Get the current step position
    pub fn current_step(&self) -> u32 {
        let state = self.state.lock().unwrap();
        state.current_step
    }

    /// Get information about the current sequence
    pub fn current_sequence_info(&self) -> Option<(u32, usize)> {
        let state = self.state.lock().unwrap();
        state
            .current_sequence
            .as_ref()
            .map(|seq| (seq.sequence_length, seq.trigs.len()))
    }

    /// Shutdown the playback system
    pub fn shutdown(&self) {
        if let Some(ref tx) = self.playback_control {
            let _ = tx.send(PlaybackCommand::Shutdown);
        }
    }

    /// Print the events for a given step (extracted for cleaner code)
    fn print_step_events(sequence: &Sequence, step: u32) {
        println!("🎵 Step {} of {}:", step, sequence.sequence_length);

        // Find and print all trigs for this step
        let step_trigs: Vec<&Trig> = sequence
            .trigs
            .iter()
            .filter(|trig| trig.step == step)
            .collect();

        if step_trigs.is_empty() {
            println!("   (silence)");
        } else {
            for trig in step_trigs {
                match &trig.note {
                    Some(note) => {
                        println!("   🎶 Track {}: Play {}", trig.track, note);
                    }
                    None => {
                        println!("   🔇 Track {}: REST", trig.track);
                    }
                }
            }
        }
    }
}

impl Default for Sequencer {
    fn default() -> Self {
        Self::new()
    }
}
