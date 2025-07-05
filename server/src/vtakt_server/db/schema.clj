(ns vtakt-server.db.schema
  (:require [datomic.api :as d]))

(def vtakt-schema
  [;; Projects
   {:db/ident :project/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a VTakt project"}

   {:db/ident :project/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Name of the VTakt project"}

   {:db/ident :project/author
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Author of the VTakt project"}

   {:db/ident :project/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the project was created"}

   {:db/ident :project/updated-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the project was last updated"}

   {:db/ident :project/bpm
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one
    :db/doc "BPM (beats per minute) of the project"}

   {:db/ident :project/patterns
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Patterns in this project"}

   {:db/ident :project/sounds
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Sounds in this project"}

   ;; Patterns
   {:db/ident :pattern/bank
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Pattern bank (1-8)"}

   {:db/ident :pattern/number
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Pattern number (1-16)"}

   {:db/ident :pattern/project
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to the project this pattern belongs to"}

   {:db/ident :pattern/project+bank+number
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:pattern/project :pattern/bank :pattern/number]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique (project, bank, number) triple"}

   {:db/ident :pattern/length
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Length of pattern in steps"}

   {:db/ident :pattern/tracks
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Tracks in this pattern"}

   ;; Tracks
   {:db/ident :track/number
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Track number"}

   {:db/ident :track/pattern
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to the parent pattern associated with a track."}

   {:db/ident :track/pattern+number
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:track/pattern :track/number]
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique (pattern, number) tuple"}

   {:db/ident :track/steps
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Steps in this track"}

   {:db/ident :track/midi-channel
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Midi channel for this track to output on (0-15 if defined)"}

    ;; Steps
   {:db/ident :step/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a step"}

   {:db/ident :step/position
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Position of the step (0-15 for standard patterns)"}

   {:db/ident :step/note
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "MIDI note (e.g., 'C3', 'D#4')"}

   {:db/ident :step/velocity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Velocity of the note (0-127)"}

   {:db/ident :step/length
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Length of the note in ticks"}

   {:db/ident :step/active
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether this step is active"}

   {:db/ident :step/parameter-locks
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Parameter locks for this step"}

   ;; Parameter Locks
   {:db/ident :plock/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a parameter lock"}

   {:db/ident :plock/parameter
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Parameter being locked (e.g., :filter-cutoff, :attack)"}

   {:db/ident :plock/value
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Value of the parameter lock (0-127)"}

   ;; Sound
   {:db/ident :sound/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a sound"}

   {:db/ident :sound/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Name of the sound"}

   {:db/ident :sound/algorithm
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "FM algorithm (1-8)"}

   {:db/ident :sound/operators
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "FM operators for this sound"}

   {:db/ident :sound/filter-type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Filter type (e.g., :lpf, :hpf, :bpf)"}

   {:db/ident :sound/filter-cutoff
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Filter cutoff frequency (0-127)"}

   {:db/ident :sound/filter-resonance
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Filter resonance (0-127)"}

   {:db/ident :sound/envelope-settings
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "Envelope settings for this sound"}

   ;; FM Operator
   {:db/ident :operator/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for an FM operator"}

   {:db/ident :operator/number
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Operator number (1-4)"}

   {:db/ident :operator/level
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Operator output level (0-127)"}

   {:db/ident :operator/ratio
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one
    :db/doc "Frequency ratio"}

   {:db/ident :operator/feedback
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Operator feedback amount (0-127)"}

   ;; Envelope
   {:db/ident :envelope/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for an envelope"}

   {:db/ident :envelope/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Type of envelope (e.g., :amp, :filter, :pitch)"}

   {:db/ident :envelope/attack
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Attack time (0-127)"}

   {:db/ident :envelope/decay
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Decay time (0-127)"}

   {:db/ident :envelope/sustain
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Sustain level (0-127)"}

   {:db/ident :envelope/release
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Release time (0-127)"}])

;; Function to install the schema
(defn install-schema
  "Install the VTakt schema into the database"
  [conn]
  @(d/transact conn vtakt-schema))
