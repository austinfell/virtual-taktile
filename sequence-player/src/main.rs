use helloworld_tonic::{Sequencer};
use helloworld_tonic::server::{SequencerServiceImpl, SequencerServiceServer, FILE_DESCRIPTOR_SET};
use tonic::transport::Server;
use tonic_reflection::server::Builder;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let addr = "[::1]:50051".parse()?;
    let sequencer = Sequencer::new();
    sequencer.start_playback_loop();

    let sequencer_service = SequencerServiceImpl::new(sequencer);

    println!("Sequencer service listening on {}", addr);

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
