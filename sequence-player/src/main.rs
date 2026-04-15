use helloworld_tonic::server::{SequencerServiceImpl, SequencerServiceServer, FILE_DESCRIPTOR_SET};
use helloworld_tonic::CoreSequencer;
use helloworld_tonic::midi::{MidiStepHandler};
use midir::MidiOutput;
use tonic::transport::Server;
use tonic_reflection::server::Builder;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Wiring up MIDI
    let midi_out = MidiOutput::new("Sequencer").unwrap();
    let ports = midi_out.ports();
    let last_port = ports.iter().last().unwrap();
    println!("Last Port: {:?}", midi_out.port_name(last_port));
    let conn = midi_out.connect(last_port, "seq").unwrap();

    let step_handler = MidiStepHandler::new(conn);

    // Wiring up sequencer
    let sequencer = CoreSequencer::new(step_handler);
    let sequencer_service = SequencerServiceImpl::new(sequencer);

    // Wiring up server
    let addr = "[::1]:50051".parse()?;
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
