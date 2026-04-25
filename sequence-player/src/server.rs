use crate::sequencer::{Sequencer, SequencerError};
use sequence::sequencer_service_server::SequencerService;
use sequence::{CueResponse, Empty, Sequence};
use tonic::{Request, Response, Status};
use std::sync::Mutex;

pub mod sequence {
    tonic::include_proto!("sequence");
}

pub use sequence::sequencer_service_server::SequencerServiceServer;
pub const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("sequence_descriptor");

#[derive(Debug)]
pub struct SequencerServiceImpl<T: Sequencer> {
    sequencer: Mutex<T>,
}

impl <T: Sequencer>SequencerServiceImpl<T> {
    pub fn new(sequencer: T) -> Self {
        let boxed_sequencer = Mutex::new(sequencer);
        Self {
            sequencer: boxed_sequencer
        }
    }
}

impl From<SequencerError> for Status {
    fn from(error: SequencerError) -> Self {
        match error {
            SequencerError::PlaybackNotInitialized => {
                Status::failed_precondition("Playback system not initialized")
            }
            SequencerError::CommandSendFailed => {
                Status::internal("Failed to send command to playback thread")
            }
            SequencerError::NoSequenceCued => {
                Status::failed_precondition("No sequence cued for playback")
            }
            SequencerError::Other(msg) => Status::internal(format!("Sequencer error: {}", msg)),
        }
    }
}

#[tonic::async_trait]
impl <T: Sequencer> SequencerService for SequencerServiceImpl<T> {
    async fn swap_sequence(&self, request: Request<Sequence>) -> Result<Response<Empty>, Status> {
        println!("Received a SwapSequence message");

        self.sequencer.lock().unwrap().swap_sequence(request.into_inner())?;
        Ok(Response::new(Empty {}))
    }

    async fn cue_sequence(
        &self,
        request: Request<Sequence>,
    ) -> Result<Response<CueResponse>, Status> {
        println!("Received a CueSequence message");

        let metadata = self.sequencer.lock().unwrap().cue_sequence(request.into_inner())?;

        println!("{:?}", metadata);

        Ok(Response::new(CueResponse {
            success: true, // Always true if we get here (no error)
            remaining_steps: metadata.remaining_steps as u32,
        }))
    }

    async fn start_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StartSequence message");

        self.sequencer.lock().unwrap().start_sequence()?;

        Ok(Response::new(Empty {}))
    }

    async fn stop_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StopSequence request");

        self.sequencer.lock().unwrap().stop_sequence()?;

        Ok(Response::new(Empty {}))
    }

    async fn pause_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a PauseSequence request");

        self.sequencer.lock().unwrap().pause_sequence()?;

        Ok(Response::new(Empty {}))
    }
}
