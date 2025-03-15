(ns vtakt-client.db
  (:require
   [vtakt-client.components.keyboard :as kb]))

(def default-db
  {:name "re-frame"
   :keyboard-root (kb/create-note :c 4)

   :selected-chord :off
   :selected-scale :chromatic

   :keyboard-mode :chromatic
   :keyboard-transpose 0
   :chords kb/chords
   :scales kb/scales

   :pressed-notes []
   })
