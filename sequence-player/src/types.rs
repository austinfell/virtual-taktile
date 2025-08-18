use std::fmt;

pub use crate::server::sequence::{self, Note, NoteValue, Sequence, Subdivision, Trig};

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
        let note_value = NoteValue::try_from(self.value).unwrap_or(NoteValue::UnknownNote);
        write!(f, "{}{} (vel: {})", note_value, self.octave, self.velocity)
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

impl fmt::Display for Sequence {
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
