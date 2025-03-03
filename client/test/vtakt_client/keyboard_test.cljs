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

    (is (= (kb/->Note :c 4)
           (kb/transpose-note (kb/->Note :c 4) nil))
        "Nil transposition should return original note")

    (is (= (kb/->Note :c 4)
           (kb/transpose-note (kb/->Note :c 4) 0))
        "Zero transposition should return original note")))

(deftest test-transpose-note-single-steps
  (testing "Transposing up by single step"
    (is (= (kb/->Note :c 5)
           (kb/transpose-note (kb/->Note :b 4) 1))
        "B → C (octave boundary crossing up)")
    (is (= (kb/->Note :csdf 4)
           (kb/transpose-note (kb/->Note :c 4) 1))
        "C → C#/Db")
    (is (= (kb/->Note :d 4)
           (kb/transpose-note (kb/->Note :csdf 4) 1))
        "C#/Db → D")
    (is (= (kb/->Note :dsef 4)
           (kb/transpose-note (kb/->Note :d 4) 1))
        "D → D#/Eb")
    (is (= (kb/->Note :e 4)
           (kb/transpose-note (kb/->Note :dsef 4) 1))
        "D#/Eb → E")
    (is (= (kb/->Note :f 4)
           (kb/transpose-note (kb/->Note :e 4) 1))
        "E → F")
    (is (= (kb/->Note :fsgf 4)
           (kb/transpose-note (kb/->Note :f 4) 1))
        "F → F#/Gb")
    (is (= (kb/->Note :g 4)
           (kb/transpose-note (kb/->Note :fsgf 4) 1))
        "F#/Gb → G")
    (is (= (kb/->Note :gsaf 4)
           (kb/transpose-note (kb/->Note :g 4) 1))
        "G → G#/Ab")
    (is (= (kb/->Note :a 4)
           (kb/transpose-note (kb/->Note :gsaf 4) 1))
        "G#/Ab → A")
    (is (= (kb/->Note :asbf 4)
           (kb/transpose-note (kb/->Note :a 4) 1))
        "A → A#/Bb")
    (is (= (kb/->Note :b 4)
           (kb/transpose-note (kb/->Note :asbf 4) 1))
        "A#/Bb → B")))

(deftest test-transpose-note-multi-steps
  (testing "Transposing up by multiple steps"
    (is (= (kb/->Note :e 4)
           (kb/transpose-note (kb/->Note :c 4) 4))
        "C → E (up 4 semitones)")

    (is (= (kb/->Note :c 5)
           (kb/transpose-note (kb/->Note :c 4) 12))
        "C → C (up 1 octave)")

    (is (= (kb/->Note :fsgf 4)
           (kb/transpose-note (kb/->Note :c 4) 6))
        "C → F#/Gb (up tritone)"))

  (testing "Transposing down by multiple steps"
    (is (= (kb/->Note :a 3)
           (kb/transpose-note (kb/->Note :c 4) -3))
        "C → A (down 3 semitones)")

    (is (= (kb/->Note :c 3)
           (kb/transpose-note (kb/->Note :c 4) -12))
        "C → C (down 1 octave)")

    (is (= (kb/->Note :fsgf 3)
           (kb/transpose-note (kb/->Note :c 4) -6))
        "C → F#/Gb (down tritone)")))

(deftest test-transpose-note-octave-changes
  (testing "Octave changes when crossing B-C boundary"
    (is (= (kb/->Note :c 5)
           (kb/transpose-note (kb/->Note :b 4) 1))
        "B4 → C5 (up, crossing octave)")

    (is (= (kb/->Note :b 3)
           (kb/transpose-note (kb/->Note :c 4) -1))
        "C4 → B3 (down, crossing octave)")

    (is (= (kb/->Note :d 5)
           (kb/transpose-note (kb/->Note :b 3) 15))
        "B3 → D5 (up 15 semitones, multiple octave crossings)")

    (is (= (kb/->Note :a 2)
           (kb/transpose-note (kb/->Note :c 4) -15))
        "C4 → A2 (down 15 semitones, multiple octave crossings)")))

(deftest test-transpose-note-edge-cases
  (testing "Extreme transpositions"
    (is (= (kb/->Note :c 104)
           (kb/transpose-note (kb/->Note :c 4) 1200))
        "C4 → C104 (up 100 octaves)")

    (is (= (kb/->Note :c -96)
           (kb/transpose-note (kb/->Note :c 4) -1200))
        "C4 → C-96 (down 100 octaves)"))

  (testing "Edge cases for octaves"
    (is (= (kb/->Note :b -3)
           (kb/transpose-note (kb/->Note :c -2) -1))
        "C-2 → B-3 (from low octave down)")

    (is (= (kb/->Note :c 10)
           (kb/transpose-note (kb/->Note :b 9) 1))
        "B9 → C10 (from high octave up)")))

(deftest test-transpose-note-properties
  (testing "Full chromatic scale transposition"
    (doseq [i (range 12)]
      (let [current-note (kb/->Note (nth all-notes i) 4)
            next-idx (mod (inc i) 12)
            next-octave (if (= next-idx 0) 5 4)
            next-note (kb/->Note (nth all-notes next-idx) next-octave)]
        (is (= next-note (kb/transpose-note current-note 1))
            (str (nth all-notes i) "4 → " (nth all-notes next-idx) next-octave)))))

  (testing "Property: round trip transposition"
    (let [note-samples (gen/sample (s/gen ::kb/note) 10)
          transposition-samples (gen/sample (s/gen ::kb/transposition-amount) 10)]
      (doseq [note note-samples
              n transposition-samples]
        (let [note-obj (kb/map->Note note)]
          (is (= note-obj
                 (kb/transpose-note (kb/transpose-note note-obj n) (- n)))
              (str "Transposing " note " by " n " then by " (- n)
                   " should return original note"))))))

  (testing "Property: octave preservation with 12-semitone transposition"
    (let [note-samples (gen/sample (s/gen ::kb/note) 20)]
      (doseq [note note-samples]
        (let [note-obj (kb/map->Note note)
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
        (let [note-obj (kb/map->Note note)
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
        (let [note-obj (kb/map->Note note)
              full-trans (kb/transpose-note note-obj trans)
              mod-trans (kb/transpose-note note-obj (mod trans 12))
              octave-diff (quot trans 12)]
          (is (= (:name full-trans) (:name mod-trans))
              "Note name should be the same for full and modulo transposition")
          (is (= (:octave full-trans) (+ (:octave mod-trans) octave-diff))
              "Octave should differ by the expected amount"))))))

;; Run all tests
(run-tests)
