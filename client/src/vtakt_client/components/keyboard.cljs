(ns vtakt-client.components.keyboard)

;; ----------
;; General musical scale data that is useful for generating
;; keyboards.
;; ----------
;; Layouts as the appear on the keyboard of natural and sharps.
(def sharp-note-layout [nil :csdf :dsef nil :fsgf :gsaf :asbf])
(def natural-note-layout [:c :d :e :f :g :a :b ])
;; Chromatic notes and some extra metadata about them.
(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])
(def chromatic-breakpoint-natural :c)
(def chromatic-breakpoint-accidental :csdf)

(defn scale-filter-generator
  "Given a vector of sequential notes and associated scale degrees, returns
  another function that given one of the notes in that list will generate
  a scale with degrees using that note as a starting point.

  For example:
      ((scale-filter-generator chromatic-notes [0 2 4 5 7 9 11]) :c)
      => [:c :d :e :f :g :a :b]
      ((scale-filter-generator chromatic-notes [0 2 4 5 7 9 11]) :d)
      => [:d :e :fsgf :g :a :b :csdf]

   A few limitations: don't pass nil in. Don't pass scale-degrees that
   are negative or greater than the given list, don't pass a value into
   the resulting method that isn't in the scale you are using.
   "
  [notes scale-degrees]
  (fn [scale]
    (let [offset (.indexOf notes scale)
          buff-size (+ 1 (apply max scale-degrees))
          buff (into [] (take buff-size (drop offset (cycle notes))))]
      (set (mapv buff scale-degrees)))))

(defn create-note-group [intervals]
  (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes intervals) n)]) chromatic-notes)))

(def scales
  {:chromatic (create-note-group [0 1 2 3 4 5 6 7 8 9 10 11])
   :ionian (create-note-group [0 2 4 5 7 9 11])
   :dorian (create-note-group [0 2 3 5 7 9 10])
   :phrygian (create-note-group [0 1 3 5 7 8 10])
   :lydian (create-note-group [0 2 4 6 7 9 11])
   :mixolydian (create-note-group [0 2 4 5 7 9 10])
   :aeolian (create-note-group [0 2 3 5 7 8 10])
   :locrian (create-note-group [0 1 3 5 6 8 10])
   :minor-pentatonic (create-note-group [0 3 5 7 10])
   :major-pentatonic (create-note-group [0 2 4 7 9])
   :melodic-minor (create-note-group [0 2 3 5 7 9 11])
   :harmonic-minor (create-note-group [0 2 3 5 7 8 11])
   :whole-tone (create-note-group [0 2 4 6 8 10])
   :blues (create-note-group [0 3 5 6 7 10])
   :combo-minor (create-note-group [0 2 3 5 7 9 10])
   :persian (create-note-group [0 1 4 5 7 8 11])
   :iwato (create-note-group [0 1 5 6 10])
   :in-sen (create-note-group [0 1 5 7 10])
   :hirajoshi (create-note-group [0 2 3 5 7])
   :pelog (create-note-group [0 1 3 4 7])
   :phrygian-dominant (create-note-group [0 1 4 5 7 8 10])
   :whole-half-diminished (create-note-group [0 1 3 4 6 7 9 10])
   :half-whole-diminished (create-note-group [0 1 3 4 6 7 9 10])
   :spanish (create-note-group [0 1 4 5 7 8 11])
   :major-locrian (create-note-group [0 1 3 5 6 8 10])
   :super-locrian (create-note-group [0 1 3 4 6 8 10])
   :dorian-b2 (create-note-group [0 1 3 5 7 9 10])
   :lydian-augmented (create-note-group [0 2 4 6 8 9 11])
   :lydian-dominant (create-note-group [0 2 4 6 7 9 10])
   :double-harmonic-major (create-note-group [0 1 4 5 7 8 11])
   :lydian-#2-#6 (create-note-group [0 2 4 6 8 10 11])
   :ultraphrygian (create-note-group [0 1 3 5 7 8 9])
   :hungarian-minor (create-note-group [0 2 3 6 7 8 11])
   :oriental (create-note-group [0 1 4 5 7 8 10])
   :ionian-#2-#5 (create-note-group [0 2 4 6 7 8 11])
   :locrian-bb3-bb7 (create-note-group [0 1 3 5 6 8 9])})

;; ----------
;; Algorithms used to generate the structure of various keyboards
;; (folding, chromatic) on the DT.
;; ----------

(defrecord Note [name octave])
(defn- generate-octaves
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
      (lazy-seq (cons new-note (generate-octaves remaining-notes octave-split-point new-octave)))
      new-note)))

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
                              (take 8 (drop offset (generate-octaves (cycle layout) split-point)))))]
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
                                             (generate-octaves (cycle chromatic-notes) :c)))))]
    (->Keyboard
     (generate-row (+ offset 8))  ; Top row with offset shifted by 8
     (generate-row offset))))     ; Bottom row with original offset

;; ----------
;; Keyboards can have chords applied to them: different scale selections often alter
;; the end choices the user has. This implements both a base way to generate chord data
;; as well as a way to generate bitmaps across the Keyboard data structure defined above
;; that can be used by the presentation layer.
;; ----------
(def chords
  {:major (create-note-group [0 4 7])          ;; Root, Major 3rd, Perfect 5th
   :minor (create-note-group [0 3 7])          ;; Root, Minor 3rd, Perfect 5th
   :dominant-7 (create-note-group [0 4 7 10])  ;; Root, Major 3rd, Perfect 5th, Minor 7th
   :minor-7 (create-note-group [0 3 7 10])     ;; Root, Minor 3rd, Perfect 5th, Minor 7th
   :major-7 (create-note-group [0 4 7 11])     ;; Root, Major 3rd, Perfect 5th, Major 7th
   :diminished (create-note-group [0 3 6])     ;; Root, Minor 3rd, Diminished 5th
   :diminished-7 (create-note-group [0 3 6 9]) ;; Root, Minor 3rd, Diminished 5th, Diminished 7th
   })

(defn get-chord-notes
  [note octave chord]
  (take (count ((chords chord) note))
        (filter
         #(if (contains? ((chords chord) note) (:name %)) % nil)
         (drop-while #(or (not= (:name %) note) (not= (:octave %) octave))
                     (generate-octaves (cycle chromatic-notes) 0 :c)))))


;; TODO -  Implement individual chord generator.
