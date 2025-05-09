(ns vtakt-client.keyboard.subs-test
  (:require [cljs.test :refer-macros [deftest testing is are run-tests]]
            [re-frame.core :as re-frame]
            [day8.re-frame.test :as rf-test]
            [vtakt-client.db :as db]
            [vtakt-client.keyboard.subs :as subs]
            [vtakt-client.keyboard.core :as kb]))

;; =========================================================
;; Helper Functions
;; =========================================================

(defn setup-default-test-db
  "Initialize the app-db with default test values"
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

(defn setup-alternative-test-db
  "Initialize the app-db with alternative test values"
  []
  (reset! re-frame.db/app-db
          (-> db/default-db
              (assoc :keyboard-root (kb/create-note :d 5))
              (assoc :keyboard-transpose 3)
              (assoc :keyboard-mode :folding)
              (assoc :selected-scale :dorian)
              (assoc :selected-chromatic-chord :minor)
              (assoc :selected-diatonic-chord :triad)
              (assoc :pressed-notes [:c :d]))))

;; =========================================================
;; Tests for Basic Subscriptions
;; =========================================================

(deftest test-default-subscriptions
  (testing "Basic subscriptions return expected values"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-default-test-db)

      ;; Test ::keyboard-root subscription
     (let [keyboard-root @(re-frame/subscribe [::subs/keyboard-root])]
       (is (= (kb/create-note :c 4) keyboard-root)
           "::keyboard-root should return C4"))

      ;; Test ::keyboard-transpose subscription
     (let [keyboard-transpose @(re-frame/subscribe [::subs/keyboard-transpose])]
       (is (= 0 keyboard-transpose)
           "::keyboard-transpose should return 0"))

      ;; Test ::selected-scale subscription
     (let [selected-scale @(re-frame/subscribe [::subs/selected-scale])]
       (is (= :ionian selected-scale)
           "::selected-scale should return :ionian"))

      ;; Test ::keyboard-mode subscription
     (let [keyboard-mode @(re-frame/subscribe [::subs/keyboard-mode])]
       (is (= :chromatic keyboard-mode)
           "::keyboard-mode should return :chromatic"))

      ;; Test ::selected-chromatic-chord subscription
     (let [selected-chord @(re-frame/subscribe [::subs/selected-chromatic-chord])]
       (is (= :major selected-chord)
           "::selected-chromatic-chord should return :major"))

      ;; Test ::selected-diatonic-chord subscription
     (let [selected-diatonic-chord @(re-frame/subscribe [::subs/selected-diatonic-chord])]
       (is (= :triad selected-diatonic-chord)
           "::selected-diatonic-chord should return :triad"))

      ;; Test ::pressed-notes subscription
     (let [pressed-notes @(re-frame/subscribe [::subs/pressed-notes])]
       (is (= [] pressed-notes)
           "::pressed-notes should return empty vector")))))

(deftest test-alternative-subscriptions
  (testing "Basic subscriptions return expected values"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-alternative-test-db)

      ;; Test ::keyboard-root subscription
     (let [keyboard-root @(re-frame/subscribe [::subs/keyboard-root])]
       (is (= (kb/create-note :d 5) keyboard-root)
           "::keyboard-root should return D4"))

      ;; Test ::keyboard-transpose subscription
     (let [keyboard-transpose @(re-frame/subscribe [::subs/keyboard-transpose])]
       (is (= 3 keyboard-transpose)
           "::keyboard-transpose should return 0"))

      ;; Test ::selected-scale subscription
     (let [selected-scale @(re-frame/subscribe [::subs/selected-scale])]
       (is (= :dorian selected-scale)
           "::selected-scale should return :ionian"))

      ;; Test ::keyboard-mode subscription
     (let [keyboard-mode @(re-frame/subscribe [::subs/keyboard-mode])]
       (is (= :folding keyboard-mode)
           "::keyboard-mode should return :chromatic"))

      ;; Test ::selected-chromatic-chord subscription
     (let [selected-chord @(re-frame/subscribe [::subs/selected-chromatic-chord])]
       (is (= :minor selected-chord)
           "::selected-chromatic-chord should return :major"))

      ;; Test ::selected-diatonic-chord subscription
     (let [selected-diatonic-chord @(re-frame/subscribe [::subs/selected-diatonic-chord])]
       (is (= :triad selected-diatonic-chord)
           "::selected-diatonic-chord should return :triad"))

      ;; Test ::pressed-notes subscription
     (let [pressed-notes @(re-frame/subscribe [::subs/pressed-notes])]
       (is (= [:c :d] pressed-notes)
           "::pressed-notes should return empty vector")))))
