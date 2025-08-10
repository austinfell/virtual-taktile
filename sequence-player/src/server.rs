use sequence::sequencer_service_server::{SequencerService};
use sequence::{CueResponse, Empty, Sequence};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::interval;
use tonic::{Request, Response, Status};

// Import our extracted types
use crate::types::{SequenceData, Trig};

pub mod sequence {
    tonic::include_proto!("sequence");
}

// Make these public so main.rs can use them
pub use sequence::sequencer_service_server::SequencerServiceServer;
pub const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("sequence_descriptor");

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
    async fn swap_sequence(&self, request: Request<Sequence>) -> Result<Response<Empty>, Status> {
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

        Ok(Response::new(CueResponse {
            success: true,
            // We need to rework the way our IO subsystem works to allow messaging of where it is at here.
            remaining_steps: 16,
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
