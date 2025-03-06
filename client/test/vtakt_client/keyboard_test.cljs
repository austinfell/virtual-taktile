(ns vtakt-client.keyboard-test
  (:require [cljs.test :refer-macros [deftest testing is are run-tests]]
            [vtakt-client.components.keyboard :as kb]
            [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as tcp]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))

;; =========================================================
;; Test Data
;; =========================================================

(def all-notes [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b])

(def common-scales
  {:major          [0 2 4 5 7 9 11]
   :minor          [0 2 3 5 7 8 10]
   :chromatic      [0 1 2 3 4 5 6 7 8 9 10 11]})

;; Expected results for common scale types
(def expected-major-scale
  {:c [:c :d :e :f :g :a :b],
   :csdf [:csdf :dsef :f :fsgf :gsaf :asbf :c],
   :d [:d :e :fsgf :g :a :b :csdf],
   :dsef [:dsef :f :g :gsaf :asbf :c :d],
   :e [:e :fsgf :gsaf :a :b :csdf :dsef],
   :f [:f :g :a :asbf :c :d :e],
   :fsgf [:fsgf :gsaf :asbf :b :csdf :dsef :f],
   :g [:g :a :b :c :d :e :fsgf],
   :gsaf [:gsaf :asbf :c :csdf :dsef :f :g],
   :a [:a :b :csdf :d :e :fsgf :gsaf],
   :asbf [:asbf :c :d :dsef :f :g :a],
   :b [:b :csdf :dsef :e :fsgf :gsaf :asbf]})

(def expected-minor-scale
  {:c [:c :d :dsef :f :g :gsaf :asbf],
   :csdf [:csdf :dsef :e :fsgf :gsaf :a :b],
   :d [:d :e :f :g :a :asbf :c],
   :dsef [:dsef :f :fsgf :gsaf :asbf :b :csdf],
   :e [:e :fsgf :g :a :b :c :d],
   :f [:f :g :gsaf :asbf :c :csdf :dsef],
   :fsgf [:fsgf :gsaf :a :b :csdf :d :e],
   :g [:g :a :asbf :c :d :dsef :f],
   :gsaf [:gsaf :asbf :b :csdf :dsef :e :fsgf],
   :a [:a :b :c :d :e :f :g],
   :asbf [:asbf :c :csdf :dsef :f :fsgf :gsaf],
   :b [:b :csdf :d :e :fsgf :g :a]})

(def expected-chromatic-scale
  {:c [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b],
   :csdf [:csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b :c],
   :d [:d :dsef :e :f :fsgf :g :gsaf :a :asbf :b :c :csdf],
   :dsef [:dsef :e :f :fsgf :g :gsaf :a :asbf :b :c :csdf :d],
   :e [:e :f :fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef],
   :f [:f :fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef :e],
   :fsgf [:fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef :e :f],
   :g [:g :gsaf :a :asbf :b :c :csdf :d :dsef :e :f :fsgf],
   :gsaf [:gsaf :a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g],
   :a [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf],
   :asbf [:asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf :a],
   :b [:b :c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf]})

(def intervals->empty-scale-map (into {} (map (fn [root] [root []]) all-notes)))
(def intervals->single-note-scale-map (into {} (map (fn [root] [root [root]]) all-notes)))

;; =========================================================
;; Specs
;; =========================================================

(s/def ::intervals (s/coll-of nat-int? :kind vector?))
(s/def ::musical-intervals
  (s/with-gen ::intervals
    (fn []
      (gen/frequency
       [[10 (gen/elements (vals common-scales))]
        [3  (gen/vector (gen/choose 0 11) 3 8)]
        [1  (gen/vector (gen/choose 0 24) 3 8)]]))))

;; =========================================================
;; Tests for create-scale-group
;; =========================================================

(deftest test-edge-cases
  (testing "Empty and nil inputs"
    (is (= intervals->empty-scale-map
           (kb/create-scale-group []))
        "Empty interval list should create map with empty vectors")

    (is (= intervals->empty-scale-map
           (kb/create-scale-group nil))
        "Nil interval list should create map with empty vectors"))

  (testing "Single interval cases"
    (is (= intervals->single-note-scale-map
           (kb/create-scale-group [0]))
        "Single interval [0] should create map with single-element vectors")

    (is (= (into {} (map (fn [root] [root [root root]]) all-notes))
           (kb/create-scale-group [0 0]))
        "Repeated intervals should create scales with repeated notes")))

