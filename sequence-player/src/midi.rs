//! MIDI step handler implementation for standard operating systems.
//!
//! This module provides a MIDI output implementation of the `StepHandler` trait
//! using the `midir` library. It is designed for desktop and server environments
//! where standard MIDI I/O is available.
//!
//! # Platform Support
//!
//! This implementation is suitable for:
//! - Linux, macOS, and Windows desktop systems
//! - Any platform with standard MIDI device support
//!
//! This implementation is **not** suitable for:
//! - WebAssembly (WASM) targets - use a web-specific MIDI handler instead
//! - Embedded devices - use an embedded-specific MIDI handler instead
//!
//! # Example
//!
//! ```no_run
//! use midir::MidiOutput;
//!
//! let midi_out = MidiOutput::new("My Sequencer").unwrap();
//! let ports = midi_out.ports();
//! let connection = midi_out.connect(&ports[0], "output").unwrap();
//!
//! let handler = MidiStepHandler::new(connection);
//! // Use handler with your sequencer...
//! ```

use crate::sequencer::StepHandler;
use midir::MidiOutputConnection;
use std::sync::{Mutex};
use crate::sequencer::{Event};

/// A MIDI step handler that outputs events to a physical or virtual MIDI device.
///
/// This handler processes sequencer events and converts them into standard MIDI
/// messages, which are then sent through the provided MIDI connection.
///
/// # Thread Safety
///
/// The MIDI connection is protected by a `Mutex`, allowing the handler to be
/// safely shared across threads. However, note that MIDI message sending will
/// be serialized - only one thread can send messages at a time.
pub struct MidiStepHandler {
    /// The MIDI output connection, protected by a mutex for thread-safe access.
    midi_connection: Mutex<MidiOutputConnection>,
}

impl MidiStepHandler {
    /// Creates a new MIDI step handler with the given MIDI output connection.
    ///
    /// # Arguments
    ///
    /// * `midi_connection` - An established MIDI output connection from the `midir` crate
    ///
    /// # Example
    ///
    /// ```no_run
    /// use midir::MidiOutput;
    ///
    /// let midi_out = MidiOutput::new("My Sequencer").unwrap();
    /// let ports = midi_out.ports();
    /// let connection = midi_out.connect(&ports[0], "output").unwrap();
    ///
    /// let handler = MidiStepHandler::new(connection);
    /// ```
    pub fn new(midi_connection: MidiOutputConnection) -> Self {
        Self {
            midi_connection: Mutex::new(midi_connection),
        }
    }
}

impl StepHandler for MidiStepHandler {
    /// Handles a batch of sequencer events by converting them to MIDI messages.
    ///
    /// This method processes each event in the provided slice and sends the corresponding
    /// MIDI message through the configured output connection. Events are processed
    /// sequentially in the order they appear.
    ///
    /// # Arguments
    ///
    /// * `events` - A slice of tuples containing events and their associated step numbers
    ///
    /// # MIDI Message Format
    ///
    /// - `NoteOn` events are converted to MIDI Note On messages (status byte 0x90 + channel)
    /// - `NoteOff` events are converted to MIDI Note Off messages (status byte 0x80 + channel)
    /// - The track number from the event is used as the MIDI channel
    ///
    /// # Error Handling
    ///
    /// If a MIDI message fails to send, an error message is printed to stdout, but
    /// processing continues with the remaining events.
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
