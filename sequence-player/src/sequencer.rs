use crate::server::sequence::Note as SequenceNote;
use crate::server::sequence::{Sequence, Trig};
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use wmidi::Velocity;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, Ordering};
use std::marker::PhantomData;
use std::thread;
use heapless::Vec;

const INIT_BPM: f64 = 120.0;
const TICKS_PER_BEAT: f64 = 768.0;
const SECONDS_PER_MINUTE: f64 = 60.0;

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
    bpm: f64,
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

    fn get_events_at_index_matching_tick(&self, start_index: usize) -> Option<&[(Event, usize)]> {
        let Some(start_el) = self.events.get(start_index) else {
            return None
        };

        let mut end_index = start_index + 1;

        while start_index != end_index && start_el.1 == self.events[end_index % self.events.len()].1 {
            end_index += 1
        }

        Some(&self.events[start_index..end_index])
    }

    fn get_first_event_at_index(&self, start_index: usize) -> Option<&(Event, usize)> {
        self.events.get(start_index)
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
        let sequence_length_ticks = (sequence.sequence_length * 768) as usize;

        for trig in &sequence.trigs {
            if let Some(note) = &trig.note {
                let midi_pitch = parse_note_to_midi(note);
                let note_on_tick = ((trig.step as i32 * 768) + trig.offset).rem_euclid(sequence_length_ticks.try_into().unwrap()) as usize;
                events.push((Event::NoteOn(midi_pitch, note.velocity as u8, trig.track as u8), note_on_tick));
                let note_off_tick = ((trig.step as i32 * 768) + trig.offset + (trig.length as i32)).rem_euclid(sequence_length_ticks.try_into().unwrap()) as usize;
                events.push((Event::NoteOff(midi_pitch, note.velocity as u8, trig.track as u8), note_off_tick));
            }
        }

        events.sort_by_key(|(_, tick)| *tick);

        self.buffers[self.current_buffer] = EventBuffer {
            events,
            tick_length: sequence_length_ticks,
            bpm: sequence.bpm as f64
        };
    }

    fn cue_sequence(&mut self, sequence: &Sequence) {
        let mut events: Vec<(Event, usize), 2000> = Vec::new();
        let sequence_length_ticks = (sequence.sequence_length * 768) as usize;

        for trig in &sequence.trigs {
            if let Some(note) = &trig.note {
                let midi_pitch = parse_note_to_midi(note);
                let note_on_tick = ((trig.step as i32 * 768) + trig.offset).rem_euclid(sequence_length_ticks.try_into().unwrap()) as usize;
                events.push((Event::NoteOn(midi_pitch, note.velocity as u8, trig.track as u8), note_on_tick));
                let note_off_tick = ((trig.step as i32 * 768) + trig.offset + (trig.length as i32)).rem_euclid(sequence_length_ticks.try_into().unwrap()) as usize;
                events.push((Event::NoteOff(midi_pitch, note.velocity as u8, trig.track as u8), note_off_tick));
            }
        }

        events.sort_by_key(|(_, tick)| *tick);

        let target_buffer = 1 - self.current_buffer;
        self.buffers[target_buffer] = EventBuffer {
            events,
            tick_length: sequence_length_ticks,
            bpm: sequence.bpm as f64
        };
        self.cued_buffer = target_buffer;
    }

    fn current_buffer_ref(&self) -> &EventBuffer {
        &self.buffers[self.current_buffer]
    }

    fn switch_to_cued_buffer(&mut self) {
        if self.position == 0 && self.cued_buffer != self.current_buffer {
            self.current_buffer = self.cued_buffer;
        }
    }

    fn read_next_first(&self) -> Option<&(Event, usize)> {
        //  Get the current buffer.
        let current_buffer = &self.buffers[self.current_buffer];
        if current_buffer.is_empty() {
            return None;
        }

        current_buffer.get_first_event_at_index(self.position)
    }

    fn read_next_all(&self) -> Option<&'_[(Event, usize)]> {
        //  Get the current buffer.
        let current_buffer = &self.buffers[self.current_buffer];
        if current_buffer.is_empty() {
            return None;
        }

        // Get all events at the current position.
        let Some(events) = current_buffer.get_events_at_index_matching_tick(self.position) else {
            return None;
        };

        Some(events)
    }

    fn inc(&mut self) {
        //  Get the current buffer.
        let current_buffer = &self.buffers[self.current_buffer];
        if current_buffer.is_empty() {
            return
        }

        // Get all events at the current position.
        let Some(events) = current_buffer.get_events_at_index_matching_tick(self.position) else {
            return
        };

        // Increment to the next position.
        self.position = (self.position + (events.len())) % current_buffer.len();
    }

    fn tick_len(&self) -> usize {
        self.current_buffer_ref().tick_length
    }

    fn current_bpm(&self) -> f64 {
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
    event_ring: Arc<RwLock<EventRing>>,
    _phantom: std::marker::PhantomData<T>
}

impl<T: StepHandler> CoreSequencer<T> {
    pub fn new(step_handler: T) -> Self {
        let running = Arc::new(AtomicBool::new(false));

        let event_ring = Arc::new(RwLock::new(EventRing::new()));
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

fn sequencer_loop<T: StepHandler>(running: Arc<AtomicBool>, ring: Arc<RwLock<EventRing>>, step_handler: T) {
    let mut tick = 0;

    let mut loop_helper = LoopHelper::builder()
        .build_with_target_rate((INIT_BPM * TICKS_PER_BEAT) / SECONDS_PER_MINUTE);

    loop {
        if !running.load(Ordering::Relaxed) {
            loop_helper.loop_start();
            loop_helper.loop_sleep();
            continue;
        }

        let (next_event_tick, sequence_length) = {
            let mut event_ring = ring.write().unwrap();

            // If we are supposed to be on the cued sequence (and we are at pos 0 in the sequence)
            // then switch immediately before doing anything.
            event_ring.switch_to_cued_buffer();
            let next = event_ring.read_next_first().unwrap();
            (
                // Figure out what the next event tick is.
                next.1,
                event_ring.tick_len()
            )
        };
        {
            // Tick until we get to the next event.
            while tick != next_event_tick {
                loop_helper.loop_start();
                tick = (tick + 1) % (sequence_length+ 1);
                loop_helper.loop_sleep();
            }

            // Grab all of the events with the same tick and trigger hardware.
            let event_ring = ring.read().unwrap();
            let curr_bpm = event_ring.current_bpm();
            if curr_bpm != loop_helper.target_rate() {
                loop_helper.set_target_rate(((curr_bpm as f64) * TICKS_PER_BEAT * 4.0) / SECONDS_PER_MINUTE);
            }
            let next_events = event_ring.read_next_all().unwrap();
            step_handler.handle_events(next_events);
            println!("{:?}", next_events);
        }
        {
            // Increment the ring so that next set of events can be read.
            let mut event_ring = ring.write().unwrap();
            event_ring.inc();
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
        self.event_ring.write().unwrap().swap_sequence(&s);
        Result::Ok(SwapMetadata { replaced_existing: true })
    }

    fn cue_sequence(&mut self, s: Sequence) -> CueResult {
        self.event_ring.write().unwrap().cue_sequence(&s);
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
