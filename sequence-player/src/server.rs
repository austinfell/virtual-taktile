use sequence::sequencer_service_server::{SequencerService, SequencerServiceServer};
use sequence::{CueResponse, Empty, Sequence};
use std::fmt;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::interval;
use tonic::{Request, Response, Status, transport::Server};
use tonic_reflection::server::Builder;

pub mod sequence {
    tonic::include_proto!("sequence");
}

// Try to include the file descriptor set - this should work with the fixed build.rs
const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("sequence_descriptor");

// Native Rust data structures
#[derive(Debug, Clone, PartialEq)]
pub struct Subdivision {
    pub numerator: i64,
    pub denominator: i64,
}

#[derive(Debug, Clone, PartialEq)]
pub enum NoteValue {
    UnknownNote,
    C,
    CSharp,
    D,
    DSharp,
    E,
    F,
    FSharp,
    G,
    GSharp,
    A,
    ASharp,
    B,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Note {
    pub octave: i32,
    pub value: NoteValue,
    pub velocity: u32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Trig {
    pub note: Option<Note>,
    pub track: u32,
    pub step: u32,
    pub offset: f32,
    pub length: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct SequenceData {
    pub sequence_length: u32,
    pub trig_subdivision: Option<Subdivision>,
    pub trigs: Vec<Trig>,
}

// Display implementations
impl fmt::Display for Subdivision {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}/{}", self.numerator, self.denominator)
    }
}

impl fmt::Display for NoteValue {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let note_str = match self {
            NoteValue::UnknownNote => "?",
            NoteValue::C => "C",
            NoteValue::CSharp => "C#",
            NoteValue::D => "D",
            NoteValue::DSharp => "D#",
            NoteValue::E => "E",
            NoteValue::F => "F",
            NoteValue::FSharp => "F#",
            NoteValue::G => "G",
            NoteValue::GSharp => "G#",
            NoteValue::A => "A",
            NoteValue::ASharp => "A#",
            NoteValue::B => "B",
        };
        write!(f, "{}", note_str)
    }
}

impl fmt::Display for Note {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}{} (vel: {})", self.value, self.octave, self.velocity)
    }
}

impl fmt::Display for Trig {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let note_str = match &self.note {
            Some(note) => format!("{}", note),
            None => "REST".to_string(),
        };
        write!(
            f,
            "Track {}, Step {}: {} [offset: {:.2}, length: {:.2}]",
            self.track, self.step, note_str, self.offset, self.length
        )
    }
}

impl fmt::Display for SequenceData {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Sequence (length: {})", self.sequence_length)?;

        if let Some(subdivision) = &self.trig_subdivision {
            writeln!(f, "  Subdivision: {}", subdivision)?;
        }

        writeln!(f, "  Trigs ({}):", self.trigs.len())?;
        for (i, trig) in self.trigs.iter().enumerate() {
            writeln!(f, "    {}: {}", i + 1, trig)?;
        }

        Ok(())
    }
}

// Conversion from protobuf to native structs
impl From<sequence::NoteValue> for NoteValue {
    fn from(proto_value: sequence::NoteValue) -> Self {
        match proto_value {
            sequence::NoteValue::UnknownNote => NoteValue::UnknownNote,
            sequence::NoteValue::C => NoteValue::C,
            sequence::NoteValue::CSharp => NoteValue::CSharp,
            sequence::NoteValue::D => NoteValue::D,
            sequence::NoteValue::DSharp => NoteValue::DSharp,
            sequence::NoteValue::E => NoteValue::E,
            sequence::NoteValue::F => NoteValue::F,
            sequence::NoteValue::FSharp => NoteValue::FSharp,
            sequence::NoteValue::G => NoteValue::G,
            sequence::NoteValue::GSharp => NoteValue::GSharp,
            sequence::NoteValue::A => NoteValue::A,
            sequence::NoteValue::ASharp => NoteValue::ASharp,
            sequence::NoteValue::B => NoteValue::B,
        }
    }
}

impl From<sequence::Subdivision> for Subdivision {
    fn from(proto_subdivision: sequence::Subdivision) -> Self {
        Subdivision {
            numerator: proto_subdivision.numerator,
            denominator: proto_subdivision.denominator,
        }
    }
}

impl From<sequence::Note> for Note {
    fn from(proto_note: sequence::Note) -> Self {
        Note {
            octave: proto_note.octave,
            value: sequence::NoteValue::try_from(proto_note.value)
                .unwrap_or(sequence::NoteValue::UnknownNote)
                .into(),
            velocity: proto_note.velocity,
        }
    }
}

impl From<sequence::Trig> for Trig {
    fn from(proto_trig: sequence::Trig) -> Self {
        Trig {
            note: proto_trig.note.map(|n| n.into()),
            track: proto_trig.track,
            step: proto_trig.step,
            offset: proto_trig.offset,
            length: proto_trig.length,
        }
    }
}

