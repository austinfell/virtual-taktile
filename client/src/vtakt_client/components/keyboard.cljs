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

(def chromatic-scales
  (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [1 2 3 4 5 6 7 8 9 10 11 12]) n)]) chromatic-notes)))
(def major-scales
  (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [0 2 4 5 7 9 11]) n)]) chromatic-notes)))
(def minor-scales
  (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [0 2 3 5 7 8 10]) n)]) chromatic-notes)))
;; TODO - Get feature parity with DT here - need some extra scales...


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

(defn chromatic-keyboard [offset scale-filter]
  (let [sharps (map #(if (scale-filter (:name %)) % nil) (take 8 (drop offset (generate-octaves (cycle sharp-note-layout) :csdf))))
        naturals (map #(if (scale-filter (:name %)) % nil) (take 8 (drop offset (generate-octaves (cycle natural-note-layout) :c))))]
    [sharps naturals]))

(defn folding-keyboard [offset scale-filter]
  (let [top-row (take 8 (drop (+ offset 7) (filter #(scale-filter (:name %)) (generate-octaves (cycle chromatic-notes) :csdf))))
        bottom-row (take 8 (drop offset (filter #(scale-filter (:name %)) (generate-octaves (cycle chromatic-notes) :c))))]
    [top-row bottom-row]))

;; ----------
;; TODO - Keyboards can have chords applied to them: different scale selections often alter
;; the end choices the user has.
;; ----------
