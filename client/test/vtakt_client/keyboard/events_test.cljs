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

(deftest test-set-chromatic-chord
  (testing "Setting chord to minor"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Verify: Selected chord should be updated
      (is (= :single-note (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should initially be single note")
      (is (= :single-note (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected diatonic chord should initially be single note")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-selected-chromatic-chord :minor])

      ;; Verify: Selected chord should be updated
      (is (= :minor (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should be minor after setting")
      (is (= :triad (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected diatonic chord should be triad after setting")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-selected-chromatic-chord :single-note])

      ;; Verify: Selected chord should be updated
      (is (= :single-note (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should be single note after setting")
      (is (= :single-note (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected diatonic chord should be single note after setting"))))

(deftest test-set-diatonic-chord
  (testing "Setting chord to minor"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      ;; Verify: Selected chord should be updated
      (is (= :single-note (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected diatonic chord should initially be single note")
      (is (= :single-note (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should initially be single note")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-selected-diatonic-chord :triad])

      ;; Verify: Selected chord should be updated
      (is (= :triad (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected chord should be minor after setting")
      (is (= :major (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should be major after setting")

      ;; Execute: dispatch the event
      (re-frame/dispatch [::events/set-selected-diatonic-chord :single-note])

      ;; Verify: Selected chord should be updated
      (is (= :single-note (:selected-chromatic-chord @re-frame.db/app-db))
          "Selected chromatic chord should be single-note after setting")
      (is (= :single-note (:selected-diatonic-chord @re-frame.db/app-db))
          "Selected diatonic chord should be single-note after setting"))))

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
;; Tests for Trigger Note
;; =========================================================
(deftest test-set-and-clear-pressed-notes
  (testing "Setting pressed notes"
    (rf-test/run-test-sync
      ;; Setup: use the test DB value
      (reset! re-frame.db/app-db db/default-db)

      (re-frame/dispatch [::events/trigger-note (kb/create-note :c 4)])
      (re-frame/dispatch [::events/trigger-note (kb/create-note :e 4)])
      (re-frame/dispatch [::events/trigger-note (kb/create-note :g 4)])

      (is (=
           [{:name :c, :octave 4} {:name :e, :octave 4} {:name :g, :octave 4}]
           (:pressed-notes @re-frame.db/app-db))
        "Pressed notes should match the provided notes after setting")

      (re-frame/dispatch [::events/trigger-note nil])
      (is (=
           []
           (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should be empty after clearing")

      (re-frame/dispatch [::events/trigger-note (kb/create-note :d 4)])
      (re-frame/dispatch [::events/trigger-note (kb/create-note :f 4)])
      (re-frame/dispatch [::events/trigger-note (kb/create-note :a 4)])

      ;; Another round
      (is (=
           [{:name :d, :octave 4} {:name :f, :octave 4} {:name :a, :octave 4}]
           (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should match the provided notes after setting"))))

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


;; =========================================================
;; Tests for trigger-note Event Handler
;; =========================================================

(deftest test-trigger-note-nil
  (testing "Triggering with nil should clear pressed notes"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with some pressed notes
      (reset! re-frame.db/app-db (assoc db/default-db :pressed-notes
                                        [(kb/create-note :c 4)
                                         (kb/create-note :e 4)
                                         (kb/create-note :g 4)]))

      ;; Execute: dispatch the event with nil
      (re-frame/dispatch [::events/trigger-note nil])

      ;; Verify: Pressed notes should be cleared
      (is (empty? (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should be empty after triggering with nil"))))

(deftest test-trigger-note-chromatic-single-note
  (testing "Triggering a single note in chromatic scale"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale and single-note chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :single-note)
                  (assoc :pressed-notes [])))

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :d 4)]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Only the single note should be pressed
        (is (= [test-note] (:pressed-notes @re-frame.db/app-db))
            "Only the triggered note should be in pressed notes")))))

(deftest test-trigger-note-chromatic-major-chord
  (testing "Triggering a major chord in chromatic scale"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale and major chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :major)
                  (assoc :pressed-notes [])))

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :c 4)
            expected-chord [(kb/create-note :c 4)
                            (kb/create-note :e 4)
                            (kb/create-note :g 4)]]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Major chord should be pressed
        (is (= expected-chord (:pressed-notes @re-frame.db/app-db))
            "C major chord should be in pressed notes")))))

(deftest test-trigger-note-chromatic-minor-chord
  (testing "Triggering a minor chord in chromatic scale"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale and minor chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :minor)
                  (assoc :pressed-notes [])))

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :a 3)
            expected-chord [(kb/create-note :a 3)
                            (kb/create-note :c 4)
                            (kb/create-note :e 4)]]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Minor chord should be pressed
        (is (= expected-chord (:pressed-notes @re-frame.db/app-db))
            "A minor chord should be in pressed notes")))))

(deftest test-trigger-note-with-transposition
  (testing "Triggering notes with transposition applied"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale, single-note chord and transposition of 2
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :single-note)
                  (assoc :pressed-notes [])))

      (re-frame/dispatch [::events/inc-keyboard-transpose])
      (re-frame/dispatch [::events/inc-keyboard-transpose])

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :c 4)
            ;; trigger notes doesn't do transposition. Its expected that
            ;; the notes you want to trigger are the notes that are passed in.
            expected-note (kb/create-note :c 4)]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Transposed note should be pressed
        (is (= [expected-note] (:pressed-notes @re-frame.db/app-db))
            "Note should be transposed up by 2 semitones")))))

(deftest test-trigger-note-diatonic-scale
  (testing "Triggering notes in a diatonic scale"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with ionian scale and triad chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :ionian)
                  (assoc :selected-diatonic-chord :triad)
                  (assoc :pressed-notes [])))

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :c 4)]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Diatonic triad should be pressed
        (let [pressed-notes (:pressed-notes @re-frame.db/app-db)]
          (is (= 3 (count pressed-notes))
              "Should have 3 notes in a triad")

          ;; Check if it contains the right notes (C, E, G for C major triad)
          (is (some #(and (= (:name %) :c) (= (:octave %) 4)) pressed-notes)
              "Triad should contain root note C4")
          (is (some #(and (= (:name %) :e) (= (:octave %) 4)) pressed-notes)
              "Triad should contain third note E4")
          (is (some #(and (= (:name %) :g) (= (:octave %) 4)) pressed-notes)
              "Triad should contain fifth note G4"))))))

(deftest test-trigger-note-multiple-times
  (testing "Triggering different notes in sequence"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale and single-note chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :single-note)
                  (assoc :pressed-notes [])))

      ;; First note
      (let [note1 (kb/create-note :c 4)]
        (re-frame/dispatch [::events/trigger-note note1])
        (is (= [note1] (:pressed-notes @re-frame.db/app-db))
            "First note should be in pressed notes"))

      ;; Clear pressed notes
      (re-frame/dispatch [::events/trigger-note nil])
      (is (empty? (:pressed-notes @re-frame.db/app-db))
          "Pressed notes should be cleared")

      ;; Second note
      (let [note2 (kb/create-note :e 4)]
        (re-frame/dispatch [::events/trigger-note note2])
        (is (= [note2] (:pressed-notes @re-frame.db/app-db))
            "Second note should be in pressed notes")))))

(deftest test-trigger-note-seventh-chord
  (testing "Triggering a seventh chord"
    (rf-test/run-test-sync
      ;; Setup: use the test DB with chromatic scale and dominant-7 chord
      (reset! re-frame.db/app-db
              (-> db/default-db
                  (assoc :selected-scale :chromatic)
                  (assoc :selected-chromatic-chord :dominant-7)
                  (assoc :pressed-notes [])))

      ;; Execute: dispatch the event with a note
      (let [test-note (kb/create-note :g 3)
            expected-chord [(kb/create-note :g 3)
                            (kb/create-note :b 3)
                            (kb/create-note :d 4)
                            (kb/create-note :f 4)]]
        (re-frame/dispatch [::events/trigger-note test-note])

        ;; Verify: Dominant 7th chord should be pressed
        (is (= expected-chord (:pressed-notes @re-frame.db/app-db))
            "G dominant 7th chord should be in pressed notes")))))

;; Run tests
(run-tests)
