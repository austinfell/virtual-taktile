(ns vtakt-client.keyboard.events-test
  (:require [cljs.test :refer-macros [deftest testing is are run-tests]]
            [re-frame.core :as re-frame]
            [vtakt-client.db :as db]
            [day8.re-frame.test :as rf-test]
            [vtakt-client.keyboard.events :as events]
            [vtakt-client.keyboard.core :as kb]))

;; =========================================================
;; Tests for Keyboard Root Events
;; =========================================================
(deftest test-inc-keyboard-root
  (testing "Incrementing keyboard root"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/inc-keyboard-root])

      ;; Verify: Root note should be transposed up by 1 semitone (C4 to C#4)
      (let [updated-root (:keyboard-root @re-frame.db/app-db)]
        (is (= :csdf (:name updated-root))
            "Root note name should be C#/Db after incrementing")
        (is (= 4 (:octave updated-root))
            "Root note octave should remain 4 after incrementing"))

      ;; Inc again.
      (re-frame/dispatch [::events/inc-keyboard-root])

      ;; Verify: Root note should be transposed up by 1 semitone again (C#4 to D4)
      (let [updated-root (:keyboard-root @re-frame.db/app-db)]
        (is (= :d (:name updated-root))
            "Root note name should be C#/Db after incrementing")
        (is (= 4 (:octave updated-root))
            "Root note octave should remain 4 after incrementing")))))

(deftest test-dec-keyboard-root
  (testing "Decrementing keyboard root"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/dec-keyboard-root])

      ;; Verify: Root note should be transposed down by 1 semitone (C4 to B3)
      (let [updated-root (:keyboard-root @re-frame.db/app-db)]
        (is (= :b (:name updated-root))
            "Root note name should be B after decrementing")
        (is (= 3 (:octave updated-root))
            "Root note octave should be 3 after decrementing"))

      ;; Dec again.
      (re-frame/dispatch [::events/dec-keyboard-root])

      ;; Verify: Root note should be transposed down again by 1 semitone (B3 to A#3)
      (let [updated-root (:keyboard-root @re-frame.db/app-db)]
        (is (= :asbf (:name updated-root))
            "Root note name should be B after decrementing")
        (is (= 3 (:octave updated-root))
            "Root note octave should be 3 after decrementing")))))

(deftest test-inc-and-dec-keyboard-root
  (testing "Incrementing and decrementing keyboard root"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (let [operations (concat
                        (repeat 5 [::events/inc-keyboard-root])
                        (repeat 6 [::events/dec-keyboard-root]))]
        (doseq [op operations]
          (re-frame/dispatch op)))

      ;; Verify: Root note should be transposed up by 1 semitone again (C4 to B3)
      (let [updated-root (:keyboard-root @re-frame.db/app-db)]
        (is (= :b (:name updated-root))
            "Root note name should be C#/Db after incrementing")
        (is (= 3 (:octave updated-root))
            "Root note octave should remain 4 after incrementing")))))

;; =========================================================
;; Tests for Keyboard Transpose Events
;; =========================================================
(deftest test-inc-keyboard-transpose
  (testing "Incrementing keyboard transpose value"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/inc-keyboard-transpose])

      ;; Verify: Transpose value should be incremented by 1
      (is (= 1 (:keyboard-transpose @re-frame.db/app-db))
          "Transpose value should be 1 after incrementing")

      ;; Inc again
      (re-frame/dispatch [::events/inc-keyboard-transpose])

      ;; Verify: Transpose value should be incremented by 1 again
      (is (= 2 (:keyboard-transpose @re-frame.db/app-db))
          "Transpose value should be 2 after incrementing twice"))))

(deftest test-dec-keyboard-transpose
  (testing "Decrementing keyboard transpose value"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with a non-zero transpose value
      (reset! re-frame.db/app-db (assoc db/default-db :keyboard-transpose 5))

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/dec-keyboard-transpose])

      ;; Verify: Transpose value should be decremented by 1
      (is (= 4 (:keyboard-transpose @re-frame.db/app-db))
          "Transpose value should be 4 after decrementing from 5")

      ;; Dec again
      (re-frame/dispatch [::events/dec-keyboard-transpose])

      ;; Verify: Transpose value should be decremented by 1 again
      (is (= 3 (:keyboard-transpose @re-frame.db/app-db))
          "Transpose value should be 3 after decrementing twice"))))

(deftest test-inc-and-dec-keyboard-transpose
  (testing "Incrementing and decrementing keyboard transpose value"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event sequence
      (let [operations (concat
                        (repeat 5 [::events/inc-keyboard-transpose])
                        (repeat 6 [::events/dec-keyboard-transpose]))]
        (doseq [op operations]
          (re-frame/dispatch op)))

      ;; Verify: Transpose value should be -1 after operations
      (is (= -1 (:keyboard-transpose @re-frame.db/app-db))
          "Transpose value should be -1 after incrementing 5 times and decrementing 6 times"))))

;; =========================================================
;; Tests for Setting Scale, Chord, and Keyboard Mode
;; =========================================================

