use crate::server::sequence::Note as SequenceNote;
use crate::server::sequence::{Sequence, Trig};
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, Mutex};
use std::sync::atomic::{AtomicBool, Ordering};
use std::marker::PhantomData;
use std::thread;
use heapless::Vec;

#[derive(Debug)]
enum Event {
    NoteOn(u8),
    NoteOff(u8)
}

type Events = Vec<(Event, usize), 2000>;

struct EventRing {
    // TODO - We'll want to make this a swappable pair so that once we get to a position, if the
    // other events buffer is populated, it clears out the old one and switches to the new one...
    events: Events,
    position: usize,
    tick_length: usize
}

fn parse_note_to_midi(note: &SequenceNote) -> u8 {
    ((note.octave * 12) + note.value as i32).try_into().unwrap()
}

impl EventRing {
    fn new() -> Self {
        Self {
            events: Vec::new(),
            position: 0,
            tick_length: 0
        }
    }

    fn swap_sequence(&mut self, sequence: &Sequence) {
        let mut events: Vec<(Event, usize), 2000> = Vec::new();

        let sequence_length_ticks = (sequence.sequence_length * 256) as usize;

        for trig in &sequence.trigs {
            if let Some(note) = &trig.note {
                let midi_pitch = parse_note_to_midi(note);
                let note_on_tick = (trig.step * 256) as usize % sequence_length_ticks;
                events.push((Event::NoteOn(midi_pitch), note_on_tick));
                let note_off_tick = (note_on_tick + 64) as usize % sequence_length_ticks;
                events.push((Event::NoteOff(midi_pitch), note_off_tick));
            }
        }

        events.sort_by_key(|(_, tick)| *tick);

        self.events = events;
        self.position = 0;
        self.tick_length = sequence_length_ticks;
    }

    fn peek(&self) -> Option<&(Event, usize)> {
        if self.events.is_empty() {
            return None;
        }

        Some(&self.events[self.position])
    }

    fn take(&mut self) -> Option<&(Event, usize)> {
        if self.events.is_empty() {
            return None;
        }

        let r = Some(&self.events[self.position]);
        self.position = (self.position + 1) % self.events.len();
        r
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

// General sequencer data structure definition.
pub trait StepHandler: Send + Sync + 'static {
    fn handle_notes_on(&self, trigs: Vec<&Trig, 100>);
    fn handle_notes_off(&self, trigs: Vec<&Trig, 100>);
}

pub struct CoreSequencer<T: StepHandler> {
    running: Arc<AtomicBool>,
    bpm_tx: mpsc::Sender<f32>,
    event_ring: Arc<Mutex<EventRing>>,
    _phantom: std::marker::PhantomData<T>
}

impl<T: StepHandler> CoreSequencer<T> {
    pub fn new(step_handler: T) -> Self {
        let running = Arc::new(AtomicBool::new(false));
        let (bpm_tx, bpm_rx) = mpsc::channel();

        let event_ring = Arc::new(Mutex::new(EventRing::new()));
        let event_ring_clone = event_ring.clone();

        let running_clone = Arc::clone(&running);
        thread::spawn(move || {
            sequencer_loop(running_clone, bpm_rx, event_ring_clone, step_handler);
        });

        CoreSequencer {
            running,
            bpm_tx,
            event_ring,
            _phantom: PhantomData
        }
    }

}

fn sequencer_loop<T: StepHandler>(running: Arc<AtomicBool>, bpm_rx: Receiver<f32>, seq: Arc<Mutex<EventRing>>, step_handler: T) {
    let mut loop_helper = LoopHelper::builder()
        .build_with_target_rate(1000.0);
    let mut tick = 0;

    loop {
        loop_helper.loop_start();

        // Handle BPM changes
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
            // Collect notes on and off events for this tick
            let mut notes_on : Vec<Trig, 100> = Vec::new();
            let mut notes_off : Vec<Trig, 100> = Vec::new();

            {
                let mut event_ring = seq.lock().unwrap();

                // Process events that match the current tick
                // We need to check if we have events and if the next event is for this tick
                while let Some((event, event_tick)) = event_ring.peek() {
                    if *event_tick == tick {
                        // This event is for the current tick - take it and process
                        if let Some((event, _)) = event_ring.take() {
                            match event {
                                Event::NoteOn(pitch) => {
                                    let trig = Trig {
                                        note: Some(SequenceNote {
                                            octave: (*pitch as i32 / 12) - 1,
                                            value: (*pitch as i32) % 12,
                                            velocity: 100,
                                        }),
                                        track: 0,
                                        step: 0,
                                        offset: 0,
                                        length: None,
                                    };
                                    notes_on.push(trig);
                                }
                                Event::NoteOff(pitch) => {
                                    let trig = Trig {
                                        note: Some(SequenceNote {
                                            octave: (*pitch as i32 / 12) - 1,
                                            value: (*pitch as i32) % 12,
                                            velocity: 0,
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
                    } else {
                        // Next event is not for this tick, stop processing
                        break;
                    }
                }
            }

            // Handle the collected events
            if !notes_on.is_empty() {
                let trig_refs: Vec<&Trig, 100> = notes_on.iter().collect();
                step_handler.handle_notes_on(trig_refs);
            }

            if !notes_off.is_empty() {
                let trig_refs: Vec<&Trig, 100> = notes_off.iter().collect();
                step_handler.handle_notes_off(trig_refs);
            }

            tick += 1;

            // Handle sequence looping
            {
                let event_ring = seq.lock().unwrap();
                if tick >= event_ring.tick_length {
                    tick = 0;
                    println!("Sequence loop - back to tick 0");
                }
            }
        }

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
        self.event_ring.lock().unwrap().swap_sequence(&s);
        Result::Ok(SwapMetadata { replaced_existing: true })
    }

    fn cue_sequence(&mut self, s: Sequence) -> CueResult {
        self.bpm_tx.send(s.bpm);
        self.event_ring.lock().unwrap().swap_sequence(&s);
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
    fn handle_notes_on(&self, trigs: Vec<&Trig, 100>) {
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

    fn handle_notes_off(&self, trigs: Vec<&Trig, 100>) {
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
