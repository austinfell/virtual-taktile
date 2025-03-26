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
   :chromatic-chords kb/chords
   ;; TODO - Eventually we will want to use these instead of hardcoding assumptions in
   ;; event handler...
   :scale-chords {:single-note [0] :triad [0 2 4]}
   :selected-chromatic-chord :single-note

   :pressed-notes []
   })