(deftest test-interval-normalization
  (testing "Octave equivalent intervals are normalized to one octave"
    (is (= (kb/create-scale-group [0 1])
           (kb/create-scale-group [0 13]))
        "Intervals should be normalized to one octave")

    (is (= expected-major-scale
           (kb/create-scale-group (:major common-scales)))
        "Major scale should match expected structure")

    (is (= expected-major-scale
           (kb/create-scale-group [0 14 4 17 7 21 11]))
        "Major scale should match with intervals >12")

    (is (= expected-major-scale
           (kb/create-scale-group [12 14 16 17 19 21 23]))
        "Major scale should match with all intervals >12")))

(deftest test-standard-scales
  (testing "Major scale"
    (is (= expected-major-scale
           (kb/create-scale-group (:major common-scales)))
        "Major scale should match expected structure"))

  (testing "Minor scale"
    (is (= expected-minor-scale
           (kb/create-scale-group (:minor common-scales)))
        "Minor scale should match expected structure"))

  (testing "Chromatic scale"
    (is (= expected-chromatic-scale
           (kb/create-scale-group (:chromatic common-scales)))
        "Chromatic scale should match expected structure")))

(deftest test-error-handling
  (testing "Invalid inputs should throw errors"
    (is (thrown? js/Error (kb/create-scale-group [-1]))
        "Negative numbers should throw error")

    (is (thrown? js/Error (kb/create-scale-group [1 -1 2]))
        "Negative numbers in otherwise valid list should throw error")

    (is (thrown? js/Error (kb/create-scale-group [1.5]))
        "Decimal numbers should throw error")

    (is (thrown? js/Error (kb/create-scale-group ["a" "b" "c"]))
        "Non-numeric values should throw error")))