(deftest test-set-scale
  (testing "Setting scale to minor"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-scale :ionian])

      ;; Verify: Selected scale should be updated
      (is (= :ionian (:selected-scale @re-frame.db/app-db))
          "Selected scale should be minor after setting")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-scale :chromatic])

      ;; Verify: Selected scale should be back on chromatic.
      (is (= :chromatic (:selected-scale @re-frame.db/app-db))
          "Selected scale should be chromatic after setting"))))

(deftest test-set-chord
  (testing "Setting chord to minor"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-chord :minor])

      ;; Verify: Selected chord should be updated
      (is (= :minor (:selected-chord @re-frame.db/app-db))
          "Selected chord should be minor after setting")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-chord :off])

      ;; Verify: Selected chord should be updated
      (is (= :off (:selected-chord @re-frame.db/app-db))
          "Selected chord should be off after setting"))))

(deftest test-set-keyboard-mode
  (testing "Setting keyboard mode to folding"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-keyboard-mode :folding])

      ;; Verify: Keyboard mode should be updated
      (is (= :folding (:keyboard-mode @re-frame.db/app-db))
          "Keyboard mode should be folding after setting")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-keyboard-mode :chromatic])

      ;; Verify: Keyboard mode should be updated
      (is (= :chromatic (:keyboard-mode @re-frame.db/app-db))
          "Keyboard mode should be folding after setting"))))

;; =========================================================
;; Tests for Pressed Notes
;; =========================================================

(deftest test-set-pressed-notes
  (testing "Setting pressed notes"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Sample note values to set
      (let [notes [(kb/create-note :c 4) (kb/create-note :e 4) (kb/create-note :g 4)]]

        ;; Execute: dispatch the event
        (re-frame/dispatch [::events/set-pressed-notes notes])

        ;; Verify: Pressed notes should be updated
        (is (= notes (:pressed-notes @re-frame.db/app-db))
            "Pressed notes should match the provided notes after setting")))))

(deftest test-clear-pressed-notes
  (testing "Clearing pressed notes"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with some existing pressed notes
      (reset! re-frame.db/app-db
              (assoc db/default-db :pressed-notes [(kb/create-note :c 4) (kb/create-note :e 4)]))

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/clear-pressed-notes])

      ;; Verify: Pressed notes should be empty
      (is (empty? (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should be empty after clearing"))))

;; =========================================================
;; Tests for Trigger Note
;; =========================================================

(deftest test-trigger-note-single-note
  (testing "Triggering a single note when chord mode is off"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chord mode set to off
      (reset! re-frame.db/app-db (assoc db/default-db :selected-chord :off))

      ;; Note to trigger
      (let [test-note (kb/create-note :d 4)]

        ;; Execute: dispatch the event
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Only the single note should be pressed
        (is (= [test-note] (:pressed-notes @re-frame.db/app-db))
            "Only the triggered note should be in pressed notes")))))

(deftest test-trigger-note-with-chord
  (testing "Triggering a note with chord mode active"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chord mode set to major
      (let [initial-db (-> db/default-db
                           (assoc :selected-chord :major)
                           (assoc-in [:chords :major :d] [:d :fsgf :a]))]
        (reset! re-frame.db/app-db initial-db)

        ;; Note to trigger
        (let [test-note (kb/create-note :d 4)
              expected-chord [(kb/create-note :d 4)
                              (kb/create-note :fsgf 4)
                              (kb/create-note :a 4)]]

          ;; Execute: dispatch the event
          (re-frame/dispatch [::events/trigger-note test-note])

          ;; Verify: The full chord should be in pressed notes
          (is (= expected-chord (:pressed-notes @re-frame.db/app-db))
              "Full chord should be in pressed notes when chord mode is active"))))))

;; =========================================================
;; Test Combined Events (Multiple Dispatch Sequence)
;; =========================================================

(deftest test-keyboard-interaction-sequence
  (testing "Sequence of keyboard interactions"
    (rf-test/run-test-sync
      ;; Setup: Start with clean test DB
      (reset! re-frame.db/app-db db/default-db)

      ;; Step 1: Change scale to minor
      (re-frame/dispatch [::events/set-scale :minor])
      (is (= :minor (:selected-scale @re-frame.db/app-db))
          "Scale should be set to minor")

      ;; Step 2: Change root note up 2 semitones
      (re-frame/dispatch [::events/inc-keyboard-root])
      (re-frame/dispatch [::events/inc-keyboard-root])
      (let [root (:keyboard-root @re-frame.db/app-db)]
        (is (= :d (:name root))
            "Root note should be D after incrementing twice")
        (is (= 4 (:octave root))
            "Root note octave should still be 4"))

      ;; Step 3: Trigger a note
      (let [test-note (kb/create-note :e 4)]
        (re-frame/dispatch [::events/trigger-note test-note])
        (is (= [test-note] (:pressed-notes @re-frame.db/app-db))
            "Note should be in pressed notes after triggering"))

      ;; Step 4: Clear pressed notes
      (re-frame/dispatch [::events/clear-pressed-notes])
      (is (empty? (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should be empty after clearing"))))

;; Run tests
(run-tests)
