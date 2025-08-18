use crate::sequencer::{Sequencer, SequencerError, StepHandler};
use sequence::sequencer_service_server::SequencerService;
use sequence::{CueResponse, Empty, Sequence};
use tonic::{Request, Response, Status};

pub mod sequence {
    tonic::include_proto!("sequence");
}

pub use sequence::sequencer_service_server::SequencerServiceServer;
pub const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("sequence_descriptor");

#[derive(Debug)]
pub struct SequencerServiceImpl<H: StepHandler> {
    sequencer: Sequencer<H>,
}

impl<H: StepHandler> SequencerServiceImpl<H> {
    pub fn new(sequencer: Sequencer<H>) -> Self {
        Self { sequencer }
    }
}

// Helper function to convert SequencerError to tonic::Status
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
impl<H: StepHandler> SequencerService for SequencerServiceImpl<H> {
    async fn swap_sequence(&self, request: Request<Sequence>) -> Result<Response<Empty>, Status> {
        println!("Received a SwapSequence message");

        // Convert the result directly using ? operator
        self.sequencer.swap_sequence(request.into_inner())?;

        Ok(Response::new(Empty {}))
    }

    async fn cue_sequence(
        &self,
        request: Request<Sequence>,
    ) -> Result<Response<CueResponse>, Status> {
        println!("Received a CueSequence message");

        // Use the ? operator and pattern matching on the success case
        let metadata = self.sequencer.cue_sequence(request.into_inner())?;

        Ok(Response::new(CueResponse {
            success: true, // Always true if we get here (no error)
            remaining_steps: metadata.remaining_steps,
        }))
    }

    async fn start_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StartSequence message");

        // Simple conversion using ? operator
        self.sequencer.start_sequence()?;

        Ok(Response::new(Empty {}))
    }

    async fn stop_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StopSequence request");

        // Convert the result using ? operator
        self.sequencer.stop_sequence()?;

        Ok(Response::new(Empty {}))
    }
}