(deftest test-scale-group-advanced-properties
  (testing "Octave Equivalence Property"
    (let [samples (gen/sample (s/gen ::musical-intervals) 20)]
      (doseq [sample samples]
        (let [result (kb/create-scale-group sample)]
          (is (= result (kb/create-scale-group (map #(+ % 12) sample)))
              "Octave equivalent scales should be identical")))))

  (testing "Coprime Transposition Property"
    (let [samples (gen/sample (s/gen ::musical-intervals) 20)]
      (doseq [sample samples]
        (when (and (seq sample) (< (count sample) 12))
          (let [result (kb/create-scale-group sample)]
            (doseq [coprime [1 5 7 11]]
              (is (not= (update-vals result set)
                        (update-vals (kb/create-scale-group (map #(+ % coprime) sample)) set))
                  (str "Transposition by coprime interval " coprime " should create distinct scale"))))))))

  (testing "Structural requirements"
    (let [samples (gen/sample (s/gen ::musical-intervals) 20)]
      (doseq [sample samples]
        (let [result (kb/create-scale-group sample)]
          (is (= 12 (count result))
              "Result should have entries for all 12 chromatic notes")
          (doseq [[root scale] result]
            (is (= (count scale) (count sample))
                (str "Scale should have same number of notes as intervals for " root " scale"))))))))

(deftest generative-scale-group-tests
  (testing "Generative tests for create-scale-group function"
    (let [check-results (stest/check `kb/create-scale-group
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 10}})]
      (is (true? (get-in (first check-results) [:clojure.spec.test.check/ret :pass?]))
          (str "Generative tests failed: " (-> check-results first :failure))))))

;; =========================================================
;; Tests for transpose-note function
;; =========================================================

(deftest test-transpose-note-basic
  (testing "Base cases"
    (is (nil? (kb/transpose-note nil 1))
        "Nil note should return nil")

    (is (nil? (kb/transpose-note nil nil))
        "Nil note and nil transposition should return nil")

    (is (= (kb/create-note :c 4)
           (kb/transpose-note (kb/create-note :c 4) nil))
        "Nil transposition should return original note")

    (is (= (kb/create-note :c 4)
           (kb/transpose-note (kb/create-note :c 4) 0))
        "Zero transposition should return original note")))

(deftest test-transpose-note-single-steps
  (testing "Transposing up by single step"
    (is (= (kb/create-note :c 5)
           (kb/transpose-note (kb/create-note :b 4) 1))
        "B → C (octave boundary crossing up)")
    (is (= (kb/create-note :csdf 4)
           (kb/transpose-note (kb/create-note :c 4) 1))
        "C → C#/Db")
    (is (= (kb/create-note :d 4)
           (kb/transpose-note (kb/create-note :csdf 4) 1))
        "C#/Db → D")
    (is (= (kb/create-note :dsef 4)
           (kb/transpose-note (kb/create-note :d 4) 1))
        "D → D#/Eb")
    (is (= (kb/create-note :e 4)
           (kb/transpose-note (kb/create-note :dsef 4) 1))
        "D#/Eb → E")
    (is (= (kb/create-note :f 4)
           (kb/transpose-note (kb/create-note :e 4) 1))
        "E → F")
    (is (= (kb/create-note :fsgf 4)
           (kb/transpose-note (kb/create-note :f 4) 1))
        "F → F#/Gb")
    (is (= (kb/create-note :g 4)
           (kb/transpose-note (kb/create-note :fsgf 4) 1))
        "F#/Gb → G")
    (is (= (kb/create-note :gsaf 4)
           (kb/transpose-note (kb/create-note :g 4) 1))
        "G → G#/Ab")
    (is (= (kb/create-note :a 4)
           (kb/transpose-note (kb/create-note :gsaf 4) 1))
        "G#/Ab → A")
    (is (= (kb/create-note :asbf 4)
           (kb/transpose-note (kb/create-note :a 4) 1))
        "A → A#/Bb")
    (is (= (kb/create-note :b 4)
           (kb/transpose-note (kb/create-note :asbf 4) 1))
        "A#/Bb → B")))

(deftest test-transpose-note-multi-steps
  (testing "Transposing up by multiple steps"
    (is (= (kb/create-note :e 4)
           (kb/transpose-note (kb/create-note :c 4) 4))
        "C → E (up 4 semitones)")

    (is (= (kb/create-note :c 5)
           (kb/transpose-note (kb/create-note :c 4) 12))
        "C → C (up 1 octave)")

    (is (= (kb/create-note :fsgf 4)
           (kb/transpose-note (kb/create-note :c 4) 6))
        "C → F#/Gb (up tritone)"))

  (testing "Transposing down by multiple steps"
    (is (= (kb/create-note :a 3)
           (kb/transpose-note (kb/create-note :c 4) -3))
        "C → A (down 3 semitones)")

    (is (= (kb/create-note :c 3)
           (kb/transpose-note (kb/create-note :c 4) -12))
        "C → C (down 1 octave)")

    (is (= (kb/create-note :fsgf 3)
           (kb/transpose-note (kb/create-note :c 4) -6))
        "C → F#/Gb (down tritone)")))

(deftest test-transpose-note-octave-changes
  (testing "Octave changes when crossing B-C boundary"
    (is (= (kb/create-note :c 5)
           (kb/transpose-note (kb/create-note :b 4) 1))
        "B4 → C5 (up, crossing octave)")

    (is (= (kb/create-note :b 3)
           (kb/transpose-note (kb/create-note :c 4) -1))
        "C4 → B3 (down, crossing octave)")

    (is (= (kb/create-note :d 5)
           (kb/transpose-note (kb/create-note :b 3) 15))
        "B3 → D5 (up 15 semitones, multiple octave crossings)")

    (is (= (kb/create-note :a 2)
           (kb/transpose-note (kb/create-note :c 4) -15))
        "C4 → A2 (down 15 semitones, multiple octave crossings)")))

(deftest test-transpose-note-edge-cases
  (testing "Extreme transpositions"
    (is (= (kb/create-note :c 104)
           (kb/transpose-note (kb/create-note :c 4) 1200))
        "C4 → C104 (up 100 octaves)")

    (is (= (kb/create-note :c -96)
           (kb/transpose-note (kb/create-note :c 4) -1200))
        "C4 → C-96 (down 100 octaves)"))

  (testing "Edge cases for octaves"
    (is (= (kb/create-note :b -3)
           (kb/transpose-note (kb/create-note :c -2) -1))
        "C-2 → B-3 (from low octave down)")

    (is (= (kb/create-note :c 10)
           (kb/transpose-note (kb/create-note :b 9) 1))
        "B9 → C10 (from high octave up)")))

(deftest test-transpose-note-properties
  (testing "Full chromatic scale transposition"
    (doseq [i (range 12)]
      (let [current-note (kb/create-note (nth all-notes i) 4)
            next-idx (mod (inc i) 12)
            next-octave (if (= next-idx 0) 5 4)
            next-note (kb/create-note (nth all-notes next-idx) next-octave)]
        (is (= next-note (kb/transpose-note current-note 1))
            (str (nth all-notes i) "4 → " (nth all-notes next-idx) next-octave)))))

  (testing "Property: round trip transposition"
    (let [note-samples (gen/sample (s/gen ::kb/note) 10)
          transposition-samples (gen/sample (s/gen ::kb/transposition-amount) 10)]
      (doseq [note note-samples
              n transposition-samples]
        (let [note-obj (kb/create-note (:name note) (:octave note))]
          (is (= note-obj
                 (kb/transpose-note (kb/transpose-note note-obj n) (- n)))
              (str "Transposing " note " by " n " then by " (- n)
                   " should return original note"))))))

  (testing "Property: octave preservation with 12-semitone transposition"
    (let [note-samples (gen/sample (s/gen ::kb/note) 20)]
      (doseq [note note-samples]
        (let [note-obj (kb/create-note (:name note) (:octave note))
              trans-up (kb/transpose-note note-obj 12)
              trans-down (kb/transpose-note note-obj -12)]
          (is (= (:name note) (:name trans-up))
              "Transposing up by 12 should preserve note name")
          (is (= (:name note) (:name trans-down))
              "Transposing down by 12 should preserve note name")
          (is (= (+ (:octave note) 1) (:octave trans-up))
              "Transposing up by 12 should increase octave by 1")
          (is (= (- (:octave note) 1) (:octave trans-down))
              "Transposing down by 12 should decrease octave by 1")))))

  (testing "Property: composable transpositions"
    (let [note-samples (take 10 (gen/sample (s/gen ::kb/note)))
          trans-pairs (take 20 (gen/sample
                                (s/gen (s/tuple
                                        (s/int-in -20 20)
                                        (s/int-in -20 20)))))]
      (doseq [note note-samples
              [t1 t2] trans-pairs]
        (let [note-obj (kb/create-note (:name note) (:octave note))
              direct (kb/transpose-note note-obj (+ t1 t2))
              sequential (-> note-obj
                             (kb/transpose-note t1)
                             (kb/transpose-note t2))]
          (is (= direct sequential)
              (str "Transposing " note " by " t1 " then " t2
                   " should equal direct transposition by " (+ t1 t2))))))))

(deftest test-transpose-note-modulo
  (testing "Modulo 12 transposition equivalence for note names"
    (let [note-samples (take 10 (gen/sample (s/gen ::kb/note)))
          large-trans (take 10 (gen/sample (s/gen (s/int-in 12 100))))]
      (doseq [note note-samples
              trans large-trans]
        (let [note-obj (kb/create-note (:name note) (:octave note))
              full-trans (kb/transpose-note note-obj trans)
              mod-trans (kb/transpose-note note-obj (mod trans 12))
              octave-diff (quot trans 12)]
          (is (= (:name full-trans) (:name mod-trans))
              "Note name should be the same for full and modulo transposition")
          (is (= (:octave full-trans) (+ (:octave mod-trans) octave-diff))
              "Octave should differ by the expected amount"))))))

(deftest test-natural-note
  (testing "Natural note identification"
    (is (true? (kb/natural-note? (kb/create-note :c 4)))
        "C should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :d 4)))
        "D should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :e 4)))
        "E should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :f 4)))
        "F should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :g 4)))
        "G should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :a 4)))
        "A should be identified as a natural note")
    (is (true? (kb/natural-note? (kb/create-note :b 4)))
        "B should be identified as a natural note")
    (is (false? (kb/natural-note? (kb/create-note :csdf 4)))
        "C#/Db should not be identified as a natural note")
    (is (false? (kb/natural-note? (kb/create-note :dsef 4)))
        "D#/Eb should not be identified as a natural note")
    (is (false? (kb/natural-note? (kb/create-note :fsgf 4)))
        "F#/Gb should not be identified as a natural note")
    (is (false? (kb/natural-note? (kb/create-note :gsaf 4)))
        "G#/Ab should not be identified as a natural note")
    (is (false? (kb/natural-note? (kb/create-note :asbf 4)))
        "A#/Bb should not be identified as a natural note")))

(deftest test-get-flat-equivalent
  (testing "Get flat equivalent of nil"
    (is (nil? (kb/get-flat-equivalent nil))
        "Nils flat equivalent is nil."))

  (testing "Get flat equivalent of incomplete data structure"
    (is (nil? (kb/get-flat-equivalent {:name :c}))
        "C has no flat equivalent")
    (is (= {:name :csdf} (kb/get-flat-equivalent {:name :d}))
        "D's flat equivalent is C#/Db")
    (is (= nil (kb/get-flat-equivalent {:octave 2}))
        "No available mappings yields nil")
    (is (= nil (kb/get-flat-equivalent {}))
        "No data at all should yield nil."))

  (testing "Get flat equivalent of natural notes"
    (is (nil? (kb/get-flat-equivalent (kb/create-note :c 4)))
        "C has no flat equivalent")
    (is (= (kb/create-note :csdf 4)
           (kb/get-flat-equivalent (kb/create-note :d 4)))
        "D's flat equivalent is C#/Db")
    (is (= (kb/create-note :dsef 4)
           (kb/get-flat-equivalent (kb/create-note :e 4)))
        "E's flat equivalent is D#/Eb")
    (is (nil? (kb/get-flat-equivalent (kb/create-note :f 4)))
        "F has no flat equivalent")
    (is (= (kb/create-note :fsgf 4)
           (kb/get-flat-equivalent (kb/create-note :g 4)))
        "G's flat equivalent is F#/Gb")
    (is (= (kb/create-note :gsaf 4)
           (kb/get-flat-equivalent (kb/create-note :a 4)))
        "A's flat equivalent is G#/Ab")
    (is (= (kb/create-note :asbf 4)
           (kb/get-flat-equivalent (kb/create-note :b 4)))
        "B's flat equivalent is A#/Bb"))

  (testing "Property: get-flat-equivalent preserves octave for all natural notes with mappings"
    (let [natural-notes-with-mappings [:d :e :g :a :b]
          octaves (range -3 8)]
      (doseq [note-name natural-notes-with-mappings
              octave octaves]
        (let [note (kb/create-note note-name octave)
              flat-equiv (kb/get-flat-equivalent note)]
          (is (= octave (:octave flat-equiv))
              (str note-name octave " flat equivalent should preserve octave"))))))

  (testing "Property: get-flat-equivalent always returns nil for notes without mappings"
    (let [natural-notes-without-mappings [:c :f :csdf :dsef :fsgf :gsaf :asbf]
          octaves (range -3 8)]
      (doseq [note-name natural-notes-without-mappings
              octave octaves]
        (let [note (kb/create-note note-name octave)
              flat-equiv (kb/get-flat-equivalent note)]
          (is (nil? flat-equiv))))))

  (testing "Property: get-flat-equivalent handles non-note inputs gracefully"
    (let [non-notes [nil
                     42
                     "string"
                     []
                     {}
                     {:not-a-note true}
                     {:name "not-a-keyword"}]]
      (doseq [input non-notes]
        (is (nil? (kb/get-flat-equivalent input))
            (str "Non-note input " input " should be nil"))))))

(deftest test-create-note-predicate-from-collection
  (testing "Edge case: note passed into predicate is nil"
    (let [c-major-notes [:c :e :g]
          predicate (kb/create-note-predicate-from-collection c-major-notes)]
      (is (false? (predicate nil))
          "Predicate should return false for nil")))

  (testing "Edge case: list passed into predicate generator is nil"
    (let [predicate (kb/create-note-predicate-from-collection nil)]
      (is (false? (predicate (kb/create-note :c 4)))
          "Predicate should return false for C in nil list")))

  (testing "Edge case: list passed into predicate generator is nil as
            well as note passed into generated function"
    (let [predicate (kb/create-note-predicate-from-collection nil)]
      (is (false? (predicate nil))
          "Predicate should return false for nil in nil list")))

  (testing "Basic note collection predicate"
    (let [c-major-notes [:c :e :g]
          predicate (kb/create-note-predicate-from-collection c-major-notes)]
      (is (true? (predicate (kb/create-note :c 4)))
          "Predicate should return true for C in C major")
      (is (true? (predicate (kb/create-note :e 4)))
          "Predicate should return true for E in C major")
      (is (true? (predicate (kb/create-note :g 4)))
          "Predicate should return true for G in C major")
      (is (false? (predicate (kb/create-note :d 4)))
          "Predicate should return false for D in C major")
      (is (false? (predicate (kb/create-note :f 4)))
          "Predicate should return false for F in C major")
      (is (false? (predicate (kb/create-note :b 4)))
          "Predicate should return false for B in C major")))

  (testing "Predicate with accidentals"
    (let [e-minor-notes [:e :fsgf :g :a :b :csdf :dsef]
          predicate (kb/create-note-predicate-from-collection e-minor-notes)]
      (is (true? (predicate (kb/create-note :e 4)))
          "Predicate should return true for E in E minor")
      (is (true? (predicate (kb/create-note :fsgf 3)))
          "Predicate should return true for F#/Gb in E minor")
      (is (false? (predicate (kb/create-note :gsaf 6)))
          "Predicate should return true for G#/Ab in E minor")
      (is (false? (predicate (kb/create-note :c 4)))
          "Predicate should return false for C in E minor")
      (is (false? (predicate (kb/create-note :f 4)))
          "Predicate should return false for F in E minor")))

  (testing "Predicate with empty collection"
    (let [empty-notes []
          predicate (kb/create-note-predicate-from-collection empty-notes)]
      (is (false? (predicate (kb/create-note :c 4)))
          "Predicate should return false for any note with empty collection")
      (is (false? (predicate (kb/create-note :d 5)))
          "Predicate should return false for any note with empty collection")
      (is (false? (predicate (kb/create-note :fsgf 5)))
          "Predicate should return false for any note with empty collection")
      (is (false? (predicate (kb/create-note :gsaf 3)))
          "Predicate should return false for any note with empty collection")))

  (testing "Predicate is octave agnostic"
    (let [notes [:c :e :g]
          predicate (kb/create-note-predicate-from-collection notes)]
      (doseq [i (range -2 10)]
        (is (true? (predicate (kb/create-note :c i)))
            "Predicate should return true regardless of octave")
        (is (false? (predicate (kb/create-note :f i)))
            "Predicate should return true regardless of octave")
        (is (false? (predicate (kb/create-note :fsgf i)))
            "Predicate should return true regardless of octave")))))

(deftest test-keyboard-protocol-implementation
  (testing "ChromaticKeyboard implementation"
    (let [keyboard (kb/create-chromatic-keyboard (kb/create-note :c 4))
          rows (kb/get-rows keyboard)]
      (testing "Basic keyboard structure"
        (is (map? rows) "get-rows should return a map")
        (is (contains? rows :top) "Rows should contain :top key")
        (is (contains? rows :bottom) "Rows should contain :bottom key")
        (is (vector? (:top rows)) "Top row should be a vector")
        (is (vector? (:bottom rows)) "Bottom row should be a vector"))

      (testing "Shift operations"
        (let [shifted-right (kb/shift-right keyboard)
              right-rows (kb/get-rows shifted-right)
              first-bottom (first (:bottom right-rows))
              shifted-left (kb/shift-left keyboard)
              left-rows (kb/get-rows shifted-left)
              first-bottom-left (first (:bottom left-rows))]
          (is (= (kb/create-note :c 4) first-bottom)
              "Shifting right should not move yet, because C and C# display the same keyboard.")
          (is (= (kb/create-note :b 3) first-bottom-left)
              "Shifting left should move to previous note (C to B)")))

      (testing "Filter and map operations"
        (let [c-major-scale [:c :d :e :f :g :a :b]
              filter-fn (kb/create-note-predicate-from-collection c-major-scale)
              filtered-keyboard (kb/filter-notes keyboard filter-fn)
              filtered-rows (kb/get-rows filtered-keyboard)
              c-note (kb/create-note :c 4)
              csdf-note (kb/create-note :csdf 4)
              map-fn (fn [note]
                       (when note
                         (kb/transpose-note note 1)))
              mapped-keyboard (kb/map-notes keyboard map-fn)
              mapped-rows (kb/get-rows mapped-keyboard)]
          (is (some #(= c-note %) (flatten (vals filtered-rows)))
              "C note should be present in filtered keyboard")
          (is (not (some #(= csdf-note %) (flatten (vals filtered-rows))))
              "C#/Db note should not be present in filtered keyboard")
          (is (= (kb/create-note :csdf 4)
                 (first (:bottom mapped-rows)))
              "First note in mapped keyboard should be C# (C transposed up)")))))

  (testing "FoldingKeyboard implementation"
    (let [keyboard (kb/create-folding-keyboard (kb/create-note :c 4))
          rows (kb/get-rows keyboard)]
      (testing "Basic keyboard structure"
        (is (map? rows) "get-rows should return a map")
        (is (contains? rows :top) "Rows should contain :top key")
        (is (contains? rows :bottom) "Rows should contain :bottom key")
        (is (= 8 (count (:top rows))) "Top row should have 8 notes")
        (is (= 8 (count (:bottom rows))) "Bottom row should have 8 notes"))

      (testing "Shift operations"
        (let [shifted-right (kb/shift-right keyboard)
              right-rows (kb/get-rows shifted-right)
              first-note (first (:bottom right-rows))
              shifted-left (kb/shift-left keyboard)
              left-rows (kb/get-rows shifted-left)
              first-note-left (first (:bottom left-rows))]
          (is (= (kb/create-note :csdf 4) first-note)
              "Shifting right should move to next note (C to C#/Db)")
          (is (= (kb/create-note :b 3) first-note-left)
              "Shifting left should move to previous note (C to B)")))

      (testing "Filter and map operations"
        (let [c-major-scale [:c :d :e :f :g :a :b]
              filter-fn (kb/create-note-predicate-from-collection c-major-scale)
              filtered-keyboard (kb/filter-notes keyboard filter-fn)
              filtered-rows (kb/get-rows filtered-keyboard)
              map-fn (fn [note]
                       (when note
                         (kb/transpose-note note 12)))
              mapped-keyboard (kb/map-notes keyboard map-fn)
              mapped-rows (kb/get-rows mapped-keyboard)]
          (is (some? (some #(and % (= (:name %) :c)) (flatten (vals filtered-rows))))
              "C note should be present in filtered keyboard")
          (is (not (some #(and % (= (:name %) :csdf)) (flatten (vals filtered-rows))))
              "C#/Db note should not be present in filtered keyboard")
          (is (= (kb/create-note :c 5)
                 (first (:bottom mapped-rows)))
              "First note in mapped keyboard should be C5 (C4 transposed up an octave)"))))))

(deftest build-chord-generative-tests
  (testing "Generative tests for build-chord"
    (let [check-results (stest/check `kb/build-chord
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 100}})]
      (is (true? (get-in (first check-results) [:clojure.spec.test.check/ret :pass?]))
          (str "build-chord generative tests failed: " (-> check-results first :failure))))))

(deftest build-chord-property-tests
  (testing "Property: Chord inversion preserves note names"
    (let [chord-samples [[:c :e :g]  ; C major
                         [:a :c :e]  ; A minor
                         [:g :b :d :f]] ; G dominant 7
          octaves [3 4 5]
          inversions [0 1 2 3]]
      (doseq [chord chord-samples
              octave octaves
              inversion inversions]
        (when (< inversion (count chord))
          (let [built-chord (kb/build-chord chord octave inversion)
                chord-note-names (set (map :name built-chord))]
            (is (= (set chord) chord-note-names)
                (str "Inversion " inversion " of " chord
                     " should contain the same note names")))))))

  (testing "Property: Chord spans correct number of octaves"
    (let [big-chord [:c :e :g :b
                     :c :e :g :b
                     :c :e :g :b
                     :c :e :g :b]  ; C major 7th chord, looped multiple times
          octave 4]
      (doseq [size (range 1 (inc (count big-chord)))]
        (let [chord (take size big-chord)
              built-chord (kb/build-chord chord octave 0)
              min-octave (apply min (map :octave built-chord))
              max-octave (apply max (map :octave built-chord))
              expected-span (quot (dec size) 4)] ;; after b, we start getting more octaves.
          (is (= expected-span (- max-octave min-octave))
              (str "Chord of size " size " should span " expected-span " octaves"))))))

  (testing "Property: Inversions rotate the chord correctly"
    (let [chord [:c :e :g]  ; C major triad
          octave 4]
      ;; Root position (0 inversion)
      (let [root-chord (kb/build-chord chord octave 0)]
        (is (= :c (:name (first root-chord)))
            "Root position should start with C")
        (is (= 4 (:octave (first root-chord)))
            "Root position should start at specified octave"))

      ;; First inversion
      (let [first-inv (kb/build-chord chord octave 1)]
        (is (= :e (:name (first first-inv)))
            "First inversion should start with E")
        (is (= 4 (:octave (first first-inv)))
            "First inversion should start at specified octave")
        (is (= :c (:name (last first-inv)))
            "First inversion should end with C")
        (is (= 5 (:octave (last first-inv)))
            "First inversion should have C in next octave"))

      ;; Second inversion
      (let [second-inv (kb/build-chord chord octave 2)]
        (is (= :g (:name (first second-inv)))
            "Second inversion should start with G")
        (is (= 4 (:octave (first second-inv)))
            "Second inversion should start at specified octave")
        (is (= :e (:name (last second-inv)))
            "Second inversion should end with E")
        (is (= 5 (:octave (last second-inv)))
            "Second inversion should have E in next octave")))))

(deftest build-chord-blackbox-tests
  (testing "Empty chord construction"
    (is (= [] (kb/build-chord [] 4))
        "Empty note list should result in empty chord")
    (is (= [] (kb/build-chord [] 4 2))
        "Empty note list with inversion should result in empty chord"))

  (testing "Single note chord construction"
    (is (= [(kb/create-note :c 4)] (kb/build-chord [:c] 4))
        "Single note chord should contain just that note")
    (is (= [(kb/create-note :c 4)] (kb/build-chord [:c] 4 5))
        "Single note chord with inversion should still contain just that note"))

  (testing "Common chord construction - C major triad"
    (let [c-major-root (kb/build-chord [:c :e :g] 4 0)
          expected-root [(kb/create-note :c 4)
                         (kb/create-note :e 4)
                         (kb/create-note :g 4)]]
      (is (= expected-root c-major-root)
          "C major in root position should have correct notes and octaves")))

  (testing "Common chord construction - C major triad first inversion"
    (let [c-major-first (kb/build-chord [:c :e :g] 4 1)
          expected-first [(kb/create-note :e 4)
                          (kb/create-note :g 4)
                          (kb/create-note :c 5)]]
      (is (= expected-first c-major-first)
          "C major in first inversion should have correct notes and octaves")))

  (testing "Common chord construction - C major triad second inversion"
    (let [c-major-second (kb/build-chord [:c :e :g] 4 2)
          expected-second [(kb/create-note :g 4)
                           (kb/create-note :c 5)
                           (kb/create-note :e 5)]]
      (is (= expected-second c-major-second)
          "C major in second inversion should have correct notes and octaves")))

  (testing "Seventh chord construction - C dominant 7"
    (let [c-dom7 (kb/build-chord [:c :e :g :asbf] 3)
          expected [(kb/create-note :c 3)
                    (kb/create-note :e 3)
                    (kb/create-note :g 3)
                    (kb/create-note :asbf 3)]]
      (is (= expected c-dom7)
          "C dominant 7 should have correct notes and octaves")))

  (testing "Seventh chord construction - C dominant 7 third inversion"
    (let [c-dom7-3rd (kb/build-chord [:c :e :g :asbf] 3 3)
          expected [(kb/create-note :asbf 3)
                    (kb/create-note :c 4)
                    (kb/create-note :e 4)
                    (kb/create-note :g 4)]]
      (is (= expected c-dom7-3rd)
          "C dominant 7 in third inversion should have correct notes and octaves")))

  (testing "Negative octave handling"
    (let [chord (kb/build-chord [:c :e :g] -1)
          expected [(kb/create-note :c -1)
                    (kb/create-note :e -1)
                    (kb/create-note :g -1)]]
      (is (= expected chord)
          "Chord should support negative octaves")))

  (testing "Large inversion handling"
    (let [chord (kb/build-chord [:c :e :g] 4 100)  ; 100 % 3 = 1
          expected [(kb/create-note :e 4)
                    (kb/create-note :g 4)
                    (kb/create-note :c 5)]]
      (is (= expected chord)
          "Large inversion number should be properly modulo'd")))

  (testing "Negative inversion handling"
    (let [chord (kb/build-chord [:c :e :g] 4 -1)  ; -1 % 3 = -1 + 3 = 2
          expected [(kb/create-note :g 4)
                    (kb/create-note :c 5)
                    (kb/create-note :e 5)]]
      (is (= expected chord)
          "Negative inversion should wrap around correctly")))

  (testing "Non-standard chord construction"
    (let [chord (kb/build-chord [:c :fsgf :asbf] 4)
          expected [(kb/create-note :c 4)
                    (kb/create-note :fsgf 4)
                    (kb/create-note :asbf 4)]]
      (is (= expected chord)
          "Non-standard chord should be constructed correctly")))

  (testing "Chord with duplicate notes"
    (let [chord (kb/build-chord [:c :e :c] 4)
          expected [(kb/create-note :c 4)
                    (kb/create-note :e 4)
                    (kb/create-note :c 5)]]
      (is (= expected chord)
          "Chord with duplicate notes should have different octaves")))

  (testing "Multiple octave chord"
    (let [large-chord (kb/build-chord [:c :d :e :f :g :a :b :c :d :e] 3)
          highest-note (last large-chord)]
      (is (= 10 (count large-chord))
          "Large chord should contain all specified notes")
      (is (= :e (:name highest-note))
          "Last note of large chord should be E")
      (is (= 4 (:octave highest-note))
          "Last note of large chord should be in octave 4")))

  (testing "Large chord inversion"
    (let [note-names [:c :d :e :f :g :a :b :c :d :e]
          root-chord (kb/build-chord note-names 3 0)
          inv-5-chord (kb/build-chord note-names 3 5)
          root-first-note (first root-chord)
          inv-first-note (first inv-5-chord)
          root-last-note (last root-chord)
          inv-last-note (last inv-5-chord)]

    ;; Check that 5th inversion starts with the 6th note
      (is (= :a (:name inv-first-note))
          "5th inversion should start with A")
      (is (= 3 (:octave inv-first-note))
          "5th inversion should start in octave 3")

    ;; Check that 5th inversion ends with G in the next octave
      (is (= :g (:name inv-last-note))
          "5th inversion should end with G")
      (is (= 5 (:octave inv-last-note))
          "5th inversion should end in octave 5")

    ;; Check that both chords have the same notes but in different order
      (is (= (count root-chord) (count inv-5-chord))
          "Inverted chord should have same number of notes")
      (is (= (set (map :name root-chord)) (set (map :name inv-5-chord)))
          "Inverted chord should have same note names"))))

;; Run all tests
(run-tests)
