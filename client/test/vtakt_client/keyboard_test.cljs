(ns vtakt-client.keyboard-test
  (:require [cljs.test :refer-macros [deftest testing is are run-tests]]
            [vtakt-client.components.keyboard :as kb]
            [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as tcp]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))

(def common-scales
  {:major [0 2 4 5 7 9 11]
   :minor [0 2 3 5 7 8 10]
   :pentatonic-major [0 2 4 7 9]
   :pentatonic-minor [0 3 5 7 10]
   :blues [0 3 5 6 7 10]
   :chromatic [0 1 2 3 4 5 6 7 8 9 10 11]})

(s/def ::intervals (s/coll-of nat-int? :kind vector?))
(s/def ::musical-intervals
  (s/with-gen ::intervals
    (fn []
      (gen/frequency
       [[10 (gen/elements (vals common-scales))]
        [3  (gen/vector (gen/choose 0 11) 3 8)]
        [1  (gen/vector (gen/choose 0 24) 3 8)]]))))

(deftest test-expected-attributes-of-create-scale-group
  (testing "Make sure across random sample of input that attributes apply."
    (let [samples (gen/sample (s/gen ::musical-intervals) 10000)]
      (doseq [[i sample] (map-indexed vector samples)]
        (let [result (kb/create-scale-group sample)]
          ;; 1. Octave Equivalence Property
          ;; Notes separated by octaves are equivalent in the 12-tone system
          (is (= result (kb/create-scale-group (map #(+ % 12) sample))))
          ;; 2.) Coprime Transposition Property
          ;; For scales with fewer than 12 notes, transposing by intervals that are
          ;; coprime with 12 (1, 5, 7, 11) must generate distinct scale patterns.
          ;; This is because these intervals are generators of the cyclic group z_12
          ;; (integers modulo 12) and will traverse all 12 pitch classes.
          ;;
          ;; Conversely, non-coprime intervals like 2, 3, 4, 6 can generate equivalent
          ;; scales after transposition due to their shared factors with 12, creating
          ;; smaller cyclic subgroups in modular arithmetic.
          ;;
          ;; Example: The Whole Tone Scale [0 2 4 6 8 10] transposed by 2 semitones
          ;; yields [2 4 6 8 10 0], which contains the same pitch classes.
          ;; Similarly, diminished seventh chords have 3-semitone symmetry because
          ;; gcd(3,12)=3, creating a cycle of only 4 unique transpositions.
          (if (< (count sample) 12)
            (doseq [coprime [1 5 7 11]]
              (is (not=
                   (update-vals result set)
                   (update-vals (kb/create-scale-group (map #(+ % coprime) sample)) set)))))
          ;; TODO 3.) Interval Vector Inversion Equivalence Property
          ;; TODO 4.) Balzano Property - All Diatonic Like Scales Are Unique
          ;; TODO 5.) Z-Relation Property - This is interesting.. Not sure if applicable for this test, but I'd like to learn more about it.

          ;; 3.) Structural: Have results for all diatonic notes
          (is (= 12 (count result)))
          ;; 4.) Structural: # of intervals = # of notes in scale
          (doseq [[root scale] result]
            (is (= (count scale) (count sample)))))))))

;; Common note sequences for testing
(def all-notes [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b])

;; Common interval patterns
(def chromatic-intervals [0 1 2 3 4 5 6 7 8 9 10 11])
(def major-scale-intervals [0 2 4 5 7 9 11])
(def minor-scale-intervals [0 2 3 5 7 8 10])

(deftest test-empty-and-nil-inputs
  (testing "Empty interval list creates a map with empty vectors for all roots"
    (is (= (into {} (map (fn [root] [root []]) all-notes))
           (kb/create-scale-group []))))

  (testing "Nil interval list throws an error"
    (is (= (into {} (map (fn [root] [root []]) all-notes))
           (kb/create-scale-group nil)))))

(deftest test-single-element-scales
  (testing "Single interval [0] creates a map with single-element vectors for all roots"
    (is (= (into {} (map (fn [root] [root [root]]) all-notes))
           (kb/create-scale-group [0]))))

  (testing "Repeated intervals create scales with repeated notes"
    (is (= (into {} (map (fn [root] [root [root root]]) all-notes))
           (kb/create-scale-group [0 0])))))

(deftest test-two-note-scales
  (testing "Two-note scale with interval 1 semitone"
    (let [expected {:c [:c :csdf], :csdf [:csdf :d], :d [:d :dsef], :dsef [:dsef :e],
                    :e [:e :f], :f [:f :fsgf], :fsgf [:fsgf :g], :g [:g :gsaf],
                    :gsaf [:gsaf :a], :a [:a :asbf], :asbf [:asbf :b], :b [:b :c]}]
      (is (= expected (kb/create-scale-group [0 1])))))

  (testing "Octave equivalent intervals are normalized to one octave"
    (is (= (kb/create-scale-group [0 1])
           (kb/create-scale-group [0 13])))))

(deftest test-standard-scales
  (testing "Major scale structure is correctly generated"
    (let [expected {:c [:c :d :e :f :g :a :b],
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
                    :b [:b :csdf :dsef :e :fsgf :gsaf :asbf]}]
      (is (= expected (kb/create-scale-group major-scale-intervals)))
      (is (= expected (kb/create-scale-group [0 2 4 5 7 9 11])))
      (is (= expected (kb/create-scale-group [0 14 4 17 7 21 11])))
      (is (= expected (kb/create-scale-group [12 14 16 17 19 21 23])))))

  (testing "Minor scale structure is correctly generated"
    (let [expected {:c [:c :d :dsef :f :g :gsaf :asbf],
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
                    :b [:b :csdf :d :e :fsgf :g :a]}]
      (is (= expected (kb/create-scale-group minor-scale-intervals)))))

  (testing "Chromatic scale includes all twelve notes"
    (let [expected {:c [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b],
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
                    :b [:b :c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf]}]
      (is (= expected (kb/create-scale-group chromatic-intervals))))))

(deftest test-scale-properties
  (testing "Output always contains the same number of notes as intervals"
    (let [interval-counts [1 10 100 1000]
          results (map #(kb/create-scale-group (into [] (take % (range)))) interval-counts)]
      (doseq [[intervals result] (map vector interval-counts results)]
        (doseq [scale (vals result)]
          (is (= intervals (count scale)))))))

  (testing "First note of each scale is always the root note"
    (let [result (kb/create-scale-group major-scale-intervals)]
      (doseq [[root scale] result]
        (is (= root (first scale)))))))

(deftest generative-scale-group-tests
  (testing "Generative tests for create-scale-group function"
    (let [check-results (stest/check `kb/create-scale-group
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 10}})]
      (is (true? (get-in (first check-results) [:clojure.spec.test.check/ret :pass?]))
          (str "Failed with: " (-> check-results first :failure))))))

(deftest test-error-handling
  (testing "Negative numbers throw error"
    (is (thrown? js/Error (kb/create-scale-group [-1]))))

  (testing "Negative numbers in otherwise valid list throw error"
    (is (thrown? js/Error (kb/create-scale-group [1 -1 2]))))

  (testing "Decimal numbers throw error"
    (is (thrown? js/Error (kb/create-scale-group [1.5]))))

  (testing "Non-numerics"
    (is (thrown? js/Error (kb/create-scale-group ["a" "b" "c"])))))

(kb/transpose-note (kb/->Note :c 4) nil)

(deftest transpose-note-test
  ;; Base cases
  (testing "nil inputs"
    (is (nil? (kb/transpose-note nil 1)))
    (is (= (kb/transpose-note (kb/->Note :c 4) nil) (kb/->Note :c 4))))
  (testing "zero transposition"
    (let [note (kb/->Note :c 4)]
      (is (= note (kb/transpose-note note 0)))))
  ;; Single-step transpositions
  (testing "transposing up by 1"
    (is (= (kb/->Note :csdf 4) (kb/transpose-note (kb/->Note :c 4) 1)))
    (is (= (kb/->Note :d 4) (kb/transpose-note (kb/->Note :csdf 4) 1)))
    (is (= (kb/->Note :c 5) (kb/transpose-note (kb/->Note :b 4) 1))))
  (testing "transposing down by 1"
    (is (= (kb/->Note :b 3) (kb/transpose-note (kb/->Note :c 4) -1)))
    (is (= (kb/->Note :asbf 3) (kb/transpose-note (kb/->Note :b 3) -1)))
    (is (= (kb/->Note :c 3) (kb/transpose-note (kb/->Note :csdf 3) -1))))
  ;; Multi-step transpositions
  (testing "transposing up by multiple steps"
    (is (= (kb/->Note :e 4) (kb/transpose-note (kb/->Note :c 4) 4)))
    (is (= (kb/->Note :c 5) (kb/transpose-note (kb/->Note :c 4) 12)))
    (is (= (kb/->Note :fsgf 4) (kb/transpose-note (kb/->Note :c 4) 6))))
  (testing "transposing down by multiple steps"
    (is (= (kb/->Note :a 3) (kb/transpose-note (kb/->Note :c 4) -3)))
    (is (= (kb/->Note :c 3) (kb/transpose-note (kb/->Note :c 4) -12)))
    (is (= (kb/->Note :fsgf 3) (kb/transpose-note (kb/->Note :c 4) -6))))
  ;; Octave changes
  (testing "octave changes when crossing B-C boundary"
    ;; Moving up from B to C increases octave
    (is (= (kb/->Note :c 5) (kb/transpose-note (kb/->Note :b 4) 1)))
    ;; Moving down from C to B decreases octave
    (is (= (kb/->Note :b 3) (kb/transpose-note (kb/->Note :c 4) -1)))
    ;; Multiple crossings up
    (is (= (kb/->Note :d 5) (kb/transpose-note (kb/->Note :b 3) 15)))
    ;; Multiple crossings down
    (is (= (kb/->Note :a 2) (kb/transpose-note (kb/->Note :c 4) -15))))
  ;; Extreme cases
  (testing "large transpositions"
    ;; Up 100 octaves (1200 semitones)
    (is (= (kb/->Note :c 104) (kb/transpose-note (kb/->Note :c 4) 1200)))
    ;; Down 100 octaves
    (is (= (kb/->Note :c -96) (kb/transpose-note (kb/->Note :c 4) -1200))))
  ;; Edge cases
  (testing "edge cases for octaves"
    ;; From lowest octave down
    (is (= (kb/->Note :b -3) (kb/transpose-note (kb/->Note :c -2) -1)))
    ;; From highest octave up
    (is (= (kb/->Note :c 10) (kb/transpose-note (kb/->Note :b 9) 1))))
  ;; Property-based tests
  (testing "property: transposing by n then by -n returns the original note"
    (let [note-samples (gen/sample (s/gen ::kb/note) 10)
          transposition-samples (gen/sample (s/gen ::kb/transposition-amount) 10)]
      (doseq [note note-samples
              n transposition-samples]
        (is (= (kb/map->Note note) (kb/transpose-note (kb/transpose-note (kb/map->Note note) n) (- n)))
            (str "Failed roundtrip test for note " note " with transposition " n)))))
  (testing "property: transposing by 12 changes octave but preserves note name"
    (let [note-samples (gen/sample (s/gen ::kb/note) 50)]
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
  (testing "property: composable transpositions"
    (let [note-samples (gen/sample (s/gen ::kb/note) 20)
          trans-pairs (take 40000 (gen/sample
                                   (s/gen (s/tuple
                                           (s/int-in -100 100)
                                           (s/int-in -100 100)))))]
      (doseq [note note-samples
              [t1 t2] trans-pairs]
        (let [note-obj (kb/map->Note note)
              direct (kb/transpose-note note-obj (+ t1 t2))
              sequential (-> note-obj
                             (kb/transpose-note t1)
                             (kb/transpose-note t2))]
          (is (= direct sequential)
              (str "Transposing by " t1 " then " t2 " should equal direct transposition by " (+ t1 t2)))))))
  (testing "property: modulo 12 transposition equivalence for note names"
    (let [note-samples (gen/sample (s/gen ::kb/note) 40)
          large-trans (gen/sample (s/gen (s/int-in 12 500)) 40)
          chromatic-notes [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b]]
      (doseq [note note-samples
              trans large-trans]
        (let [note-obj (kb/map->Note note)
              full-trans (kb/transpose-note note-obj trans)
              mod-trans (kb/transpose-note note-obj (mod trans 12))
              octave-diff (quot trans 12)]
          (is (= (:name full-trans) (:name mod-trans))
              "Note name should be the same for full and modulo transposition")
          (is (= (:octave full-trans) (+ (:octave mod-trans) octave-diff))
              "Octave should differ by the expected amount")))))
  (testing "full chromatic scale transposition"
    ;; Test every note in the scale
    (let [chromatic-notes [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b]]
      (doseq [i (range 12)]
        (let [current-note (kb/->Note (nth chromatic-notes i) 4)
              next-idx (mod (inc i) 12)
              next-octave (if (= next-idx 0) 5 4)
              next-note (kb/->Note (nth chromatic-notes next-idx) next-octave)]
          (is (= next-note (kb/transpose-note current-note 1))))))))

(run-tests)