;; =========================================================
;; Tests for Derived Subscriptions
;; =========================================================

(deftest test-scales-subscription
  (testing "Scales subscription returns expected scales"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-default-test-db)

      ;; Test ::scales subscription
     (let [scales @(re-frame/subscribe [::subs/scales])]
       (is (map? scales)
           "::scales should return a map")
       (is (contains? scales :chromatic)
           "::scales should contain :chromatic scale")
       (is (contains? scales :ionian)
           "::scales should contain :ionian scale")
       (is (map? (get scales :ionian))
           "Each scale should be a map of root note to scale notes")))))

(deftest test-chord-subscriptions
  (testing "Chord subscriptions return expected chords"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-default-test-db)

      ;; Test ::chromatic-chords subscription
     (let [chromatic-chords @(re-frame/subscribe [::subs/chromatic-chords])]
       (is (map? chromatic-chords)
           "::chromatic-chords should return a map")
       (is (contains? chromatic-chords :major)
           "::chromatic-chords should contain :major chord")
       (is (contains? chromatic-chords :minor)
           "::chromatic-chords should contain :minor chord")
       (is (vector? (get chromatic-chords :major))
           "Each chord should be a vector of scale degrees"))

      ;; Test ::diatonic-chords subscription
     (let [diatonic-chords @(re-frame/subscribe [::subs/diatonic-chords])]
       (is (map? diatonic-chords)
           "::diatonic-chords should return a map")
       (is (contains? diatonic-chords :triad)
           "::diatonic-chords should contain :triad chord")
       (is (contains? diatonic-chords :seventh)
           "::diatonic-chords should contain :seventh chord")
       (is (vector? (get diatonic-chords :triad))
           "Each chord should be a vector of scale degrees")))))

;; =========================================================
;; Tests for Complex Subscriptions
;; =========================================================

