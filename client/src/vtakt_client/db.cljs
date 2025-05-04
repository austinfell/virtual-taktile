(ns vtakt-client.db
  (:require
   [vtakt-client.keyboard.chord :as chord]
   [vtakt-client.keyboard.core :as kb]))

(def default-db
  {:name "re-frame"
   :keyboard-root (kb/create-note :c 4)

   :selected-scale :chromatic

   :keyboard-mode :chromatic
   :keyboard-transpose 0
   :scales kb/scales

   ;;
   ;; Chord mode
   ;;
   :chromatic-chords chord/chromatic-chords
   :diatonic-chords chord/diatonic-chords

   :selected-chromatic-chord :single-note
   :selected-diatonic-chord :single-note

   :pressed-notes [] ;; User presses some arbitrary set of notes...
   :triggered-notes #{} ;; Depending on polyphony, those notes get filtered down
   :sounded-notes #{} ;; Depending on chord mode, those notes get converted into chords.

   :midi-outputs nil
   :selected-midi-output nil
   })
