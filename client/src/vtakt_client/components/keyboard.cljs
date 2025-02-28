(ns vtakt-client.components.keyboard
  (:require [clojure.walk :refer [postwalk]]
            [clojure.spec.alpha :as s]))

;; Music theory.
(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])
(def sharp-note-layout [nil :csdf :dsef nil :fsgf :gsaf :asbf])
(def natural-note-layout [:c :d :e :f :g :a :b ])
(def chromatic-breakpoint-natural :c)
(def chromatic-breakpoint-accidental :csdf)

(defn- scale-generator
  "Given a vector of sequential notes and associated scale degrees, returns
  another function that given one of the notes in that list will generate
  a scale with degrees using that note as a starting point.

  For example:
      ((scale-generator chromatic-notes [0 2 4 5 7 9 11]) :c)
      => [:c :d :e :f :g :a :b]
      ((scale-generator chromatic-notes [0 2 4 5 7 9 11]) :d)
      => [:d :e :fsgf :g :a :b :csdf]
  "
  [notes scale-degrees]
  (fn [scale]
    (let [offset (.indexOf notes scale)
          buff-size (inc (apply max scale-degrees))
          buff (->> notes
                    cycle
                    (drop offset)
                    (take buff-size)
                    (into []))]
      (mapv buff scale-degrees))))

