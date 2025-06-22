(ns vtakt-client.db
 (:require
   [vtakt-client.keyboard.chord :as chord]
   [vtakt-client.project.core :as pj]
   [vtakt-client.keyboard.core :as kb]))

(def num-tracks 4)

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

   :current-project (pj/->Project nil "Untitled" "Austin Fell" "Tue May 20 09:41:07 EDT 2025")
   :project-name ""
   ;; TODO We should convert this to keyed map of ids to objects. Easier to work with.
   :loaded-projects []
   :selected-projects #{}

   :selected-track 1
   :available-tracks (into [] (range 1 (+ 1 num-tracks)))

   :midi-outputs nil
   :selected-midi-channel 0
   :selected-midi-output nil})