impl From<Sequence> for SequenceData {
    fn from(proto_sequence: Sequence) -> Self {
        SequenceData {
            sequence_length: proto_sequence.sequence_length,
            trig_subdivision: proto_sequence.trig_subdivision.map(|s| s.into()),
            trigs: proto_sequence.trigs.into_iter().map(|t| t.into()).collect(),
        }
    }
}

#[derive(Debug)]
pub struct MySequencerService {
    state: Arc<Mutex<SequencerState>>,
}

impl MySequencerService {
    pub fn new() -> Self {
        Self {
            state: Arc::new(Mutex::new(SequencerState::default())),
        }
    }

    // Start the playback loop in the background
    pub fn start_playback_loop(&self) {
        let state = Arc::clone(&self.state);

        tokio::spawn(async move {
            let mut ticker = interval(Duration::from_secs(1));
            let mut current_step = 0u32;

            loop {
                ticker.tick().await;

                // Check if we should be playing and get current sequence
                let should_play = {
                    let state_guard = state.lock().unwrap();
                    state_guard.playing && state_guard.current_sequence.is_some()
                };

                if should_play {
                    let sequence_data = {
                        let state_guard = state.lock().unwrap();
                        state_guard.current_sequence.clone().unwrap()
                    };

                    // Print step header
                    println!(
                        "🎵 Step {} of {}:",
                        current_step, sequence_data.sequence_length
                    );

                    // Find and print all trigs for this step
                    let step_trigs: Vec<&Trig> = sequence_data
                        .trigs
                        .iter()
                        .filter(|trig| trig.step == current_step)
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

                    // Advance to next step (loop back to 0 when we reach sequence length)
                    current_step = (current_step + 1) % sequence_data.sequence_length;
                } else {
                    // Reset step counter when not playing
                    current_step = 0;
                }
            }
        });
    }
}

impl Default for MySequencerService {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Default)]
struct SequencerState {
    current_sequence: Option<SequenceData>,
    cued_sequence: Option<SequenceData>,
    playing: bool,
}

#[tonic::async_trait]
impl SequencerService for MySequencerService {
    async fn swap_sequence(
        &self,
        request: Request<Sequence>,
    ) -> Result<Response<Empty>, Status> {
        println!("Got a SwapSequence request");

        let sequence_data: SequenceData = request.into_inner().into();

        println!("{}", sequence_data);

        // TODO - Eventually we could diff the sequence and do a less intrusive swap...
        let mut state = self.state.lock().unwrap();
        let replaced_existing = state.current_sequence.is_some();
        state.current_sequence = Some(sequence_data);

        if replaced_existing {
            println!("Replaced existing playing sequence");
        } else {
            println!("Replaced existing sequence");
        }

        Ok(Response::new(Empty {}))
    }

    async fn cue_sequence(
        &self,
        request: Request<Sequence>,
    ) -> Result<Response<CueResponse>, Status> {
        println!("Got a CueSequence request");

        let proto_sequence = request.into_inner();

        // Convert to native Rust structs
        let sequence_data: SequenceData = proto_sequence.into();

        // Use the Display implementation to echo
        println!("{}", sequence_data);

        // Update the cued sequence in our state
        let mut state = self.state.lock().unwrap();
        let replaced_existing = state.cued_sequence.is_some();
        state.cued_sequence = Some(sequence_data);

        if replaced_existing {
            println!("Replaced existing cued sequence");
        } else {
            println!("Cued new sequence");
        }

        // Return a mock response
        Ok(Response::new(CueResponse {
            success: true,
            remaining_steps: 16, // Mock value
        }))
    }

    async fn start_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        let mut state = self.state.lock().unwrap();

        if let Some(cued_sequence) = state.cued_sequence.take() {
            state.current_sequence = Some(cued_sequence);
            state.playing = true;
            println!("▶️  Started cued sequence - now playing!");
        } else {
            println!("❌ No sequence cued - cannot start");
        }

        Ok(Response::new(Empty {}))
    }

    async fn stop_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        let mut state = self.state.lock().unwrap();
        state.playing = false;
        println!("⏹️  Stopped sequence");

        if let Some(current) = &state.current_sequence {
            println!("Stopped sequence had {} trigs", current.trigs.len());
        }

        Ok(Response::new(Empty {}))
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let addr = "[::1]:50051".parse()?;
    let sequencer_service = MySequencerService::new();

    // Start the playback loop
    sequencer_service.start_playback_loop();

    println!("🎛️  Sequencer service listening on {}", addr);
    println!("📡 gRPC reflection enabled - you can use grpcurl to explore the API");

    // Build the reflection service
    let reflection_service = Builder::configure()
        .register_encoded_file_descriptor_set(FILE_DESCRIPTOR_SET)
        .build()
        .unwrap();

    Server::builder()
        .add_service(SequencerServiceServer::new(sequencer_service))
        .add_service(reflection_service)
        .serve(addr)
        .await?;

    Ok(())
}