(s/def ::intervals (s/coll-of nat-int? :kind vector?))
(s/def ::chromatic-note (into #{} chromatic-notes))
(s/def ::parallel-scale-mapping (s/map-of ::chromatic-note (s/coll-of ::chromatic-note :kind vector?)))

(s/fdef create-scale-group
  :args (s/cat :intervals ::intervals)
  :ret ::parallel-scale-mapping)
(defn create-scale-group
  "Given a set of intervals, generates a scale for each chromatic note,
  with a root as the initial note and subsequent notes matching the intervals provided."
  [intervals]
  {:pre [(s/valid? ::intervals intervals)]
   :post [(s/valid? ::parallel-scale-mapping %)]}
  (let [scale-gen (scale-generator chromatic-notes intervals)]
    (into {} (map (fn [root-note] [root-note (scale-gen root-note)]) chromatic-notes))))

(def scales
  {:chromatic (create-scale-group [0 1 2 3 4 5 6 7 8 9 10 11])
   :ionian (create-scale-group [0 2 4 5 7 9 11])
   :dorian (create-scale-group [0 2 3 5 7 9 10])
   :phrygian (create-scale-group [0 1 3 5 7 8 10])
   :lydian (create-scale-group [0 2 4 6 7 9 11])
   :mixolydian (create-scale-group [0 2 4 5 7 9 10])
   :aeolian (create-scale-group [0 2 3 5 7 8 10])
   :locrian (create-scale-group [0 1 3 5 6 8 10])
   :minor-pentatonic (create-scale-group [0 3 5 7 10])
   :major-pentatonic (create-scale-group [0 2 4 7 9])
   :melodic-minor (create-scale-group [0 2 3 5 7 9 11])
   :harmonic-minor (create-scale-group [0 2 3 5 7 8 11])
   :whole-tone (create-scale-group [0 2 4 6 8 10])
   :blues (create-scale-group [0 3 5 6 7 10])
   :combo-minor (create-scale-group [0 2 3 5 7 9 10])
   :persian (create-scale-group [0 1 4 5 7 8 11])
   :iwato (create-scale-group [0 1 5 6 10])
   :in-sen (create-scale-group [0 1 5 7 10])
   :hirajoshi (create-scale-group [0 2 3 5 7])
   :pelog (create-scale-group [0 1 3 4 7])
   :phrygian-dominant (create-scale-group [0 1 4 5 7 8 10])
   :whole-half-diminished (create-scale-group [0 1 3 4 6 7 9 10])
   :half-whole-diminished (create-scale-group [0 1 3 4 6 7 9 10])
   :spanish (create-scale-group [0 1 4 5 7 8 11])
   :major-locrian (create-scale-group [0 1 3 5 6 8 10])
   :super-locrian (create-scale-group [0 1 3 4 6 8 10])
   :dorian-b2 (create-scale-group [0 1 3 5 7 9 10])
   :lydian-augmented (create-scale-group [0 2 4 6 8 9 11])
   :lydian-dominant (create-scale-group [0 2 4 6 7 9 10])
   :double-harmonic-major (create-scale-group [0 1 4 5 7 8 11])
   :lydian-#2-#6 (create-scale-group [0 2 4 6 8 10 11])
   :ultraphrygian (create-scale-group [0 1 3 5 7 8 9])
   :hungarian-minor (create-scale-group [0 2 3 6 7 8 11])
   :oriental (create-scale-group [0 1 4 5 7 8 10])
   :ionian-#2-#5 (create-scale-group [0 2 4 6 7 8 11])
   :locrian-bb3-bb7 (create-scale-group [0 1 3 5 6 8 9])})

(def chords
  {:major (create-scale-group [0 4 7])
   :minor (create-scale-group [0 3 7])
   :dominant-7 (create-scale-group [0 4 7 10])
   :minor-7 (create-scale-group [0 3 7 10])
   :major-7 (create-scale-group [0 4 7 11])
   :diminished (create-scale-group [0 3 6])
   :diminished-7 (create-scale-group [0 3 6 9])
   })

;; TODO - Consider how to decouple this from global chord map.
(defn get-chord-notes
  "Returns a set of notes from the specified chord that match the given note and octave.

  Takes a `note` (keyword), an `octave` (non-negative integer), and a `chord` (keyword)
  to filter and return the corresponding notes from the chromatic scale."
  [note octave chord]
  (let [chord-notes (set ((chords chord) note))]
    (->> (scale-generator (cycle chromatic-notes) 0)
         (drop-while #(or (not= (:name %) note) (not= (:octave %) octave)))
         (filter #(chord-notes (:name %)))
         (take (count chord-notes))
         set)))

;; Notes and corresponding algorithms.
(defrecord Note [name octave])

(defn transpose-note
  "Transposes a Note by the given number of semitones.

  Takes a `Note` (a record with `:name` and `:octave` keys) and a `semitones` (number of
  semitones to transpose). Returns the transposed Note, or `nil` if the input Note is `nil`."
  [note semitones]
  (when note
    (->> (scale-generator (cycle chromatic-notes) 0)
         (drop-while #(not= % note))
         (drop semitones)
         first)))

(defn note-generator
  "Generates an infinite sequence of ascending octaves from a set of cyclical notes.

  Takes a sequence of `notes`, an `octave-split-point`, and an optional starting `octave`.
  The sequence of notes starts at `octave` and will cycle through the notes, returning
  a sequence of `Note` objects with a name and the octave they belong to.

  If no `octave` is provided, it defaults to 0."
  [notes octave-split-point & [octave]]
  (let [octave (or octave 0)
        [note & remaining-notes] notes
        new-octave (if (= note octave-split-point) (inc octave) octave)
        new-note (map->Note {:octave new-octave :name note})]
    (if (seq remaining-notes)
      (lazy-seq (cons new-note (note-generator remaining-notes octave-split-point new-octave)))
      new-note)))

;; Keyboard data structure and creation & manipulation algorithms.
(defrecord Keyboard [top-row bottom-row])

(defn chromatic-keyboard
  "Generates a standard chromatic keyboard with sharp notes on the top row and natural notes on the bottom row.
   The top row will contain 8 notes, some of which are potentially nil to represent lack of a value, as will
   the bottom row.

  `offset` aligns the keyboard starting point to a given root note and octave.
  `scale-filter` is a function that filters the notes; notes that don't match the filter will be replaced with nil."
  [offset scale-filter]
  (let [generate-notes (fn [layout split-point]
                         (map #(if (scale-filter (:name %)) % nil)
                              (take 8 (drop offset (note-generator (cycle layout) split-point)))))]
    (->Keyboard
     (generate-notes sharp-note-layout :csdf)
     (generate-notes natural-note-layout :c))))

(defn folding-keyboard
  "Generates a folding keyboard where each column corresponds to a valid note in the given scale.

  `offset` aligns the keyboard starting point to a given root note and octave.
  `scale-filter` is a function that filters the notes; only notes that match the filter will be included."
  [offset scale-filter]
  (let [generate-row (fn [offset]
                       (take 8 (drop offset
                                     (filter #(scale-filter (:name %))
                                             (note-generator (cycle chromatic-notes) :c)))))]
    (->Keyboard
     (generate-row (+ offset 8))  ; Top row with offset shifted by 8
     (generate-row offset))))     ; Bottom row with original offset

(defn modify-notes-on-keyboard
  "Applies a function to all `Note` objects in a Keyboard.

  Takes a `Keyboard` (a record with `:top-row` and `:bottom-row` keys, each containing
  sequences of `Note` records) and a `f` (a function to apply to each `Note`).
  Returns a new Keyboard with the function applied to each `Note`, preserving the
  original structure."
  [kb f]
  (postwalk #(if (instance? Note %) (f %) %) kb))

(defn retain-notes-on-keyboard
  "Returns a modified keyboard with only the notes present in the provided `notes` set.
   Notes is expected to be the Note record type."
  [kb notes]
  (modify-notes-on-keyboard kb #(if (notes %) % nil)))

(defn transpose-keyboard
  "Transposes all Notes in a Keyboard by the given number of semitones."
  [kb semitones]
  (modify-notes-on-keyboard kb #(transpose-note % semitones)))
