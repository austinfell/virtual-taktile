(ns vtakt-client.db
  (:require
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
   :chromatic-chords kb/chromatic-chords
   :diatonic-chords kb/diatonic-chords

   :selected-chromatic-chord :single-note
   :selected-diatonic-chord :single-note

   :pressed-notes []
   })
