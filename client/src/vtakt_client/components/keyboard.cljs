(ns vtakt-client.components.keyboard
  (:require [clojure.walk :refer [postwalk]]
            [clojure.spec.alpha :as s]))

;; Music theory.
(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])
(def sharp-note-layout [nil :csdf :dsef nil :fsgf :gsaf :asbf])
(def natural-note-layout [:c :d :e :f :g :a :b])
(def chromatic-breakpoint-natural :c)
(def chromatic-breakpoint-accidental :csdf)

(defn- pscale-generator
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
(def scale-generator (memoize pscale-generator))

(s/def ::intervals (s/nilable (s/coll-of nat-int? :kind sequential?)))
(defn- pcreate-scale-group
  "Given a set of intervals, generates a scale for each chromatic note,
  with a root as the initial note and subsequent notes matching the intervals provided."
  [intervals]
  {:pre [(or (nil? intervals)
             (s/valid? ::intervals intervals))]}
  (let [scale-gen (scale-generator chromatic-notes (if (nil? intervals) [] intervals))]
    (into {} (map (fn [root-note] [root-note (scale-gen root-note)]) chromatic-notes))))
(s/def ::chromatic-note (into #{} chromatic-notes))
(s/def ::parallel-scale-mapping (s/map-of ::chromatic-note (s/coll-of ::chromatic-note :kind sequential?)))
(s/fdef create-scale-group
  :args (s/nilable (s/cat :intervals ::intervals))
  :ret ::parallel-scale-mapping)
(def create-scale-group (memoize pcreate-scale-group))

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

;; Notes and corresponding algorithms.
(defrecord Note [name octave])

(defn- pshift-note
  "Shifts a note up or down by the specified delta.
   For delta=1, increments the note.
   For delta=-1, decrements the note."
  [note direction]
  (when note
    (let [note-pos (.indexOf chromatic-notes (:name note))
          delta (case direction
                  :up 1
                  :down -1)
          next-note-pos (mod (+ note-pos delta) 12)
          next-note-name (get chromatic-notes next-note-pos)
          octave-change (cond
                          ;; When going up and moving from B to C
                          (and (= delta 1) (= (:name note) :b)) 1
                          ;; When going down and moving from C to B
                          (and (= delta -1) (= (:name note) :c)) -1
                          ;; Should not be hit as spec only allows -1 or 1.
                          :else 0)]
      (->Note
       next-note-name
       (+ (:octave note) octave-change)))))
(def shift-note (memoize pshift-note))

(s/def ::octave int?)
(s/def ::transposition-amount (s/int-in -2000 2001))
(s/def ::name ::chromatic-note)
(s/def ::note (s/keys :req-un [::name ::octave]))
(defn- ptranspose-note
  "Shifts a note by n semitones. Positive n shifts up, negative shifts down."
  [note n]
  {:pre [(and (s/valid? (s/nilable ::note) note) (s/valid? (s/nilable ::transposition-amount) n))]}
  (when (not (nil? note))
    (if (zero? n)
      note
      (let [direction (if (pos? n) :up :down)
            remaining-transposition (if (pos? n) (dec n) (inc n))
            shifted-note (shift-note note direction)]
        (if (zero? remaining-transposition)
          (map->Note shifted-note)
          (recur shifted-note remaining-transposition))))))

(s/fdef transpose-note
  :args (s/cat :note (s/nilable ::note) :n (s/nilable ::transposition-amount))
  :ret (s/nilable ::note))
(def transpose-note (memoize ptranspose-note))

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
                              (take 8 (drop offset (scale-generator chromatic-notes (into [] (range 12)))))))]
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
                                             (scale-generator chromatic-notes (into [] (range 12)))))))]
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

;; Chords
(def chords
  {:major (create-scale-group [0 4 7])
   :minor (create-scale-group [0 3 7])
   :dominant-7 (create-scale-group [0 4 7 10])
   :minor-7 (create-scale-group [0 3 7 10])
   :major-7 (create-scale-group [0 4 7 11])
   :diminished (create-scale-group [0 3 6])
   :diminished-7 (create-scale-group [0 3 6 9])})

(defn build-chord
  "Builds a chord with the given note names starting at the specified octave.
   Optional inversion parameter defaults to 0 (root position)."
  ([note-names octave]
   (build-chord note-names octave 0))
  ([note-names octave inversion]
   (let [num-notes (count note-names)
         shift (mod inversion num-notes)
         rotated-notes (vec (take num-notes (drop shift (cycle note-names))))]
     (loop [result []
            remaining-notes rotated-notes
            current-note (->Note (first rotated-notes) octave)]
       (if (empty? remaining-notes)
         result
         (if (= (first remaining-notes) (:name current-note))
           (recur (conj result current-note) (rest remaining-notes) (shift-note current-note :up))
           (recur result remaining-notes (shift-note current-note :up))))))))
