mod types;
mod server;

use server::{MySequencerService, SequencerServiceServer, FILE_DESCRIPTOR_SET};
use tonic::transport::Server;
use tonic_reflection::server::Builder;

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
