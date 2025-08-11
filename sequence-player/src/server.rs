use sequence::sequencer_service_server::{SequencerService};

use sequence::{CueResponse, Empty, Sequence};
use tonic::{Request, Response, Status};

use crate::types::SequenceData;
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

    pub fn sequencer(&self) -> &Sequencer {
        &self.sequencer
    }
}

#[tonic::async_trait]
impl SequencerService for SequencerServiceImpl {
    async fn swap_sequence(&self, request: Request<Sequence>) -> Result<Response<Empty>, Status> {
        println!("Got a SwapSequence request");

        let sequence_data: SequenceData = request.into_inner().into();

        let result = self.sequencer.swap_sequence(sequence_data);

        if result.success {
            Ok(Response::new(Empty {}))
        } else {
            Err(Status::internal("Failed to swap sequence"))
        }
    }

    async fn cue_sequence(
        &self,
        request: Request<Sequence>,
    ) -> Result<Response<CueResponse>, Status> {
        println!("Got a CueSequence request");
        let sequence_data: SequenceData = request.into_inner().into();

        let result = self.sequencer.cue_sequence(sequence_data);

        Ok(Response::new(CueResponse {
            success: result.success,
            remaining_steps: result.remaining_steps,
        }))
    }

    async fn start_sequence(&self, _request: Request<Empty>) -> Result<Response<Empty>, Status> {
        println!("Got a StartSequence request");

        let result = self.sequencer.start_sequence();
        if result.success {
            Ok(Response::new(Empty {}))
        } else {
            Err(Status::failed_precondition(result.message))
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
