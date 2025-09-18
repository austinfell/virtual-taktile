use crate::server::sequence::Note as SequenceNote;
use crate::server::sequence::{Sequence, Trig};
use crate::types::sequence;
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use wmidi::Note;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, Mutex};
use std::sync::atomic::{AtomicBool, Ordering};
use std::marker::PhantomData;
use std::thread;

// General sequencer data structure definition.
pub trait StepHandler: Send + Sync + 'static {
    fn handle_notes_on(&self, trigs: Vec<&Trig>);
    fn handle_notes_off(&self, trigs: Vec<&Trig>);
}

#[derive(Debug)]
enum Event {
    NoteOn(u8),
    NoteOff(u8)
}

fn parse_note_to_midi(note: &SequenceNote) -> u8 {
    ((note.octave * 12) + note.value as i32).try_into().unwrap()
}

fn sequence_to_sequencer_state(sequence: &Sequence) -> SequencerState {
    let mut events = Vec::new();

    let sequence_length_ticks = sequence.sequence_length * 256;

    for trig in &sequence.trigs {
        if let Some(note) = &trig.note {
            let midi_pitch = parse_note_to_midi(note);
            let note_on_tick = (trig.step * 256) % sequence_length_ticks;
            events.push((Event::NoteOn(midi_pitch), note_on_tick));
            let note_off_tick = (note_on_tick + 64) % sequence_length_ticks;
            events.push((Event::NoteOff(midi_pitch), note_off_tick));
        }
    }

    events.sort_by_key(|(_, tick)| *tick);

    SequencerState {
        current_sequence: events,
        sequence_length: sequence_length_ticks,
    }
}


#[derive(Debug, Default)]
struct SequencerState {
    current_sequence: Vec<(Event, u32)>,
    sequence_length: u32
}

impl SequencerState {
    fn new() -> Self {
        Self {
            current_sequence: Vec::new(),
            sequence_length: 256 * 16
        }
    }

    fn swap_sequence(&mut self, new_sequence: Sequence) {
        *self = sequence_to_sequencer_state(&new_sequence);
        println!("{:?}", self.current_sequence);
    }
}

// Public interface for performing actions upon the sequencer.
#[derive(Debug)]
enum PlaybackCommand {
    Start(Sequence),
    Stop,
    Swap(Sequence),
    Shutdown,
}

// Error types for sequencer operations
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SequencerError {
    PlaybackNotInitialized,
    CommandSendFailed,
    NoSequenceCued,
    Other(String),
}

impl std::fmt::Display for SequencerError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SequencerError::PlaybackNotInitialized => write!(f, "Playback system not initialized"),
            SequencerError::CommandSendFailed => {
                write!(f, "Failed to send command to playback thread")
            }
            SequencerError::NoSequenceCued => write!(f, "No sequence cued"),
            SequencerError::Other(msg) => write!(f, "{}", msg),
        }
    }
}

impl std::error::Error for SequencerError {}

// Metadata types for successful operations
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

pub type CueResult = Result<CueMetadata, SequencerError>;
pub type StartResult = Result<(), SequencerError>;
pub type StopResult = Result<StopMetadata, SequencerError>;
pub type SwapResult = Result<SwapMetadata, SequencerError>;

pub trait Sequencer : Send + Sync + 'static {
    fn start_sequence(&self) -> StartResult;
    fn stop_sequence(&self) -> StopResult;
    fn swap_sequence(&mut self, s: Sequence) -> SwapResult;
    fn cue_sequence(&mut self, s: Sequence) -> CueResult;
}

pub struct CoreSequencer<T: StepHandler> {
    running: Arc<AtomicBool>,
    bpm_tx: mpsc::Sender<f32>,
    sequencer_state: Arc<Mutex<SequencerState>>,
    _phantom: std::marker::PhantomData<T>
}

impl<T: StepHandler> CoreSequencer<T> {
    pub fn new(step_handler: T) -> Self {
        let running = Arc::new(AtomicBool::new(false));
        let (bpm_tx, bpm_rx) = mpsc::channel();

        let sequencer_state = Arc::new(Mutex::new(SequencerState::new()));
        let sequencer_state_clone = sequencer_state.clone();

        let running_clone = Arc::clone(&running);
        thread::spawn(move || {
            sequencer_loop(running_clone, bpm_rx, sequencer_state_clone, step_handler);
        });

        CoreSequencer {
            running,
            bpm_tx,
            sequencer_state,
            _phantom: PhantomData
        }
    }

}

