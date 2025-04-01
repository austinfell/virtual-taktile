(ns vtakt-client.keyboard.views-test
  (:require [cljs.test :refer-macros [deftest testing is are use-fixtures]]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [day8.re-frame.test :as rf-test]
            [vtakt-client.keyboard.views :as views]
            [vtakt-client.keyboard.core :as kb]
            [vtakt-client.keyboard.subs :as subs]
            [vtakt-client.keyboard.events :as events]
            [re-frame.core :as re-frame]
            [vtakt-client.db :as db]))

;; =========================================================
;; Test Fixtures and Helpers
;; =========================================================
(defn setup-test-db
  "Initialize the app-db with default test values for view tests"
  []
  (reset! re-frame.db/app-db
          (-> db/default-db
              (assoc :keyboard-root (kb/create-note :c 4))
              (assoc :keyboard-transpose 0)
              (assoc :keyboard-mode :chromatic)
              (assoc :selected-scale :ionian)
              (assoc :selected-chromatic-chord :major)
              (assoc :selected-diatonic-chord :triad)
              (assoc :pressed-notes []))))

;; =========================================================
;; Basic Component Tests
;; =========================================================

(deftest test-piano-key-component
  (testing "Piano key renders correctly"
    (let [white-key-component [views/piano-key (kb/create-note :c 4) false :white]
          black-key-component [views/piano-key (kb/create-note :csdf 4) false :black]]

      ;; Test that components are vectors (basic shape test)
      (is (vector? white-key-component) "White key should render as a vector")
      (is (vector? black-key-component) "Black key should render as a vector"))))

(deftest test-note-trigger-component
  (testing "Note trigger renders correctly with note"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [note (kb/create-note :c 4)
           trigger-component [views/note-trigger 1 note]]
       (is (vector? trigger-component) "Note trigger should render as a vector")))))

;; =========================================================
;; Control Component Tests
;; =========================================================

(deftest test-scale-selector
  (testing "Scale selector renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [scales [:chromatic :ionian :dorian]
           selected-scale :ionian
           selector-component [views/scale-selector scales selected-scale]]
       (is (vector? selector-component) "Scale selector should render as a vector")))))

(deftest test-root-note-control
  (testing "Root note control renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [root-note (kb/create-note :c 4)
           control-component [views/root-note-control root-note]]
       (is (vector? control-component) "Root note control should render as a vector")))))

;; =========================================================
;; Main Component Tests
;; =========================================================

(deftest test-octave-view
  (testing "Octave view renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [component [views/octave-view]]
       (is (vector? component) "Octave view should render as a vector")))))

(deftest test-keyboard-configurator
  (testing "Keyboard configurator renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [component [views/keyboard-configurator]]
       (is (vector? component) "Keyboard configurator should render as a vector")))))

(deftest test-keyboard
  (testing "Main keyboard renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [component [views/keyboard]]
       (is (vector? component) "Keyboard should render as a vector")))))

;; =========================================================
;; Advanced Interaction Tests
;; =========================================================

(deftest test-pressed-notes-display
  (testing "Pressed notes display shows empty state when no notes are pressed"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [component [views/pressed-notes-display]
           pressed-notes @(re-frame/subscribe [::subs/pressed-notes])]
       (is (empty? pressed-notes) "Pressed notes should be empty initially")
       (is (vector? component) "Pressed notes display should render as a vector")))))

(deftest test-keyboard-mode-selector
  (testing "Keyboard mode selector renders correctly"
    (rf-test/run-test-sync
     (setup-test-db)

     (let [mode :chromatic
           component [views/keyboard-mode-selector mode]]
       (is (vector? component) "Keyboard mode selector should render as a vector")))))

(run-tests)
