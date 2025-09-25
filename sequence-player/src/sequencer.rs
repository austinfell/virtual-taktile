use crate::server::sequence::Note as SequenceNote;
use crate::server::sequence::{Sequence, Trig};
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use wmidi::Velocity;
use std::sync::{Arc, Mutex};
use std::sync::atomic::{AtomicBool, Ordering};
use std::marker::PhantomData;
use std::thread;
use heapless::Vec;

#[derive(Debug, Clone)]
enum Event {
    NoteOn(u8, u8, u8),
    NoteOff(u8, u8, u8)
}

type Events = Vec<(Event, usize), 2000>;

#[derive(Debug)]
struct EventBuffer {
    events: Events,
    tick_length: usize,
    bpm: f32,
}

impl EventBuffer {
    fn new() -> Self {
        Self {
            events: Vec::new(),
            tick_length: 0,
            bpm: 120.0,
        }
    }

    fn is_empty(&self) -> bool {
        self.events.is_empty()
    }

    fn len(&self) -> usize {
        self.events.len()
    }

    fn get_events_matching_tick(&self, start_index: usize) -> Option<&[(Event, usize)]> {
        let Some(start_el) = self.events.get(start_index) else {
            return None
        };

        let mut end_index = start_index + 1;

        while start_index != end_index && start_el.1 == self.events[end_index % self.events.len()].1 {
            end_index += 1
        }

        Some(&self.events[start_index..end_index])
    }
}

#[derive(Debug)]
struct EventRing {
    buffers: [EventBuffer; 2],
    current_buffer: usize,
    cued_buffer: usize,
    position: usize,
}

fn parse_note_to_midi(note: &SequenceNote) -> u8 {
    ((note.octave * 12) + note.value as i32).try_into().unwrap()
}

impl EventRing {
    fn new() -> Self {
        Self {
            buffers: [EventBuffer::new(), EventBuffer::new()],
            current_buffer: 0,
            cued_buffer: 0,
            position: 0,
        }
    }

    fn swap_sequence(&mut self, sequence: &Sequence) {
        let mut events: Vec<(Event, usize), 2000> = Vec::new();
        let sequence_length_ticks = (sequence.sequence_length * 256) as usize;

        for trig in &sequence.trigs {
            if let Some(note) = &trig.note {
                let midi_pitch = parse_note_to_midi(note);
                let note_on_tick = (trig.step * 256) as usize % sequence_length_ticks;
                events.push((Event::NoteOn(midi_pitch, note.velocity as u8, trig.track as u8), note_on_tick));
                // TODO Need to use note length instead...
                let note_off_tick = (note_on_tick + 64) as usize % sequence_length_ticks;
                events.push((Event::NoteOff(midi_pitch, note.velocity as u8, trig.track as u8), note_off_tick));
            }
        }

        events.sort_by_key(|(_, tick)| *tick);

        let target_buffer = 1 - self.current_buffer;
        self.buffers[target_buffer] = EventBuffer {
            events,
            tick_length: sequence_length_ticks,
            bpm: sequence.bpm
        };
        self.cued_buffer = target_buffer;
    }

    fn current_buffer_ref(&self) -> &EventBuffer {
        &self.buffers[self.current_buffer]
    }

    fn should_switch_buffer(&self) -> bool {
        self.position == 0 && self.cued_buffer != self.current_buffer
    }

    fn switch_to_cued_buffer(&mut self) {
        if self.should_switch_buffer() {
            self.current_buffer = self.cued_buffer;
        }
    }

    fn next(&mut self) -> Option<&'_[(Event, usize)]> {
        // Make sure we are at the correct internal buffer (This is a swappable
        // circular queue)
        self.switch_to_cued_buffer();

        //  Get the current buffer.
        let current_buffer = &self.buffers[self.current_buffer];
        if current_buffer.is_empty() {
            return None;
        }

        // Get all events at the current position.
        let Some(events) = current_buffer.get_events_matching_tick(self.position) else {
            return None;
        };

        // Increment to the next position.
        self.position = (self.position + (events.len())) % current_buffer.len();

        Some(events)
    }

    fn tick_len(&self) -> usize {
        self.current_buffer_ref().tick_length
    }

    fn current_bpm(&self) -> f32 {
        self.current_buffer_ref().bpm
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
    fn handle_events(&self, trigs: &[(Event, usize)]);
}

pub struct CoreSequencer<T: StepHandler> {
    running: Arc<AtomicBool>,
    event_ring: Arc<Mutex<EventRing>>,
    _phantom: std::marker::PhantomData<T>
}

impl<T: StepHandler> CoreSequencer<T> {
    pub fn new(step_handler: T) -> Self {
        let running = Arc::new(AtomicBool::new(false));

        let event_ring = Arc::new(Mutex::new(EventRing::new()));
        let event_ring_clone = event_ring.clone();

        let running_clone = Arc::clone(&running);
        thread::spawn(move || {
            sequencer_loop(running_clone, event_ring_clone, step_handler);
        });

        CoreSequencer {
            running,
            event_ring,
            _phantom: PhantomData
        }
    }
}

fn sequencer_loop<T: StepHandler>(running: Arc<AtomicBool>, seq: Arc<Mutex<EventRing>>, step_handler: T) {
    let mut curr_bpm = 600.0;
    let mut tick = 0;
    let mut loop_helper = LoopHelper::builder()
        .build_with_target_rate((curr_bpm * 256.0) / 60.0);


    loop {

        if running.load(Ordering::Relaxed) {
            // Implementation goes here.
            let mut event_ring = seq.lock().unwrap();

            let tick_len = event_ring.tick_len();

            let next_events = event_ring.next();

            while next_events.is_some() {
                loop_helper.loop_start();
                tick = (tick + 1) % (tick_len + 1);

                if tick == next_events.unwrap().first().unwrap().1 {
                    step_handler.handle_events(next_events.unwrap());
                    println!("{:?}", next_events);
                    break;
                }
                loop_helper.loop_sleep();
            }
        }

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
        self.event_ring.lock().unwrap().swap_sequence(&s);
        Result::Ok(SwapMetadata { replaced_existing: true })
    }

    fn cue_sequence(&mut self, s: Sequence) -> CueResult {
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
    fn handle_events(&self, events: &[(Event, usize)]) {
        if events.is_empty() {
            return;
        }

        let mut connection = self.midi_connection.lock().unwrap();

        for event in events {
            let (status_byte, note, velocity, channel, event_name) = match event.0 {
                Event::NoteOff(note, velocity, channel) => {
                    (0x80, note, velocity, channel, "NoteOff")
                },
                Event::NoteOn(note, velocity, channel) => {
                    (0x90, note, velocity, channel, "NoteOn")
                }
            };

            let midi_msg = [status_byte | channel, note, velocity];

            println!("{} - Ch:{} Note:{} Vel:{} -> {:02X?}", event_name, channel, note, velocity, midi_msg);
            match connection.send(&midi_msg) {
                Ok(_) => {
                },
                Err(e) => {
                    println!("Failed to send MIDI message {:02X?}: {}", midi_msg, e);
                }
            }
        }
    }
}
