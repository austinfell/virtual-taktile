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
   :diminished-7 [0 3 6 9]})

(s/def ::chord-notes (s/coll-of ::kb/chromatic-note :min-count 0 :kind sequential?))
(s/def ::octave int?)
(s/def ::inversion int?)
(s/def ::chord (s/coll-of ::kb/note :kind vector?))
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
     (loop [result []
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
(defn identify-chords [notes]
  [(name (or (:name (first notes) "none")))])
