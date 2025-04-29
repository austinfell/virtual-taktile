(ns vtakt-client.keyboard.chord
  (:require
   [vtakt-client.keyboard.core :as kb]
   [clojure.spec.alpha :as s]))

(def chromatic-chords
  {:single-note [0]
   :major [0 4 7]
   :minor [0 3 7]
   :dominant-7 [0 4 7 10]
   :minor-7 [0 3 7 10]
   :major-7 [0 4 7 11]
   :diminished [0 3 6]
   :diminished-7 [0 3 6 9]
   :augmented [0 4 8]
   :suspended-2 [0 2 7]
   :suspended-4 [0 5 7]
   :major-6 [0 4 7 9]
   :minor-6 [0 3 7 9]
   :minor-major-7 [0 3 7 11]
   :half-diminished-7 [0 3 6 10]
   :augmented-7 [0 4 8 10]
   :augmented-major-7 [0 4 8 11]
   :dominant-9 [0 4 7 10 2]
   :dominant-11 [0 4 7 10 2 5]
   :dominant-13 [0 4 7 10 2 5 9]
   :major-9 [0 4 7 11 2]
   :major-11 [0 4 7 11 2 5]
   :major-13 [0 4 7 11 2 5 9]
   :minor-9 [0 3 7 10 2]
   :minor-11 [0 3 7 10 2 5]
   :minor-13 [0 3 7 10 2 5 9]
   :add-9 [0 4 7 2]
   :add-11 [0 4 7 5]
   :add-13 [0 4 7 9]
   :minor-add-9 [0 3 7 2]
   :6-9 [0 4 7 9 2]
   :minor-6-9 [0 3 7 9 2]
   :7-flat-5 [0 4 6 10]
   :7-sharp-5 [0 4 8 10]
   :7-flat-9 [0 4 7 10 1]
   :7-sharp-9 [0 4 7 10 3]
   :7-flat-5-flat-9 [0 4 6 10 1]
   :7-sharp-5-flat-9 [0 4 8 10 1]
   :7-flat-5-sharp-9 [0 4 6 10 3]
   :7-sharp-5-sharp-9 [0 4 8 10 3]
   :9-flat-5 [0 4 6 10 2]
   :9-sharp-5 [0 4 8 10 2]
   :7-sus4 [0 5 7 10]
   :7-sus2 [0 2 7 10]
   :13-sus4 [0 5 7 10 2 5 9]
   :13-sus2 [0 2 7 10 2 5 9]
   :5 [0 7]
   :quartal [0 5 10 3]
   :quintal [0 7 2 9]})

(s/def ::chord-notes (s/coll-of ::kb/chromatic-note :min-count 0 :kind sequential?))
(s/def ::octave int?)
(s/def ::inversion int?)
(s/def ::chord (s/coll-of ::kb/note :kind set?))
(s/fdef build-chord
  :args (s/alt :two-args (s/cat :note-names ::chord-notes
                                :octave ::octave)
               :three-args (s/cat :note-names ::chord-notes
                                  :octave ::octave
                                  :inversion ::inversion))
  :ret ::chord
  :fn (fn [{:keys [args ret]}]
        (let [note-names (:note-names (second args))
              empty-input? (empty? note-names)]
          (if empty-input?
            (empty? ret)
            (= (count note-names) (count ret))))))
(defn build-chord
  "Builds a chord with the given note names starting at the specified octave.
   Optional inversion parameter defaults to 0 (root position)."
  ([note-names octave]
   (build-chord note-names octave 0))
  ([note-names octave inversion]
   (let [num-notes (count note-names)
         shift (mod inversion num-notes)
         rotated-notes (vec (take num-notes (drop shift (cycle note-names))))]
     (loop [result #{}
            remaining-notes rotated-notes
            current-note (kb/create-note (first rotated-notes) octave)]
       (if (empty? remaining-notes)
         result
         (if (= (first remaining-notes) (:name current-note))
           (recur (conj result current-note) (rest remaining-notes) (kb/shift-note current-note :up))
           (recur result remaining-notes (kb/shift-note current-note :up))))))))

(s/def ::scale-notes (s/coll-of ::kb/chromatic-note :min-count 0 :kind sequential?))
(s/def ::root-note ::kb/note)
(s/def ::chord (s/coll-of ::kb/note :kind set?))
(s/fdef build-scale-chord
  :args (s/cat :scale-notes ::scale-notes
               :root-note ::root-note)
  :ret ::chord)
(defn build-scale-chord
  [scale-notes root-note positions]
  (if (empty? scale-notes)
    #{}
    (let [scale-size (count scale-notes)
          root-index (.indexOf (vec scale-notes) (:name root-note))]
      (if (= root-index -1)
        #{}
        (let [chord-note-names (mapv (fn [pos]
                                       (nth scale-notes
                                            (mod (+ root-index pos) scale-size)))
                                     positions)]
          (build-chord chord-note-names (:octave root-note)))))))

(def diatonic-chords
  {:single-note [0]
   :diad [0 2]
   :triad [0 2 4]
   :seventh [0 2 4 6]
   :ninth [0 2 4 6 8]
   :eleventh [0 2 4 6 8 10]
   :thirteenth [0 2 4 6 8 12]
   :sus-2 [0 1 4]
   :sus-4 [0 3 4]
   :six-nine [0 2 4 5 8]
   :quartal-within-scale [0 3 6]
   :shell-seventh [0 2 6]})

;; TODO UNDER CONSTRUCTION
;; Chord Identification - function currently stubbed
(defn create-chord-name-mapping [chord-type]
  (reduce
   (fn [acc kw]
     (update acc
             (into #{} (map :name (build-scale-chord
                                   kb/chromatic-notes
                                   {:name kw :octave 4}
                                   (chord-type chromatic-chords))))
             #(conj (into #{} %) (str (clojure.string/capitalize (name kw)) " " (clojure.string/capitalize (name chord-type))))))
   {}
   kb/chromatic-notes))

(defn create-merged-chord-mappings [chord-types]
  (apply merge-with into
         (map (fn [chord-type]
                (create-chord-name-mapping chord-type))
              chord-types)))

(def chord-mappings (create-merged-chord-mappings (keys chromatic-chords)))

(chord-mappings #{:c :e :g})

;; General idea here - let's statically generate a key value mapping of
;; *sets of notes* to chord names. That way we get quick and easy (not
;; to mention fast) interface for getting names of chords.
;;
;; First, we will iterate through a dictionary of chromatic chords
;; - we're gonna need significantly more than we have displayed
;; in the UI.
;;
;; We have to progressively join all of these maps into
;; a full database.
(defn identify-chords [notes]
  (or (chord-mappings (into #{} (map :name notes))) #{"Unknown"}))
