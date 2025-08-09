fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_build::configure()
        .file_descriptor_set_path("src/sequence_descriptor.bin")
        .compile(&["proto/sequence.proto"], &["proto/"])?;
    Ok(())
}
