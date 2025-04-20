(ns vtakt-client.midi.core
  (:require [re-frame.core :as re-frame]
            [vtakt-client.midi.events :as events]))

(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])

(defn note->midi-number
  "Convert a note map with :name and :octave to a MIDI note number.
   Example: {:name :c :octave 4} -> 60 (middle C)"
  [{:keys [name octave]}]
  (let [base-octave 4      ; MIDI standard: C4 (middle C) = 60
        c-index    (.indexOf chromatic-notes :c)
        note-index (.indexOf chromatic-notes name)]
    (if (= note-index -1)
      (throw (js/Error. (str "Invalid note name: " name)))
      (let [octave-offset  (* 12 (- octave base-octave))
            ; Handle note indices properly - :c is at position 3 in your scale
            ; We need to adjust based on this since we want :c4 = 60
            c-based-offset (mod (- note-index c-index) 12)
            middle-c       60]
        (+ middle-c octave-offset c-based-offset)))))