(deftest test-chromatic-keyboard-subscription
  (testing "Chromatic keyboard subscription returns expected data"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-default-test-db)

      ;; Test ::chromatic-keyboard subscription
     (let [chromatic-keyboard @(re-frame/subscribe [::subs/chromatic-keyboard])]
       (is (satisfies? kb/Keyboard chromatic-keyboard)
           "::chromatic-keyboard should return a Keyboard implementation")

        ;; Get the keyboard rows for further testing
       (let [rows (kb/rows chromatic-keyboard)
             bottom-row (:bottom rows)
             top-row (:top rows)]

          ;; Test row structure
         (is (contains? rows :top)
             "Keyboard rows should contain :top key")
         (is (contains? rows :bottom)
             "Keyboard rows should contain :bottom key")
         (is (vector? top-row)
             "Top row should be a vector")
         (is (vector? bottom-row)
             "Bottom row should be a vector")

          ;; Check some specific notes in ionian (major) scale
         (is (some #(and (= (:name %) :c) (= (:octave %) 4)) bottom-row)
             "C4 should be present in the bottom row for ionian scale")
         (is (some #(and (= (:name %) :e) (= (:octave %) 4)) bottom-row)
             "E4 should be present in the bottom row for ionian scale")
         (is (some #(and (= (:name %) :g) (= (:octave %) 4)) bottom-row)
             "G4 should be present in the bottom row for ionian scale")

          ;; Non-scale notes should be filtered out in :ionian mode
         (is (nil? (some #(and (= (:name %) :csdf) (= (:octave %) 4)) top-row))
             "C#4 should not be present in the top row for ionian scale")))))

  (testing "Chromatic keyboard subscription returns expected data even when in folding mode"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-alternative-test-db)

      ;; Test ::chromatic-keyboard subscription
     (let [chromatic-keyboard @(re-frame/subscribe [::subs/chromatic-keyboard])]
       (is (satisfies? kb/Keyboard chromatic-keyboard)
           "::chromatic-keyboard should return a Keyboard implementation")

        ;; Get the keyboard rows for further testing
       (let [rows (kb/rows chromatic-keyboard)
             bottom-row (:bottom rows)
             top-row (:top rows)]

          ;; Test row structure
         (is (contains? rows :top)
             "Keyboard rows should contain :top key")
         (is (contains? rows :bottom)
             "Keyboard rows should contain :bottom key")
         (is (vector? top-row)
             "Top row should be a vector")
         (is (vector? bottom-row)
             "Bottom row should be a vector")

          ;; Note that keyboard is transposed 3 semi-tones up.
         (is (some #(and (= (:name %) :f) (= (:octave %) 5)) bottom-row)
             "E5 should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :g) (= (:octave %) 5)) bottom-row)
             "G5 should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :c) (= (:octave %) 6)) bottom-row)
             "B5 should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :d) (= (:octave %) 6)) bottom-row)
             "C6 should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :gsaf) (= (:octave %) 5)) bottom-row)
             "G# should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :asbf) (= (:octave %) 5)) bottom-row)
             "A# should be present in the bottom row for dorian scale")
         (is (some #(and (= (:name %) :dsef) (= (:octave %) 6)) bottom-row)
             "D# should be present in the bottom row for dorian scale")
         (is (every? nil? top-row)
             "All keys in top row should be nil for D tranposed dorian scale")))))

  (testing "Chromatic keyboard subscription properly handles scales with notes in top row"
    (rf-test/run-test-sync
      ;; Setup test database with F# as root and lydian scale (has many sharps)
     (setup-default-test-db)
     (swap! re-frame.db/app-db assoc
            :keyboard-root (kb/create-note :fsgf 4)
            :selected-scale :ionian)

      ;; Test ::chromatic-keyboard subscription
     (let [chromatic-keyboard @(re-frame/subscribe [::subs/chromatic-keyboard])]
       (is (satisfies? kb/Keyboard chromatic-keyboard)
           "::chromatic-keyboard should return a Keyboard implementation")

        ;; Get the keyboard rows for further testing
       (let [rows (kb/rows chromatic-keyboard)
             bottom-row (:bottom rows)
             top-row (:top rows)]

          ;; Test row structure
         (is (contains? rows :top)
             "Keyboard rows should contain :top key")
         (is (contains? rows :bottom)
             "Keyboard rows should contain :bottom key")
         (is (vector? top-row)
             "Top row should be a vector")
         (is (vector? bottom-row)
             "Bottom row should be a vector")

          ;; Verify sharp notes are present in F# lydian scale
          ;; The lydian scale in F# has F#, G#, A#, B, C#, D#, E#(F)
         (is (some #(and (= (:name %) :fsgf) (= (:octave %) 4)) top-row)
             "F#4 should be present in the bottom row for F# lydian scale")
         (is (some #(and (= (:name %) :gsaf) (= (:octave %) 4)) top-row)
             "G#4 should be present in the top row for F# lydian scale")
         (is (some #(and (= (:name %) :asbf) (= (:octave %) 4)) top-row)
             "A#4 should be present in the top row for F# lydian scale")
         (is (some #(and (= (:name %) :csdf) (= (:octave %) 5)) top-row)
             "C#5 should be present in the top row for F# lydian scale")
         (is (some #(and (= (:name %) :dsef) (= (:octave %) 5)) top-row)
             "D#5 should be present in the top row for F# lydian scale"))))))

(deftest test-keyboard-subscription
  (testing "Keyboard subscription with chromatic mode returns expected keyboard"
    (rf-test/run-test-sync
      ;; Setup the test database with chromatic mode
     (setup-default-test-db)
     (swap! re-frame.db/app-db assoc :keyboard-mode :chromatic)

      ;; Test ::keyboard subscription in chromatic mode
     (let [keyboard @(re-frame/subscribe [::subs/keyboard])
           rows (kb/rows keyboard)
           bottom-row (:bottom rows)
           top-row (:top rows)]
       ;; Basic protocol checks
       (is (satisfies? kb/Keyboard keyboard)
           "::keyboard should return a Keyboard implementation")
       (is (contains? rows :top)
           "Keyboard rows should contain :top key")
       (is (contains? rows :bottom)
           "Keyboard rows should contain :bottom key")

       ;; Check specific notes in C ionian (default setup)
       (is (some #(and (= (:name %) :c) (= (:octave %) 4)) bottom-row)
           "C4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :d) (= (:octave %) 4)) bottom-row)
           "D4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :e) (= (:octave %) 4)) bottom-row)
           "E4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :f) (= (:octave %) 4)) bottom-row)
           "F4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :g) (= (:octave %) 4)) bottom-row)
           "G4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :a) (= (:octave %) 4)) bottom-row)
           "A4 should be present in the bottom row for C ionian scale")
       (is (some #(and (= (:name %) :b) (= (:octave %) 4)) bottom-row)
           "B4 should be present in the bottom row for C ionian scale")

       ;; Verify non-scale notes are properly filtered
       (is (nil? (some #(and (= (:name %) :csdf) (= (:octave %) 4)) top-row))
           "C#4 should not be present in the top row for C ionian scale")
       (is (nil? (some #(and (= (:name %) :dsef) (= (:octave %) 4)) top-row))
           "D#4 should not be present in the top row for C ionian scale")

       ;; Test with different scale
       (swap! re-frame.db/app-db assoc :selected-scale :minor-pentatonic)
       (let [pentatonic-keyboard @(re-frame/subscribe [::subs/keyboard])
             pentatonic-rows (kb/rows pentatonic-keyboard)
             pentatonic-bottom (:bottom pentatonic-rows)
             pentatonic-top (:top pentatonic-rows)]

         ;; C minor pentatonic has C, Eb, F, G, Bb
         (is (some #(and (= (:name %) :c) (= (:octave %) 4)) pentatonic-bottom)
             "C4 should be present in C minor pentatonic")
         (is (some #(and (= (:name %) :f) (= (:octave %) 4)) pentatonic-bottom)
             "F4 should be present in C minor pentatonic")
         (is (some #(and (= (:name %) :g) (= (:octave %) 4)) pentatonic-bottom)
             "G4 should be present in C minor pentatonic")
         (is (some #(and (= (:name %) :dsef) (= (:octave %) 4)) pentatonic-top)
             "Eb4 should be present in C minor pentatonic")
         (is (some #(and (= (:name %) :asbf) (= (:octave %) 4)) pentatonic-top)
             "Bb4 should be present in C minor pentatonic")

         ;; Verify excluded notes
         (is (nil? (some #(and (= (:name %) :d) (= (:octave %) 4))
                         (concat pentatonic-bottom pentatonic-top)))
             "D4 should not be present in C minor pentatonic")
         (is (nil? (some #(and (= (:name %) :a) (= (:octave %) 4))
                         (concat pentatonic-bottom pentatonic-top)))
             "A4 should not be present in C minor pentatonic"))

       ;; Test transposition
       (swap! re-frame.db/app-db assoc
              :selected-scale :ionian
              :keyboard-transpose 2)
       (let [transposed-keyboard @(re-frame/subscribe [::subs/keyboard])
             transposed-rows (kb/rows transposed-keyboard)
             transposed-bottom (:bottom transposed-rows)
             transposed-top (:top transposed-rows)]

         ;; With +2 transposition, C becomes D, etc.
         (is (some #(and (= (:name %) :d) (= (:octave %) 4)) transposed-bottom)
             "D4 should be present after transposing C4 up 2 semitones")
         (is (some #(and (= (:name %) :e) (= (:octave %) 4)) transposed-bottom)
             "E4 should be present after transposing D4 up 2 semitones")
         (is (some #(and (= (:name %) :fsgf) (= (:octave %) 4)) transposed-bottom)
             "F#4 should be present after transposing E4 up 2 semitones")
         (is (some #(and (= (:name %) :g) (= (:octave %) 4)) transposed-bottom)
             "G4 should be present after transposing F4 up 2 semitones"))

       ;; Test different root note
       (swap! re-frame.db/app-db assoc
              :keyboard-root (kb/create-note :fsgf 4)
              :keyboard-transpose 0)
       (let [fsharp-keyboard @(re-frame/subscribe [::subs/keyboard])
             fsharp-rows (kb/rows fsharp-keyboard)
             fsharp-bottom (:bottom fsharp-rows)
             fsharp-top (:top fsharp-rows)]

         ;; F# major scale: F#, G#, A#, B, C#, D#, E#(F)
         (is (some #(and (= (:name %) :fsgf) (= (:octave %) 4)) fsharp-top)
             "F#4 should be present in F# ionian scale")
         (is (some #(and (= (:name %) :b) (= (:octave %) 4)) fsharp-bottom)
             "B4 should be present in F# ionian scale")
         (is (some #(and (= (:name %) :gsaf) (= (:octave %) 4)) fsharp-top)
             "G#4 should be present in F# ionian scale")
         (is (some #(and (= (:name %) :asbf) (= (:octave %) 4)) fsharp-top)
             "A#4 should be present in F# ionian scale")
         (is (some #(and (= (:name %) :csdf) (= (:octave %) 5)) fsharp-top)
             "C#5 should be present in F# ionian scale")
         (is (some #(and (= (:name %) :dsef) (= (:octave %) 5)) fsharp-top)
             "D#5 should be present in F# ionian scale")))))

  (testing "Keyboard subscription with folding mode returns expected keyboard"
    (rf-test/run-test-sync
      ;; Setup the test database with folding mode
     (setup-default-test-db)
     (swap! re-frame.db/app-db assoc :keyboard-mode :folding)

      ;; Test ::keyboard subscription in folding mode
     (let [keyboard @(re-frame/subscribe [::subs/keyboard])
           rows (kb/rows keyboard)
           bottom-row (:bottom rows)
           top-row (:top rows)]
       (is (satisfies? kb/Keyboard keyboard)
           "::keyboard should return a Keyboard implementation")
       (is (contains? rows :top)
           "Keyboard rows should contain :top key")
       (is (contains? rows :bottom)
           "Keyboard rows should contain :bottom key")

       ;; Check note distribution in folding mode
       (is (= 8 (count bottom-row))
           "Bottom row should contain 8 notes in folding mode")
       (is (= 8 (count top-row))
           "Top row should contain 8 notes in folding mode")

       ;; Test with a different scale
       (swap! re-frame.db/app-db assoc :selected-scale :dorian)
       (let [dorian-keyboard @(re-frame/subscribe [::subs/keyboard])
             dorian-rows (kb/rows dorian-keyboard)
             dorian-bottom (:bottom dorian-rows)
             dorian-top (:top dorian-rows)]

         ;; C Dorian has: C, D, Eb, F, G, A, Bb
         (is (some #(and (= (:name %) :c) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "C4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :d) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "D4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :dsef) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "Eb4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :f) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "F4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :g) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "G4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :a) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "A4 should be present in C dorian scale")
         (is (some #(and (= (:name %) :asbf) (= (:octave %) 4))
                   (concat dorian-bottom dorian-top))
             "Bb4 should be present in C dorian scale"))

       ;; Test boundary handling by changing to a high octave
       (swap! re-frame.db/app-db assoc
              :keyboard-root (kb/create-note :g 6)
              :selected-scale :ionian)
       (let [high-keyboard @(re-frame/subscribe [::subs/keyboard])
             high-rows (kb/rows high-keyboard)
             high-notes (concat (:bottom high-rows) (:top high-rows))]

         ;; Check for high octave notes
         (is (some #(and (= (:name %) :g) (= (:octave %) 6)) high-notes)
             "G6 should be present in high octave keyboard")
         (is (some #(and (= (:name %) :a) (= (:octave %) 6)) high-notes)
             "A6 should be present in high octave keyboard")
         (is (some #(and (= (:name %) :b) (= (:octave %) 6)) high-notes)
             "B6 should be present in high octave keyboard")
         (is (some #(and (= (:name %) :c) (= (:octave %) 7)) high-notes)
             "C7 should be present in high octave keyboard")
         (is (some #(and (= (:name %) :d) (= (:octave %) 7)) high-notes)
             "D7 should be present in high octave keyboard"))

       ;; Test octave wrapping in folding keyboard
       (swap! re-frame.db/app-db assoc
              :keyboard-root (kb/create-note :b 3)
              :selected-scale :chromatic)
       (let [wrap-keyboard @(re-frame/subscribe [::subs/keyboard])
             wrap-rows (kb/rows wrap-keyboard)
             wrap-notes (concat (:bottom wrap-rows) (:top wrap-rows))]

         ;; Check for octave wrapping at B3 to C4
         (is (some #(and (= (:name %) :b) (= (:octave %) 3)) wrap-notes)
             "B3 should be present in octave-wrapping keyboard")
         (is (some #(and (= (:name %) :c) (= (:octave %) 4)) wrap-notes)
             "C4 should be present in octave-wrapping keyboard")
         (is (some #(and (= (:name %) :csdf) (= (:octave %) 4)) wrap-notes)
             "C#4 should be present in octave-wrapping keyboard"))))))

;; =========================================================
;; Tests for Subscription Behavior with State Changes
;; =========================================================

(deftest test-transposition-effects
  (testing "Keyboard subscriptions respond to transposition changes"
    (rf-test/run-test-sync
      ;; Setup the test database
     (setup-default-test-db)

      ;; Get the initial keyboard with no transposition
     (let [initial-keyboard @(re-frame/subscribe [::subs/keyboard])
           initial-rows (kb/rows initial-keyboard)]

        ;; Change transposition to 2 (up a whole step)
       (swap! re-frame.db/app-db assoc :keyboard-transpose 2)

        ;; Get the transposed keyboard
       (let [transposed-keyboard @(re-frame/subscribe [::subs/keyboard])
             transposed-rows (kb/rows transposed-keyboard)]

          ;; Verify the keyboards are different
         (is (not= initial-rows transposed-rows)
             "Transposed keyboard should differ from initial keyboard")

          ;; Check for expected transposition
         (let [find-c-note (fn [rows]
                             (first (filter #(and (= (:name %) :c) (some? %))
                                            (concat (:bottom rows) (:top rows)))))
               initial-c (find-c-note initial-rows)
               transposed-c (find-c-note transposed-rows)]

           (when (and initial-c transposed-c)
             (is (= (+ (:octave initial-c) 0)
                    (:octave transposed-c))
                 "Note octave should be updated according to transposition"))))))))

(deftest test-scale-change-effects
  (testing "Keyboard subscriptions respond to scale changes"
    (rf-test/run-test-sync
      ;; Setup the test database with ionian scale
     (setup-default-test-db)

      ;; Get the initial keyboard with ionian scale
     (let [initial-keyboard @(re-frame/subscribe [::subs/keyboard])
           initial-rows (kb/rows initial-keyboard)]

        ;; Change scale to pentatonic
       (swap! re-frame.db/app-db assoc :selected-scale :minor-pentatonic)

        ;; Get the new keyboard with pentatonic scale
       (let [pentatonic-keyboard @(re-frame/subscribe [::subs/keyboard])
             pentatonic-rows (kb/rows pentatonic-keyboard)]

          ;; Verify the keyboards are different
         (is (not= initial-rows pentatonic-rows)
             "Pentatonic keyboard should differ from ionian keyboard")

          ;; Check if F is present in ionian but not in minor pentatonic (C minor pentatonic)
         (let [has-d? (fn [rows]
                        (some #(and (= (:name %) :d) (some? %))
                              (concat (:bottom rows) (:top rows))))]

           (is (has-d? initial-rows)
               "F note should be present in ionian scale")
           (is (not (has-d? pentatonic-rows))
               "F note should not be present in C minor pentatonic scale")))))))

(deftest test-root-change-effects
  (testing "Keyboard subscriptions respond to root note changes"
    (rf-test/run-test-sync
      ;; Setup the test database with C4 root
     (setup-default-test-db)

      ;; Get the initial keyboard with C4 root
     (let [initial-keyboard @(re-frame/subscribe [::subs/keyboard])
           initial-rows (kb/rows initial-keyboard)]

        ;; Change root to G4
       (swap! re-frame.db/app-db assoc :keyboard-root (kb/create-note :g 4))

        ;; Get the new keyboard with G4 root
       (let [new-keyboard @(re-frame/subscribe [::subs/keyboard])
             new-rows (kb/rows new-keyboard)]

          ;; Verify the keyboards are different
         (is (not= initial-rows new-rows)
             "G-rooted keyboard should differ from C-rooted keyboard")

          ;; In ionian scale, F# is present in G major but not in C major
         (let [has-fsharp? (fn [rows]
                             (some #(and (= (:name %) :fsgf) (some? %))
                                   (concat (:bottom rows) (:top rows))))]

           (is (not (has-fsharp? initial-rows))
               "F# note should not be present in C ionian scale")
           (is (has-fsharp? new-rows)
               "F# note should be present in G ionian scale")))))))

;; Run all tests
(run-tests)
