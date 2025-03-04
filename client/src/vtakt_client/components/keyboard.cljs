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

;; Keyboard Protocol and general utilities useful for all types of keyboards.
(defprotocol Keyboard
  (shift-left [this] "Shift the keyboard one step to the left")
  (shift-right [this] "Shift the keyboard one step to the right")
  (display [this] "Return a normalized representation of the keyboard")
  (filter-notes [this filter-fn] "Filter notes based on the provided predicate function")
  (map-notes [this map-fn] "Apply a transformation function to all notes on the keyboard."))

(defn create-note-filter-from-collection [c] (fn [n] (contains? (set c) (:name n))))

(defn create-chromatic-note-generator
  [first-note]
  (iterate #(shift-note % :up) first-note))

;; Implementation of ChromaticKeyboard.
(defn natural-note?
  "Returns true if the note is a natural note (not sharp/flat)"
  [note]
  (not (clojure.string/includes? (name (:name note)) "s")))

(def natural-note-to-flat-mapping
  {:c nil
   :d :csdf
   :e :dsef
   :f nil
   :g :fsgf
   :a :gsaf
   :b :asbf})

(defn get-flat-equivalent
  "Returns a new note with the flat equivalent name, or nil if no flat exists"
  [note]
  (when-let [flat-name (natural-note-to-flat-mapping (:name note))]
    (assoc note :name flat-name)))

(defn generate-chromatic-layout
  "Generates a chromatic keyboard layout with natural notes on bottom row
   and their corresponding flats on the top row, with optional filtering"
  ([root-note]
   (generate-chromatic-layout root-note nil))
  ([root-note map-fn]
   (let [all-notes (create-chromatic-note-generator root-note)
         natural-notes (filter natural-note? all-notes)
         bottom-row (take 8 natural-notes)
         filtered-bottom (if map-fn (mapv map-fn bottom-row) bottom-row)
         top-row (take 8 (map get-flat-equivalent bottom-row))
         filtered-top (if map-fn (mapv map-fn top-row) top-row)]
     {:bottom filtered-bottom
      :top filtered-top})))

(defrecord ChromaticKeyboard [root-note layout map-fn]
  Keyboard
  (shift-left [this]
    (let [new-root (shift-note root-note :down)]
      (ChromaticKeyboard. new-root (generate-chromatic-layout new-root map-fn) map-fn)))

  (shift-right [this]
    (let [new-root (shift-note root-note :up)]
      (ChromaticKeyboard. new-root (generate-chromatic-layout new-root map-fn) map-fn)))

  (display [this]
    (update layout :top (fn [top-row] (vec (concat [nil] (rest top-row))))))

  (filter-notes [this new-filter-fn]
    (let [new-map-fn (if (some? new-filter-fn) #(if (new-filter-fn %) % nil) identity)
          combined-fn (if map-fn
                        (comp new-map-fn map-fn)
                        new-map-fn)]
      (ChromaticKeyboard. root-note (generate-chromatic-layout root-note combined-fn) combined-fn)))

  (map-notes [this new-map-fn]
    (let [guaranteed-new-map-fn (if (some? new-map-fn) new-map-fn identity)
          combined-fn (if map-fn
                        (comp guaranteed-new-map-fn map-fn)
                         guaranteed-new-map-fn)]
      (ChromaticKeyboard. root-note (generate-chromatic-layout root-note combined-fn) combined-fn))))

(defn create-chromatic-keyboard
  "Creates a chromatic keyboard starting with the given root note, with optional filtering and mapping"
  ([root-note]
   (let [layout (generate-chromatic-layout root-note)]
     (->ChromaticKeyboard root-note layout nil))))

(defn generate-folding-layout
  ([root-note]
   (generate-folding-layout root-note nil))
  ([root-note transformations]
   (let [all-notes (create-chromatic-note-generator root-note)
         transformed-notes (reduce (fn [acc f] (f acc)) all-notes (if transformations transformations []))]
     (vec (take 16 transformed-notes)))))

(defrecord FoldingKeyboard [root-note notes transformations]
  Keyboard
  (shift-left [this]
    (let [new-root (shift-note root-note :down)]
      (FoldingKeyboard. new-root (generate-folding-layout new-root transformations) transformations)))

  (shift-right [this]
    (let [new-root (shift-note root-note :up)]
      (FoldingKeyboard. new-root (generate-folding-layout new-root transformations) transformations)))

  (display [this]
    {:bottom (subvec notes 0 8)
     :top (subvec notes 8 16)})

  (filter-notes [this new-filter-fn]
    (let [filter-impl (if new-filter-fn #(filter new-filter-fn %) #(filter (constantly true) %))
          new-transformations (if transformations (conj transformations filter-impl) [filter-impl])]
      (FoldingKeyboard. root-note (generate-folding-layout root-note new-transformations) new-transformations)))

  (map-notes [this new-map-fn]
    (let [map-impl (if new-map-fn #(map new-map-fn %) #(map identity %))
          new-transformations (if transformations (conj transformations map-impl) [map-impl])]
      (FoldingKeyboard. root-note (generate-folding-layout root-note new-transformations) new-transformations))))

(defn create-folding-keyboard
  "Creates a chromatic keyboard starting with the given root note, with optional filtering and mapping"
  ([root-note]
   (let [layout (generate-folding-layout root-note)]
     (->FoldingKeyboard root-note layout nil))))

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
