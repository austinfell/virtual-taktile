use crate::server::sequence::{Sequence};
use midir::MidiOutputConnection;
use spin_sleep::LoopHelper;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, Ordering};
use std::marker::PhantomData;
use std::thread;
use heapless::Vec;

const INIT_BPM: f64 = 120.0;
const TICKS_PER_BEAT_F: f64 = 768.0;
const TICKS_PER_BEAT_U: u32 = 768;
const SECONDS_PER_MINUTE: f64 = 60.0;

/// A MIDI-Like note message containing relevant data linking a played note to a particular track.
///
/// # Fields
/// * `track` - The MIDI track number (0-15)
/// * `note` - The MIDI note number (0-127, where 60 is middle C)
/// * `velocity` - The note velocity (0-127, where 0 is silent and 127 is maximum)
#[derive(Debug)]
pub struct NoteMessage {
    track: u8,
    note: u8,
    velocity: u8
}

/// A MIDI-Like event that can be scheduled in our looping sequencer.
#[derive(Debug)]
pub enum Event {
    /// Note on event - triggers a note to start playing.
    NoteOn(NoteMessage),
    /// Note off event - triggers a note to stop playing.
    NoteOff(NoteMessage)
}

type Events = Vec<(Event, usize), 2000>;

#[derive(Debug)]
struct EventBuffer {
    events: Events,
    tick_length: usize,
    bpm: f64,
}

impl EventBuffer {
    /// Converts a `Sequence` into a `ScheduledSequence` by processing triggers into sorted MIDI events.
    ///
    /// Validates MIDI parameters (note, track, velocity must be 0-255) and skips invalid triggers.
    /// Stops processing if event capacity (2000) is reached to avoid orphaned note-on events.
    fn from_sequence(sequence: &Sequence) -> Self {
        let sequence_length_ticks = (sequence.sequence_length * TICKS_PER_BEAT_U) as usize;

        let mut events: Events = Vec::new();
        for trig in &sequence.trigs {
            if let Some(note_data) = &trig.note {
                // Make sure we have space.
                if events.len() + 2 > events.capacity() {
                    println!("Sequence is full - skipping remaining notes to avoid orphaned events.");
                    break;
                }

                // Make sure the user isn't breaking MIDI.
                let Some(note): Option<u8> = ((note_data.octave * 12) + note_data.value).try_into().ok() else {
                    println!("Got a note outside of 8 bit range allowed by MIDI.");
                    continue;
                };
                let Some(track) = trig.track.try_into().ok() else {
                    println!("Track number {} is out of range (must be 0-255).", trig.track);
                    continue;
                };
                let Some(velocity) = note_data.velocity.try_into().ok() else {
                    println!("Velocity {} is out of range (must be 0-255).", note_data.velocity);
                    continue;
                };


                // We can unwrap these because we already ran a check to make sure we have room in our vector.
                events.push((
                    Event::NoteOff(NoteMessage {track, note, velocity}),
                    ((trig.step * TICKS_PER_BEAT_U) as i32 + trig.offset + trig.length as i32)
                        .rem_euclid(sequence_length_ticks as i32) as usize
                )).unwrap();
                events.push((
                    Event::NoteOn(NoteMessage {track, note, velocity}),
                    ((trig.step * TICKS_PER_BEAT_U) as i32 + trig.offset)
                        .rem_euclid(sequence_length_ticks as i32) as usize
                )).unwrap();
            }
        }
        events.sort_by_key(|(_, tick)| *tick);

        Self {
            events,
            tick_length: sequence_length_ticks,
            bpm: sequence.bpm,
        }
    }