fn sequencer_loop<T: StepHandler>(running: Arc<AtomicBool>, bpm_rx: Receiver<f32>, seq: Arc<Mutex<SequencerState>>, step_handler: T) {
    let mut loop_helper = LoopHelper::builder()
        .build_with_target_rate(1000.0);
    let mut tick = 0;
    loop {
        loop_helper.loop_start();
        match bpm_rx.try_recv() {
            Ok(bpm) => {
                let tps = (bpm * 256.0) / 60.0;
                loop_helper.set_target_rate(tps);
                println!("Set new TPS: {tps}");
            }
            _ => {
                // TODO - implement error handling.
            }
        }
        if running.load(Ordering::Relaxed) {
            // TODO - Tick needs to be more precisely controlled to handle jitter.

            // Collect notes on and off events for this tick
            let mut notes_on = Vec::new();
            let mut notes_off = Vec::new();

            {
                let state = seq.lock().unwrap();
                for (event, event_tick) in &state.current_sequence {
                    if *event_tick == tick {
                        match event {
                            Event::NoteOn(pitch) => {
                                let trig = Trig {
                                    note: Some(SequenceNote {
                                        octave: (*pitch as i32 / 12) - 1, // Convert MIDI pitch back to octave
                                        value: (*pitch as i32) % 12,      // Convert MIDI pitch back to note value
                                        velocity: 100, // Default velocity, you might want to store this
                                    }),
                                    track: 0,    // Default track, you might want to store this
                                    step: 0,     // Could calculate from tick if needed
                                    offset: 0,   // Could calculate from tick if needed
                                    length: None, // Not relevant for note on
                                };
                                notes_on.push(trig);
                            }
                            Event::NoteOff(pitch) => {
                                // Create a minimal Trig for the note off event
                                let trig = Trig {
                                    note: Some(SequenceNote {
                                        octave: (*pitch as i32 / 12) - 1,
                                        value: (*pitch as i32) % 12,
                                        velocity: 0, // Note off typically has 0 velocity
                                    }),
                                    track: 0,
                                    step: 0,
                                    offset: 0,
                                    length: None,
                                };
                                notes_off.push(trig);
                            }
                        }
                    }
                }
            }
            
            // Handle the collected events
            if !notes_on.is_empty() {
                let trig_refs: Vec<&Trig> = notes_on.iter().collect();
                step_handler.handle_notes_on(trig_refs);
            }
            
            if !notes_off.is_empty() {
                let trig_refs: Vec<&Trig> = notes_off.iter().collect();
                step_handler.handle_notes_off(trig_refs);
            }
            
            tick += 1;
            // Handle sequence looping
            {
                let state = seq.lock().unwrap();
                if tick >= state.sequence_length {
                    tick = 0;
                    println!("Sequence loop - back to tick 0");
                }
            }
        }
        // TODO - Fetch current notes.
        // Interface &[usize...] => &[Trig...]
        // TODO - Use handle notes on to play notes.
        loop_helper.loop_sleep();
    }
}

// Core sequencer implementation.
impl<T: StepHandler> Sequencer for CoreSequencer<T> {
    fn start_sequence(&self) -> StartResult {
        self.running.swap(true, Ordering::Relaxed);
        Result::Ok(())
    }

    fn stop_sequence(&self) -> StopResult {
        self.running.swap(false, Ordering::Relaxed);
        Result::Ok(StopMetadata { trig_count: Option::from(0) })
    }

    fn swap_sequence(&mut self, s: Sequence) -> SwapResult {
        self.bpm_tx.send(s.bpm);
        self.sequencer_state.lock().unwrap().swap_sequence(s);
        Result::Ok(SwapMetadata { replaced_existing: true })
    }

    fn cue_sequence(&mut self, s: Sequence) -> CueResult {
        self.bpm_tx.send(s.bpm);
        self.sequencer_state.lock().unwrap().swap_sequence(s);
        Result::Ok(CueMetadata { replaced_existing: true, remaining_steps: 0 })
    }
}

pub struct MidiStepHandler {
    midi_connection: Mutex<MidiOutputConnection>,
}

impl MidiStepHandler {
    pub fn new(midi_connection: MidiOutputConnection) -> Self {
        Self {
            midi_connection: Mutex::new(midi_connection),
        }
    }
}

impl StepHandler for MidiStepHandler {
    fn handle_notes_on(&self, trigs: Vec<&Trig>) {
        let mut connection = self.midi_connection.lock().unwrap();
        if trigs.is_empty() {
            println!("   (silence)");
        } else {
            for trig in trigs {
                match &trig.note {
                    Some(note) => {
                        let midi_note = parse_note_to_midi(note);
                        let channel = (trig.track % 16) as u8;
                        let note_on_msg = [0x90 | channel, midi_note, note.velocity as u8];
                        match connection.send(&note_on_msg) {
                            Ok(_) => {
                                let note_name = note_value_to_string(note.value);

                                println!(
                                    "   Track {}: Play {}{} (MIDI: {}, Velocity: {})",
                                    trig.track, note_name, note.octave, midi_note, note.velocity
                                );
                            }

                            Err(e) => {
                                let note_name = note_value_to_string(note.value);
                                println!(
                                    "   Track {}: Failed to send note on for {}{}: {}",
                                    trig.track, note_name, note.octave, e
                                );
                            }
                        }
                    }
                    None => {
                        println!("   Track {}: REST", trig.track);
                    }
                }
            }
        }
    }

    fn handle_notes_off(&self, trigs: Vec<&Trig>) {
        let mut connection = self.midi_connection.lock().unwrap();
        if !trigs.is_empty() {
            for trig in trigs {
                match &trig.note {
                    Some(note) => {
                        let midi_note = parse_note_to_midi(note);
                        let channel = (trig.track % 16) as u8;
                        let note_off_msg = [0x80 | channel, midi_note, 0];

                        match connection.send(&note_off_msg) {
                            Ok(_) => {
                                let note_name = note_value_to_string(note.value);
                                println!(
                                    "   Track {}: Off {}{} (MIDI: {})",
                                    trig.track, note_name, note.octave, midi_note
                                );
                            }
                            Err(e) => {
                                let note_name = note_value_to_string(note.value);
                                println!(
                                    "   Track {}: Failed to send note off for {}{}: {}",
                                    trig.track, note_name, note.octave, e
                                );
                            }
                        }
                    }
                    None => {
                        // No note to turn off for rests
                    }
                }
            }
        }
    }
}

fn note_value_to_string(value: i32) -> &'static str {
    match value {
        0 => "C",
        1 => "C#",
        2 => "D",
        3 => "D#",
        4 => "E",
        5 => "F",
        6 => "F#",
        7 => "G",
        8 => "G#",
        9 => "A",
        10 => "A#",
        11 => "B",
        _ => "?",
    }
}
