use crate::server::sequence::Note as SequenceNote;
use crate::server::sequence::{Sequence, Trig};
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, Mutex};
use std::sync::atomic::{AtomicBool, Ordering};
use std::thread;

// General sequencer data structure definition.
pub trait StepHandler: Send + Sync + 'static {
    fn handle_notes_on(&self, trigs: Vec<&Trig>);
    fn handle_notes_off(&self, trigs: Vec<&Trig>);
}

#[derive(Debug, Default)]
struct SequencerState {
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
    fn swap_sequence(&self, s: Sequence) -> SwapResult;
    fn cue_sequence(&self, s: Sequence) -> CueResult;
}

pub struct CoreSequencer<T: StepHandler> {
    running: Arc<AtomicBool>,
    step_handler: T,
    bpm_tx: mpsc::Sender<f32>
}

impl<T: StepHandler> CoreSequencer<T> {
    pub fn new(step_handler: T) -> Self {
        let running = Arc::new(AtomicBool::new(false));
        let (bpm_tx, bpm_rx) = mpsc::channel();

        let running_clone = Arc::clone(&running);
        thread::spawn(move || {
            sequencer_loop(running_clone, bpm_rx);
        });

        CoreSequencer {
            running,
            step_handler,
            bpm_tx
        }
    }

}

fn sequencer_loop(running: Arc<AtomicBool>, bpm_rx: Receiver<f32>) {
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
            tick += 1;

            // TODO - we should actually loop based on sequence length (quarters * 256 = length)
            // that way we can precompute our trig positions ahead of time, store them in a hashmap
            // or circular linked list, and then play them...
            if tick >= 255 {
                tick = 0;
                println!("Beat.");
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

    fn swap_sequence(&self, s: Sequence) -> SwapResult {
        self.bpm_tx.send(s.bpm);
        Result::Ok(SwapMetadata { replaced_existing: true })
    }

    fn cue_sequence(&self, s: Sequence) -> CueResult {
        self.bpm_tx.send(s.bpm);
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

fn parse_note_to_midi(note: &SequenceNote) -> u8 {
    ((note.octave * 12) + note.value as i32).try_into().unwrap()
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
