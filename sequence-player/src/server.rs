use sequence::sequencer_service_server::{SequencerService};

use sequence::{CueResponse, Empty, Sequence};
use tonic::{Request, Response, Status};

use crate::sequencer::Sequencer;

pub mod sequence {
    tonic::include_proto!("sequence");
}

pub use sequence::sequencer_service_server::SequencerServiceServer;
pub const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("sequence_descriptor");

#[derive(Debug)]
pub struct SequencerServiceImpl {
    sequencer: Sequencer,
}

impl SequencerServiceImpl {
    pub fn new(s: Sequencer) -> Self {
        Self {
            sequencer: s
        }
    }
}

#[tonic::async_trait]
impl SequencerService for SequencerServiceImpl {
    async fn swap_sequence(&self, request: Request<Sequence>) -> Result<Response<Empty>, Status> {
        println!("Received a SwapSequence message");
        let result = self.sequencer.swap_sequence(request.into_inner());

        if result.success {
            Ok(Response::new(Empty {}))
        } else {
            Err(Status::internal("Failed to swap sequence"))
        }
    }

    async fn cue_sequence(&self, request: Request<Sequence>,) -> Result<Response<CueResponse>, Status> {
        println!("Received a CueSequence message");
        let result = self.sequencer.cue_sequence(request.into_inner());

        if result.success {
            Ok(Response::new(CueResponse {
                success: result.success,
                remaining_steps: if let Some(metadata) = &result.data {
                    metadata.remaining_steps
                } else {
                    0 // or some default value
                },
            }))
        } else {
            Err(Status::internal("Failed to cue sequence"))
        }
    }

    async fn start_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StartSequence message");
        let result = self.sequencer.start_sequence();

        if result.success {
            Ok(Response::new(Empty {}))
        } else {
            Err(Status::internal("Failed to start sequence"))
        }
    }

    async fn stop_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StopSequence request");
        let result = self.sequencer.stop_sequence();

        if result.success {
            Ok(Response::new(Empty {}))
        } else {
            Err(Status::internal("Failed to stop sequence"))
        }
    }
}
