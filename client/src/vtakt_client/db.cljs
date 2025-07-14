(ns vtakt-client.db
  (:require
   [vtakt-client.keyboard.chord :as chord]
   [vtakt-client.project.core :as pj]
   [vtakt-client.keyboard.core :as kb]))

(def default-db
  {;; Midi device is configurable globally across tracks.
   :selected-midi-output nil

   ;;
   ;; (Currently static) chord data. May be user definable in the future.
   ;;
   :chromatic-chords chord/chromatic-chords
   :diatonic-chords chord/diatonic-chords

   ;;
   ;; Keyboard data
   ;;
   ;; -> Base keyboard configuration
   :keyboard-root (kb/create-note :c 4)
   :selected-scale :chromatic
   :keyboard-mode :chromatic
   :keyboard-transpose 0
   :scales kb/scales
   ;; -> Chord mode selections
   :selected-chromatic-chord :single-note
   :selected-diatonic-chord :single-note
   ;; -> Active keyboard state - "what is being pressed down"
   :pressed-notes []    ;; User presses some arbitrary set of notes...
   :triggered-notes #{} ;; Depending on polyphony, those notes get filtered down
   :sounded-notes #{}   ;; Depending on chord mode, those notes get converted into chords.

   ;;
   ;; Base project data
   ;; See the project core module for exactly how this data structure is defined
   ;;
   :loaded-projects []
   :selected-projects #{}
   :current-project pj/initial-project

   ;; -> Pattern & track selections
   :active-pattern (pj/create-pattern-id 1 1)
   :active-track 1})