    /// Returns a slice of all events that occur at the same tick as the event at `start_index`.
    ///
    /// Scans forward from `start_index` to find all consecutive events with matching tick values.
    /// Returns `None` if `start_index` is out of bounds.
    fn get_events_at_index_matching_tick(&self, start_index: usize) -> Option<&[(Event, usize)]> {
        let tick = self.events.get(start_index)?.1;
        let mut end_index = start_index;

        if self.events.len() > 1 {
            end_index += 1;
        }

        while end_index != self.events.len() && tick == self.events[end_index].1 {
            end_index += 1
        }

        Some(&self.events[start_index..end_index])
    }
}

#[derive(Debug)]
struct EventRing {
    buffers: [Option<EventBuffer>; 2],
    current_buffer: usize,
    cued_buffer: usize,
    position: usize,
}

impl EventRing {
    fn new() -> Self {
        Self {
            buffers: [None, None],
            current_buffer: 0,
            cued_buffer: 0,
            position: 0,
        }
    }

    fn swap_sequence(&mut self, sequence: &Sequence) {
        self.buffers[self.current_buffer] = Some(EventBuffer::from_sequence(sequence));
    }

    fn cue_sequence(&mut self, sequence: &Sequence) {
        let target_buffer = 1 - self.current_buffer;
        self.buffers[target_buffer] = Some(EventBuffer::from_sequence(sequence));
        self.cued_buffer = target_buffer;
    }

    fn current_buffer_ref(&self) -> &Option<EventBuffer> {
        &self.buffers[self.current_buffer]
    }

    fn switch_to_cued_buffer(&mut self) {
        if self.position == 0 && self.cued_buffer != self.current_buffer {
            self.current_buffer = self.cued_buffer;
        }
    }

    fn read_next_first(&self) -> Option<&(Event, usize)> {
        let Some(current_buffer) = &self.buffers[self.current_buffer] else {
            return None;
        };

        if current_buffer.events.is_empty() {
            return None;
        }

        current_buffer.events.get(self.position)
    }

    fn read_next_all(&self) -> Option<&'_[(Event, usize)]> {
        let Some(current_buffer) = &self.buffers[self.current_buffer] else {
            return None;
        };

        if current_buffer.events.is_empty() {
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
        let Some(current_buffer) = &self.buffers[self.current_buffer] else {
            return;
        };

        if current_buffer.events.is_empty() {
            return
        }

        // Get all events at the current position.
        let Some(events) = current_buffer.get_events_at_index_matching_tick(self.position) else {
            return
        };

        // Increment to the next position.
        self.position = (self.position + (events.len())) % current_buffer.events.len();
    }

    fn tick_len(&self) -> Option<usize> {
        let Some(current_buffer) = self.current_buffer_ref() else {
            return None;
        };
        Some(current_buffer.tick_length)
    }

    fn current_bpm(&self) -> Option<f64> {
        let Some(current_buffer) = self.current_buffer_ref() else {
            return None;
        };
        Some(current_buffer.bpm)
    }
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
        .build_with_target_rate((INIT_BPM * TICKS_PER_BEAT_F) / SECONDS_PER_MINUTE);

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
                event_ring.tick_len().or(Some(0)).unwrap()
            )
        };
        {
            // Tick until we get to the next event.
            while tick != next_event_tick {
                loop_helper.loop_start();
                tick = (tick + 1) % (sequence_length + 1);
                loop_helper.loop_sleep();
            }

            // Grab all of the events with the same tick and trigger hardware.
            let event_ring = ring.read().unwrap();
            let curr_bpm = event_ring.current_bpm().or(Some(9999.0)).unwrap();
            if curr_bpm != loop_helper.target_rate() {
                loop_helper.set_target_rate(((curr_bpm as f64) * TICKS_PER_BEAT_F * 4.0) / SECONDS_PER_MINUTE);
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
            let (status_byte, note, velocity, channel, event_name) = match &event.0 {
                Event::NoteOff(note_message) => {
                    (0x80, note_message.note, note_message.velocity, note_message.track, "NoteOff")
                },
                Event::NoteOn(note_message) => {
                    (0x90, note_message.note, note_message.velocity, note_message.track, "NoteOn")
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
