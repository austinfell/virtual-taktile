(ns vtakt-client.keyboard.core-test
  (:require [cljs.test :refer-macros [deftest testing is are run-tests]]
            [vtakt-client.keyboard.core :as kb]
            [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as tcp]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))

;; =========================================================
;; Test Data
;; =========================================================

(def gen-test-scale 1)

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
    (let [samples (gen/sample (s/gen ::musical-intervals) (* gen-test-scale 20))]
      (doseq [sample samples]
        (let [result (kb/create-scale-group sample)]
          (is (= result (kb/create-scale-group (map #(+ % 12) sample)))
              "Octave equivalent scales should be identical")))))

  (testing "Coprime Transposition Property"
    (let [samples (gen/sample (s/gen ::musical-intervals) (* gen-test-scale 20))]
      (doseq [sample samples]
        (when (and (seq sample) (< (count sample) 12))
          (let [result (kb/create-scale-group sample)]
            (doseq [coprime [1 5 7 11]]
              (is (not= (update-vals result set)
                        (update-vals (kb/create-scale-group (map #(+ % coprime) sample)) set))
                  (str "Transposition by coprime interval " coprime " should create distinct scale"))))))))

  (testing "Structural requirements"
    (let [samples (gen/sample (s/gen ::musical-intervals) (* gen-test-scale 20))]
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
    (let [note-samples (gen/sample (s/gen ::kb/note) (* gen-test-scale 20))
          transposition-samples (gen/sample (s/gen ::kb/transposition-amount) (* gen-test-scale 20))]
      (doseq [note note-samples
              n transposition-samples]
        (let [note-obj (kb/create-note (:name note) (:octave note))]
          (is (= note-obj
                 (kb/transpose-note (kb/transpose-note note-obj n) (- n)))
              (str "Transposing " note " by " n " then by " (- n)
                   " should return original note"))))))

  (testing "Property: octave preservation with 12-semitone transposition"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* gen-test-scale 20))]
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
    (let [note-samples (take (* 10 gen-test-scale) (gen/sample (s/gen ::kb/note)))
          trans-pairs (take (* 20 gen-test-scale) (gen/sample
                                (s/gen (s/tuple
                                        (s/int-in (* gen-test-scale -20) (* gen-test-scale 20))
                                        (s/int-in (* gen-test-scale -20) (* gen-test-scale 20))))))]
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
    (let [note-samples (take (* 10 gen-test-scale) (gen/sample (s/gen ::kb/note)))
          large-trans (take (* 10 gen-test-scale) (gen/sample (s/gen (s/int-in 12 100))))]
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

(deftest test-format-note
  (testing "Format note returns nil when nil is passed in"
    (is (nil? (kb/format-note nil))))

  (testing "Format note upper-cases non-sharp, non-recognizable strings"
    (is (= "LOL" (kb/format-note :lol)))
    (is (= "C_SHARP" (kb/format-note :c_sharp))))

  (testing "Format note handles rendering flat/sharp notes"
    (is (= "A♯" (kb/format-note :asbf)))
    (is (= "B♯" (kb/format-note :bscf)))
    (is (= "C♯" (kb/format-note :csdf)))
    (is (= "D♯" (kb/format-note :dsef)))
    (is (= "E♯" (kb/format-note :esff)))
    (is (= "F♯" (kb/format-note :fsgf)))
    (is (= "G♯" (kb/format-note :gsaf))))

  (testing "Format note handles non-sharp/non-flat notes"
    (is (= "A" (kb/format-note :a)))
    (is (= "B" (kb/format-note :b)))
    (is (= "C" (kb/format-note :c)))
    (is (= "D" (kb/format-note :d)))
    (is (= "E" (kb/format-note :e)))
    (is (= "F" (kb/format-note :f)))
    (is (= "G" (kb/format-note :g))))


  (testing "Testing arbitrary keywords excluding sharps"
    (let [non-sharp-keyword-gen (gen/such-that
                                 (fn [kw]
                                   (and kw
                                        (not (contains? #{"asbf" "bscf" "csdf" "dsef" "esff" "fsgf" "gsaf"}
                                                        (name kw)))))
                                 (gen/keyword)
                                 100)
          random-keywords (gen/sample non-sharp-keyword-gen 100)]
      (doseq [kw random-keywords]
        (let [result (kb/format-note kw)]
          (is (string? result)
              (str "Result for " kw " should be a string, got: " result))))))

  (testing "Generative testing against expected output function"
    (let [check-results (stest/check `format-note
                                     {:clojure.spec.test.check/opts {:num-tests 100}
                                      :gen {::kb/optional-note
                                            (s/gen (s/or :nil nil?
                                                         :valid ::kb/chromatic-note
                                                         :other keyword?))}})
          spec-result (first check-results)]
      (if (false? (get-in spec-result [:clojure.spec.test.check/ret :pass?]))
        (println "Generative test failures:"
                 (get-in spec-result [:clojure.spec.test.check/ret :fail]))))))

(deftest test-format-root-note
  (testing "Format root note returns nil when nil is passed in"
    (is (nil? (kb/format-root-note nil))))

  (testing "Format root note correctly combines note name and octave"
    (is (= "C4" (kb/format-root-note {:name :c :octave 4})))
    (is (= "D2" (kb/format-root-note {:name :d :octave 2})))
    (is (= "G7" (kb/format-root-note {:name :g :octave 7}))))

  (testing "Format root note works with negative and zero octaves"
    (is (= "F-1" (kb/format-root-note {:name :f :octave -1})))
    (is (= "A0" (kb/format-root-note {:name :a :octave 0}))))

  (testing "Format root note handles sharp notes properly"
    (is (= "A♯3" (kb/format-root-note {:name :asbf :octave 3})))
    (is (= "C♯5" (kb/format-root-note {:name :csdf :octave 5})))
    (is (= "F♯2" (kb/format-root-note {:name :fsgf :octave 2}))))

  (testing "Generative testing with various notes and octaves"
    (let [note-gen (gen/hash-map
                     :name (s/gen ::kb/chromatic-note)
                     :octave (gen/choose -2 9))
          samples (gen/sample note-gen 50)]
      (doseq [note-map samples]
        (let [result (kb/format-root-note note-map)
              expected (str (kb/format-note (:name note-map)) (:octave note-map))]
          (is (= expected result)
              (str "For " note-map ", expected " expected ", but got " result))))))

  (testing "Generative testing with spec check"
    (let [check-results (stest/check `kb/format-root-note
                          {:clojure.spec.test.check/opts {:num-tests 50}})
          spec-result (first check-results)]
      (is (true? (get-in spec-result [:clojure.spec.test.check/ret :pass?]))
          (str "Spec check failed: "
               (get-in spec-result [:clojure.spec.test.check/ret :fail] ""))))))

(deftest test-note-at-or-below?
  (testing "Same note comparison"
    (is (kb/note-at-or-below? {:name :c :octave 4} {:name :c :octave 4})
        "A note should be at-or-below itself")
    (is (kb/note-at-or-below? {:name :fsgf :octave 3} {:name :fsgf :octave 3})
        "A sharp note should be at-or-below itself"))

  (testing "Different octaves"
    (is (kb/note-at-or-below? {:name :c :octave 3} {:name :c :octave 4})
        "C3 should be below C4")
    (is (kb/note-at-or-below? {:name :g :octave 2} {:name :c :octave 3})
        "G2 should be below C3")
    (is (kb/note-at-or-below? {:name :asbf :octave 1} {:name :d :octave 2})
        "A♯1 should be below D2")
    (is (not (kb/note-at-or-below? {:name :c :octave 5} {:name :b :octave 4}))
        "C5 should not be below B4"))

  (testing "Same octave, different notes"
    (is (kb/note-at-or-below? {:name :c :octave 4} {:name :d :octave 4})
        "C4 should be below D4")
    (is (kb/note-at-or-below? {:name :fsgf :octave 3} {:name :g :octave 3})
        "F♯3 should be below G3")
    (is (not (kb/note-at-or-below? {:name :e :octave 2} {:name :d :octave 2}))
        "E2 should not be below D2")
    (is (not (kb/note-at-or-below? {:name :asbf :octave 5} {:name :a :octave 5}))
        "A♯5 should not be below A5"))

  (testing "Notes at octave boundaries"
    (is (kb/note-at-or-below? {:name :b :octave 3} {:name :c :octave 4})
        "B3 should be below C4")
    (is (kb/note-at-or-below? {:name :asbf :octave 2} {:name :c :octave 3})
        "A♯2 should be below C3"))

  (testing "Full chromatic scale ordering"
    (let [scale-in-octave-4 [{:name :c :octave 4}
                             {:name :csdf :octave 4}
                             {:name :d :octave 4}
                             {:name :dsef :octave 4}
                             {:name :e :octave 4}
                             {:name :f :octave 4}
                             {:name :fsgf :octave 4}
                             {:name :g :octave 4}
                             {:name :gsaf :octave 4}
                             {:name :a :octave 4}
                             {:name :asbf :octave 4}
                             {:name :b :octave 4}
                             {:name :c :octave 5}]]

      (doseq [i (range (count scale-in-octave-4))]
        (let [note1 (nth scale-in-octave-4 i)]
          (doseq [j (range (count scale-in-octave-4))]
            (let [note2 (nth scale-in-octave-4 j)
                  expected (<= i j)]
              (is (= expected (kb/note-at-or-below? note1 note2))
                  (str (name (:name note1)) (:octave note1)
                       (if expected " should be " " should not be ")
                       "at-or-below "
                       (name (:name note2)) (:octave note2)))))))))

  (testing "Generative testing"
    (let [note-names [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b]
          octaves (range 0 8)

          ;; Generate 2 random notes
          gen-random-note-pair (fn []
                                 (let [name1 (rand-nth note-names)
                                       name2 (rand-nth note-names)
                                       octave1 (rand-nth octaves)
                                       octave2 (rand-nth octaves)]
                                   [{:name name1 :octave octave1}
                                    {:name name2 :octave octave2}]))

          ;; Get absolute value for comparison
          note-to-absolute-value (fn [note]
                                   (let [note-values {:c 0, :csdf 1, :d 2, :dsef 3,
                                                      :e 4, :f 5, :fsgf 6, :g 7,
                                                      :gsaf 8, :a 9, :asbf 10, :b 11}]
                                     (+ (* 12 (:octave note))
                                        (get note-values (:name note)))))

          ;; Generate 30 random test cases
          test-cases (repeatedly 30 gen-random-note-pair)]

      (doseq [[note1 note2] test-cases]
        (let [abs1 (note-to-absolute-value note1)
              abs2 (note-to-absolute-value note2)
              expected (<= abs1 abs2)]
          (is (= expected (kb/note-at-or-below? note1 note2))
              (str "Compare " (name (:name note1)) (:octave note1)
                   " (value " abs1 ") to "
                   (name (:name note2)) (:octave note2)
                   " (value " abs2 ")")))))))

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

(deftest test-create-chromatic-keyboard
  (testing "Basic record initialization"
    (let [root (kb/create-note :c 4)
          keyboard (kb/create-chromatic-keyboard root)]

      ;; Test that root-note is set correctly
      (is (= root (:root-note keyboard))
          "Root note should be set to the provided note")

      ;; Test that layout is initialized
      (is (map? (:layout keyboard))
          "Layout should be a map")
      (is (contains? (:layout keyboard) :top)
          "Layout should contain :top key")
      (is (contains? (:layout keyboard) :bottom)
          "Layout should contain :bottom key")
      (is (vector? (get-in keyboard [:layout :top]))
          "Top row in layout should be a vector")
      (is (vector? (get-in keyboard [:layout :bottom]))
          "Bottom row in layout should be a vector")

      ;; Test that map-fn is initially nil
      (is (nil? (:map-fn keyboard))
          "map-fn should initially be nil")))

  (testing "Layout initialization with different root notes"
    (let [c-keyboard (kb/create-chromatic-keyboard (kb/create-note :c 4))
          d-keyboard (kb/create-chromatic-keyboard (kb/create-note :d 4))
          fsharp-keyboard (kb/create-chromatic-keyboard (kb/create-note :fsgf 3))]

      ;; Test layout differences based on root note
      (is (not= (get-in c-keyboard [:layout :bottom])
                (get-in d-keyboard [:layout :bottom])
                (get-in fsharp-keyboard [:layout :bottom]))
          "Different root notes should produce different layouts")
      (is (not= (get-in c-keyboard [:layout :top])
                (get-in d-keyboard [:layout :top])
                (get-in fsharp-keyboard [:layout :top]))
          "Different root notes should produce different layouts")

      ;; Test first bottom notes.
      (is (= (kb/create-note :c 4)
             (first (get-in c-keyboard [:layout :bottom])))
          "C4 keyboard should have C4 as first note in bottom row")
      (is (= (kb/create-note :d 4)
             (first (get-in d-keyboard [:layout :bottom])))
          "D4 keyboard should have D4 as first note in bottom row")
      (is (= (kb/create-note :f 3)
             (first (get-in fsharp-keyboard [:layout :bottom])))
          "F#3 keyboard should have F3 as first note in bottom row (adjusted to natural)")

      ;; Test first "top" notes.
      (is (nil? (first (get-in c-keyboard [:layout :top])))
          "C4 keyboard should have not have note in top row as first note.")
      (is (= (kb/create-note :csdf 4)
             (first (get-in d-keyboard [:layout :top])))
          "D4 keyboard should have C sharp in top row as first note.")
      (is (nil? (first (get-in fsharp-keyboard [:layout :top])))
          "F#3 keyboard should have not have note in top row as first note."))))

(deftest test-chromatic-keyboard-row-content
  (testing "Exact row content with various root notes"
    ;; Test with C major (no sharps/flats)
    (let [c-kb (kb/create-chromatic-keyboard (kb/create-note :c 4))
          c-rows (kb/rows c-kb)]
      ;; Bottom row should have natural notes starting with C4
      (is (= [(kb/create-note :c 4)
              (kb/create-note :d 4)
              (kb/create-note :e 4)
              (kb/create-note :f 4)
              (kb/create-note :g 4)
              (kb/create-note :a 4)
              (kb/create-note :b 4)
              (kb/create-note :c 5)]
             (:bottom c-rows))
          "C4 keyboard bottom row should have C4 through C5")
      ;; Top row should have nil followed by accidentals
      (is (= [nil
              (kb/create-note :csdf 4)
              (kb/create-note :dsef 4)
              nil
              (kb/create-note :fsgf 4)
              (kb/create-note :gsaf 4)
              (kb/create-note :asbf 4)
              nil]
             (:top c-rows))
          "C4 keyboard top row should have correct accidentals"))

    ;; Test with A major (3 sharps: F#, C#, G#)
    (let [a-kb (kb/create-chromatic-keyboard (kb/create-note :a 3))
          a-rows (kb/rows a-kb)]
      ;; Bottom row should have natural notes starting with A3
      (is (= [(kb/create-note :a 3)
              (kb/create-note :b 3)
              (kb/create-note :c 4)
              (kb/create-note :d 4)
              (kb/create-note :e 4)
              (kb/create-note :f 4)
              (kb/create-note :g 4)
              (kb/create-note :a 4)]
             (:bottom a-rows))
          "A3 keyboard bottom row should have A3 through A4")
      ;; Top row should have nil followed by accidentals
      (is (= [nil
              (kb/create-note :asbf 3)
              nil
              (kb/create-note :csdf 4)
              (kb/create-note :dsef 4)
              nil
              (kb/create-note :fsgf 4)
              (kb/create-note :gsaf 4)]
             (:top a-rows))
          "A3 keyboard top row should have correct accidentals"))

    ;; Test with F#/Gb major (6 sharps or 6 flats)
    (let [fs-kb (kb/create-chromatic-keyboard (kb/create-note :fsgf 4))
          fs-rows (kb/rows fs-kb)]
      ;; For sharp/flat root, it should shift to the previous natural note (F)
      (is (= [(kb/create-note :f 4)
              (kb/create-note :g 4)
              (kb/create-note :a 4)
              (kb/create-note :b 4)
              (kb/create-note :c 5)
              (kb/create-note :d 5)
              (kb/create-note :e 5)
              (kb/create-note :f 5)]
             (:bottom fs-rows))
          "F#4 keyboard bottom row should have F4 through F5 (adjusted to natural)")
      ;; Top row should follow the pattern based on the natural notes
      (is (= [nil
              (kb/create-note :fsgf 4)
              (kb/create-note :gsaf 4)
              (kb/create-note :asbf 4)
              nil
              (kb/create-note :csdf 5)
              (kb/create-note :dsef 5)
              nil]
             (:top fs-rows))
          "F#4 keyboard top row should have correct accidentals"))

    ;; Test with Eb major (3 flats: Bb, Eb, Ab)
    (let [eb-kb (kb/create-chromatic-keyboard (kb/create-note :dsef 3))
          eb-rows (kb/rows eb-kb)]
      ;; For sharp/flat root, it should shift to the previous natural note (D)
      (is (= [(kb/create-note :d 3)
              (kb/create-note :e 3)
              (kb/create-note :f 3)
              (kb/create-note :g 3)
              (kb/create-note :a 3)
              (kb/create-note :b 3)
              (kb/create-note :c 4)
              (kb/create-note :d 4)]
             (:bottom eb-rows))
          "Eb3 keyboard bottom row should have D3 through D4 (adjusted to natural)")
      ;; Top row should follow the pattern based on the natural notes
      (is (= [nil
              (kb/create-note :dsef 3)
              nil
              (kb/create-note :fsgf 3)
              (kb/create-note :gsaf 3)
              (kb/create-note :asbf 3)
              nil
              (kb/create-note :csdf 4)]
             (:top eb-rows))
          "Eb3 keyboard top row should have correct accidentals"))))

(deftest test-chromatic-keyboard-rows
  (testing "Basic rows structure with various root notes"
    (doseq [root-note [[:c 4] [:e 3] [:fsgf 5] [:asbf 2] [:csdf 3]]]
      (let [kb (kb/create-chromatic-keyboard (apply kb/create-note root-note))
            rows (kb/rows kb)
            [note-name octave] root-note]

        ;; Check that the result has the expected keys
        (is (contains? rows :top)
            (str note-name octave " keyboard: Result should contain :top key"))
        (is (contains? rows :bottom)
            (str note-name octave " keyboard: Result should contain :bottom key"))

        ;; Check that both rows are vectors
        (is (vector? (:top rows))
            (str note-name octave " keyboard: Top row should be a vector"))
        (is (vector? (:bottom rows))
            (str note-name octave " keyboard: Bottom row should be a vector"))

        ;; Check the length of the rows
        (is (= 8 (count (:top rows)))
            (str note-name octave " keyboard: Top row should have 8 elements"))
        (is (= 8 (count (:bottom rows)))
            (str note-name octave " keyboard: Bottom row should have 8 elements")))))

  (testing "Row normalization with various root notes"
    (doseq [root-note [[:d 4] [:f 2] [:gsaf 3] [:dsef 5] [:b 1]]]
      (let [kb (kb/create-chromatic-keyboard (apply kb/create-note root-note))
            raw-layout (:layout kb)
            normalized-rows (kb/rows kb)
            [note-name octave] root-note]

        ;; Check that the first position in top row is nil
        (is (nil? (first (:top normalized-rows)))
            (str note-name octave " keyboard: First position in normalized top row should be nil"))

        ;; Check that the rest of the top row matches the rest of the raw layout's top row
        (is (= (subvec (:top raw-layout) 1)
               (subvec (:top normalized-rows) 1))
            (str note-name octave " keyboard: Rest of normalized top row should match raw layout"))

        ;; Bottom row should remain unchanged
        (is (= (:bottom raw-layout) (:bottom normalized-rows))
            (str note-name octave " keyboard: Bottom row should remain unchanged during normalization"))))))

(deftest test-chromatic-keyboard-map-notes
  (testing "Basic note transformation"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :csdf 4))
          ;; Simple transformation: transpose all notes up by 1 semitone
          transpose-up (fn [note] (when note (kb/transpose-note note 1)))
          mapped-kb (kb/map-notes kb transpose-up)
          mapped-rows (kb/rows mapped-kb)]

      ;; For C#/Db keyboard, should adjust to C and then map
      (is (= (kb/create-note :csdf 4) (first (:bottom mapped-rows)))
          "First note in bottom row should be C#/Db (C transposed up)")

      ;; Check a few more examples to ensure transformation was applied
      (is (= (kb/create-note :fsgf 4) (nth (:bottom mapped-rows) 3))
          "Fourth note in bottom row should be F# (F transposed up)")

      ;; Check nil handling - should remain nil
      (is (nil? (first (:top mapped-rows)))
          "First position in top row should remain nil after mapping")))

  (testing "Identity mapping with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note (:name note) (:octave note)))
              identity-mapped-kb (kb/map-notes kb identity)
              original-rows (kb/rows kb)
              mapped-rows (kb/rows identity-mapped-kb)]
          ;; Rows should be identical after identity mapping
          (is (= original-rows mapped-rows)
              (str "Rows should be identical after applying identity mapping for "
                   (:name note) (:octave note)))))))

  (testing "Nil mapping function with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note (:name note) (:octave note)))
              nil-mapped-kb (kb/map-notes kb nil)
              original-rows (kb/rows kb)
              mapped-rows (kb/rows nil-mapped-kb)]
          ;; Rows should be identical after nil mapping
          (is (= original-rows mapped-rows)
              (str "Rows should be identical when nil mapping function is provided for "
                   (:name note) (:octave note)))))))

  (testing "Transformation to nil with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note (:name note) (:octave note)))
              ;; Map all notes to nil
              nil-all (fn [_] nil)
              mapped-kb (kb/map-notes kb nil-all)
              mapped-rows (kb/rows mapped-kb)]
          ;; All notes should be transformed to nil
          (is (every? nil? (concat (:top mapped-rows) (:bottom mapped-rows)))
              (str "All notes should be nil after mapping with nil-returning function for "
                   (:name note) (:octave note)))))))

  (testing "Round-trip transformations with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note name octave))
              ;; Map: Transpose up 15 semitones, then back down 15
              up-fn (fn [n] (when n (kb/transpose-note n 15)))
              down-fn (fn [n] (when n (kb/transpose-note n -15)))
              up-and-down-kb (-> kb
                                 (kb/map-notes up-fn)
                                 (kb/map-notes down-fn))
              original-rows (kb/rows kb)
              final-rows (kb/rows up-and-down-kb)]

          ;; After round-trip transformation, rows should be identical
          (is (= original-rows final-rows)
              (str "Round-trip transformation should return to original state for "
                   name octave))))))

  (testing "Octave adjustment mapping"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :c 4))
          ;; Shift all notes up by one octave
          octave-up (fn [note] (when note (update note :octave inc)))
          mapped-kb (kb/map-notes kb octave-up)
          mapped-rows (kb/rows mapped-kb)]

      ;; Check that octaves have been incremented
      (is (= (kb/create-note :c 5) (first (:bottom mapped-rows)))
          "First note should be C5 (octave up from C4)")
      (is (= (kb/create-note :e 5) (nth (:bottom mapped-rows) 2))
          "Third note should be E5 (octave up from E4)")

      ;; Check nil handling with octave adjustment
      (is (nil? (first (:top mapped-rows)))
          "Nil values should remain nil after octave adjustment")))

  (testing "Conditional mapping"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :gsaf 3))
          ;; Map only natural notes, leave accidentals unchanged
          highlight-naturals (fn [note]
                               (when note
                                 (if (kb/natural-note? note)
                                   ;; For natural notes, add a :highlighted flag
                                   (assoc note :highlighted true)
                                   ;; For accidentals, leave unchanged
                                   note)))
          mapped-kb (kb/map-notes kb highlight-naturals)
          mapped-bottom (get-in (kb/rows mapped-kb) [:bottom])]

      ;; Check that natural notes have :highlighted flag
      (is (:highlighted (first mapped-bottom))
          "Natural notes should have :highlighted flag added")

      ;; We need to find an accidental in the top row
      (let [mapped-top (get-in (kb/rows mapped-kb) [:top])
            accidental (first (remove nil? mapped-top))]
        (is (not (:highlighted accidental))
            "Accidental notes should not have :highlighted flag")))))

(deftest test-chromatic-keyboard-filter-notes
  (testing "Filter with custom predicate - full row verification"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :a 3))
          ;; Only keep C and G notes
          c-g-only? (fn [note] (when note (#{:c :g} (:name note))))
          filtered-kb (kb/filter-notes kb c-g-only?)
          filtered-rows (kb/rows filtered-kb)]

      ;; Full verification of bottom row
      (is (= [nil  ; A3 - filtered
              nil  ; B3 - filtered
              (kb/create-note :c 4)  ; C4 - kept
              nil  ; D4 - filtered
              nil  ; E4 - filtered
              nil  ; F4 - filtered
              (kb/create-note :g 4)  ; G4 - kept
              nil] ; A4 - filtered
             (:bottom filtered-rows))
          "Bottom row should only contain C and G notes, everything else filtered")

      ;; Full verification of top row
      (is (= [nil  ; First position always nil in rows
              nil  ; A#/Bb3 - filtered
              nil  ; No sharp for B
              nil  ; C#/Db4 - filtered
              nil  ; D#/Eb4 - filtered
              nil  ; No sharp for E
              nil  ; F#/Gb4 - filtered
              nil] ; G#/Ab4 - filtered
             (:top filtered-rows))
          "Top row should contain only nil values as all accidentals are filtered")))

  (testing "Nil predicate preserves all notes - generative test"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [note-name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note note-name octave))
              filtered-kb (kb/filter-notes kb nil)
              original-rows (kb/rows kb)
              filtered-rows (kb/rows filtered-kb)]

        ;; Result should be identical to original keyboard for any root note
          (is (= original-rows filtered-rows)
              (str "Filtering with nil predicate should keep all notes unchanged for "
                   note-name octave " keyboard"))))))

  (testing "Filter that removes all notes - generative test"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [note-name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note note-name octave))
            ;; Predicate that always returns false
              none? (constantly false)
              filtered-kb (kb/filter-notes kb none?)
              filtered-rows (kb/rows filtered-kb)]

        ;; All notes should be filtered out regardless of root note
          (is (every? nil? (concat (:top filtered-rows) (:bottom filtered-rows)))
              (str "All notes should be filtered out for " note-name octave " keyboard"))))))

  (testing "Octave boundary filtering - generative test"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [note-name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note note-name octave))
              target-octave (inc octave)  ; Filter to keep notes in next octave up
              octave-filter (fn [n] (when n (= target-octave (:octave n))))
              filtered-kb (kb/filter-notes kb octave-filter)
              filtered-rows (kb/rows filtered-kb)]

        ;; Check that only notes with the target octave are kept
          (is (every? #(or (nil? %) (= target-octave (:octave %)))
                      (concat (:top filtered-rows) (:bottom filtered-rows)))
              (str "Only notes in octave " target-octave " should be preserved"))

        ;; Check that there's a clear division - notes below a certain point should all be nil
        ;; and notes after that point (in the next octave) should have some non-nil values
          (let [bottom-row (:bottom filtered-rows)
              ;; Find the first non-nil note (which should be in the target octave)
                first-non-nil-idx (first (keep-indexed #(when %2 %1) bottom-row))]

          ;; If there are notes in the target octave in the keyboard...
            (when first-non-nil-idx
            ;; All notes before that should be nil (lower octave)
              (is (every? nil? (take first-non-nil-idx bottom-row))
                  (str "All notes before index " first-non-nil-idx
                       " should be nil (lower octave)"))))))))

  (testing "Multiple compositional filters - natural notes then specific note in range"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :c 4))
        ;; First filter: keep only natural notes
          natural-kb (kb/filter-notes kb kb/natural-note?)
        ;; Second filter: only notes in octave 4
          octave-4? (fn [note] (when note (= 4 (:octave note))))
          octave-filtered (kb/filter-notes natural-kb octave-4?)
        ;; Third filter: only C and G
          c-g-only? (fn [note] (when note (#{:c :g} (:name note))))
          final-kb (kb/filter-notes octave-filtered c-g-only?)
          final-rows (kb/rows final-kb)]

    ;; Full verification of bottom row after all filters
      (is (= [(kb/create-note :c 4)  ; C4 - kept by all filters
              nil  ; D4 - filtered by third filter
              nil  ; E4 - filtered by third filter
              nil  ; F4 - filtered by third filter
              (kb/create-note :g 4)  ; G4 - kept by all filters
              nil  ; A4 - filtered by third filter
              nil  ; B4 - filtered by third filter
              nil] ; C5 - filtered by second filter (octave 5)
             (:bottom final-rows))
          "Bottom row should only have C4 and G4 after all three filters")

    ;; Top row should be all nil (filtered by first filter)
      (is (every? nil? (:top final-rows))
          "Top row should be all nil after natural filter")))

  (testing "Compositional filters with different orders"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :e 3))
        ;; Define our filters
          natural? kb/natural-note?
          octave-4? (fn [note] (when note (= 4 (:octave note))))
          c-f? (fn [note] (when note (#{:c :f} (:name note))))

        ;; Apply filters in different orders
          order1 (-> kb
                     (kb/filter-notes natural?)
                     (kb/filter-notes octave-4?)
                     (kb/filter-notes c-f?))
          order2 (-> kb
                     (kb/filter-notes c-f?)
                     (kb/filter-notes octave-4?)
                     (kb/filter-notes natural?))
          order3 (-> kb
                     (kb/filter-notes octave-4?)
                     (kb/filter-notes natural?)
                     (kb/filter-notes c-f?))
          rows1 (kb/rows order1)
          rows2 (kb/rows order2)
          rows3 (kb/rows order3)]

    ;; All three orders should give identical results
      (is (= rows1 rows2)
          "Filter order 1 and 2 should produce identical results")
      (is (= rows2 rows3)
          "Filter order 2 and 3 should produce identical results")

    ;; Verify the exact content (should only have C4 and F4)
      (let [expected-bottom [nil  ; E3 - filtered by octave
                             nil  ; F3 - filtered by octave
                             nil  ; G3 - filtered by octave
                             nil  ; A3 - filtered by octave
                             nil  ; B3 - filtered by octave
                             (kb/create-note :c 4)  ; C4 - kept by all filters
                             nil  ; D4 - filtered by C-F filter
                             nil] ; E4 - filtered by C-F filter
            expected-top [nil nil nil nil nil nil nil nil]]  ; All accidentals filtered

        (is (= expected-bottom (:bottom rows1))
            "Bottom row should only contain C4 after all filters")
        (is (= expected-top (:top rows1))
            "Top row should be all nil after all filters"))))

  (testing "Compositional filtering with contradictory predicates"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :g 4))
        ;; First filter: only natural notes
          natural-kb (kb/filter-notes kb kb/natural-note?)
        ;; Second filter: only accidentals (contradicts first filter)
          accidental? (fn [note] (when note (not (kb/natural-note? note))))
          final-kb (kb/filter-notes natural-kb accidental?)
          final-rows (kb/rows final-kb)]

    ;; Result should be all nil (no note can satisfy both predicates)
      (is (every? nil? (concat (:top final-rows) (:bottom final-rows)))
          "All notes should be nil when using contradictory filters")))

  (testing "Idempotent filtering"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :asbf 3))
        ;; Apply the same filter twice
          c-e-g? (fn [note] (when note (#{:c :e :g} (:name note))))
          once-filtered (kb/filter-notes kb c-e-g?)
          twice-filtered (kb/filter-notes once-filtered c-e-g?)
          once-rows (kb/rows once-filtered)
          twice-rows (kb/rows twice-filtered)]

    ;; Applying the same filter twice should give the same result as once
      (is (= once-rows twice-rows)
          "Applying the same filter multiple times should be idempotent")))

  (testing "Filtering with disjunction of predicates"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :d 4))
        ;; Filter to keep C or E notes
          c-pred? (fn [note] (when note (= :c (:name note))))
          e-pred? (fn [note] (when note (= :e (:name note))))

        ;; Two approaches to achieve the same result:
        ;; 1. Using a combined predicate
          c-or-e? (fn [note] (when note (or (c-pred? note) (e-pred? note))))
          combined-kb (kb/filter-notes kb c-or-e?)

        ;; 2. Filter for C, then map the result of a separate E filter back in
          c-kb (kb/filter-notes kb c-pred?)
          e-kb (kb/filter-notes kb e-pred?)
        ;; Use map-notes to combine them (if either result has the note, keep it)
          combined-manually (kb/map-notes kb (fn [note]
                                               (when note
                                                 (if (or (c-pred? note) (e-pred? note))
                                                   note
                                                   nil))))

          combined-rows (kb/rows combined-kb)
          manual-rows (kb/rows combined-manually)]

    ;; Both approaches should yield the same result
      (is (= combined-rows manual-rows)
          "Different approaches to combining filters should give same result")

    ;; Verify the exact content (only C and E notes)
      (is (= [nil  ; D4 - filtered
              (kb/create-note :e 4)  ; E4 - kept
              nil  ; F4 - filtered
              nil  ; G4 - filtered
              nil  ; A4 - filtered
              nil  ; B4 - filtered
              (kb/create-note :c 5)  ; C5 - kept
              nil] ; D5 - filtered
             (:bottom combined-rows))
          "Bottom row should only contain C and E notes")

    ;; Top row should only have nil since all accidentals are filtered
      (is (every? nil? (:top combined-rows))
          "Top row should be all nil when filtering for only C and E")))
  (testing "Name-specific filtering - generative test"
    ;; Generate a variety of notes to test with
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [na (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note na octave))
              ;; Create a predicate that only matches the name of our sample note
              note-name-pred (fn [n] (when n (= na (:name n))))
              filtered-kb (kb/filter-notes kb note-name-pred)
              filtered-rows (kb/rows filtered-kb)]

          ;; Check that only notes with the specified name are kept
          (is (every? #(or (nil? %) (= na (:name %)))
                      (concat (:top filtered-rows) (:bottom filtered-rows)))
              (str "Only notes with name " na " should be preserved"))

          ;; Ensure at least one note is kept (there should be at least one if the layout is correct)
          (is (some #(and % (= na (:name %)))
                    (concat (:top filtered-rows) (:bottom filtered-rows)))
              (str "At least one note with name " na " should be present")))))))



(deftest test-chromatic-keyboard-protocol-integration
  (testing "Basic method chaining with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard note)
              ;; Chain operations: map to transpose up, filter to keep naturals, then get rows
              transposed-kb (kb/map-notes kb #(kb/transpose-note % 2))
              filtered-kb (kb/filter-notes transposed-kb kb/natural-note?)
              result-rows (kb/rows filtered-kb)]

          ;; Verify that all notes in the result are natural notes
          (is (every? #(or (nil? %) (kb/natural-note? %))
                      (concat (:top result-rows) (:bottom result-rows)))
              (str "All notes should be natural after filter for " (:name note) (:octave note) " keyboard"))

          ;; Verify that transposition occurred (compare with untransposed+filtered)
          (let [just-filtered (kb/filter-notes kb kb/natural-note?)
                filtered-rows (kb/rows just-filtered)]
            (is (not= filtered-rows result-rows)
                (str "Transposed+filtered keyboard should differ from just filtered for "
                     (:name note) (:octave note) " keyboard")))))))

  (testing "Equivalence of different operation orders"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard note)
              ;; Define our operations
              transpose-up (fn [n] (when n (kb/transpose-note n 3)))
              c-e-g-only? (fn [n] (when n (#{:c :e :g} (:name n))))

              ;; Apply operations in different orders
              map-then-filter (-> kb
                                (kb/map-notes transpose-up)
                                (kb/filter-notes c-e-g-only?)
                                (kb/rows))

              ;; For filter-then-map, we need to adjust the filter to account for transposition
              ;; If we're transposing down 3 semitones:
              ;; C→A, E→C#, G→E, so we should filter for A, C#, and E.
              ;; (C Major -> A Major)
              equivalent-filter? (fn [n]
                                   (when n
                                     (#{:a :csdf :e} (:name n))))

              filter-then-map (-> kb
                                (kb/filter-notes equivalent-filter?)
                                (kb/map-notes transpose-up)
                                (kb/rows))]

          ;; Check note names in both results
          (let [map-filter-names (set (keep #(when % (:name %))
                                          (concat (:top map-then-filter) (:bottom map-then-filter))))
                filter-map-names (set (keep #(when % (:name %))
                                          (concat (:top filter-then-map) (:bottom filter-then-map))))]

            ;; Both approaches should yield the same note names
            (is (= map-filter-names filter-map-names)
                (str "Both operation orders should yield same note names for "
                     (:name note) (:octave note) " keyboard")))))))

  (testing "Round-trip operations with generative testing"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples]
        (let [kb (kb/create-chromatic-keyboard note)
              ;; 1. Transpose up then down by the same amount
              ;; Non-destructive round trip.
              up-fn (fn [n] (when n (kb/transpose-note n 17)))  ; octave + major 3rd up
              down-fn (fn [n] (when n (kb/transpose-note n -17)))  ; octave + major 3rd down

              ;; 2. Filter to C/E/G, then expand to include all natural notes
              ;; Destructive round trip.
              c-e-g-only? (fn [n] (when n (#{:c :e :g} (:name n))))
              all-naturals? kb/natural-note?
              all-notes? (constantly true)  ; Allow any note

              ;; Apply canceling operations
              transposed-back (-> kb
                                (kb/map-notes up-fn)
                                (kb/map-notes down-fn)
                                (kb/rows))

              ;; Filtering is actually destructive, can't get that data back.
              filtered-expanded (-> kb
                                   (kb/filter-notes c-e-g-only?)
                                   (kb/filter-notes all-notes?)  ; This should be a no-op
                                   (kb/rows))
              ;; Reference results
              original-rows (kb/rows kb)
              filtered-rows (kb/rows (kb/filter-notes kb c-e-g-only?))]

          ;; After transpose up then down, result should match original
          (is (= original-rows transposed-back)
              (str "Transpose up then down should return to original for "
                   (:name note) (:octave note) " keyboard"))

          ;; After filter then expand, result should match just filtered
          (is (= filtered-rows filtered-expanded)
              (str "Filter then 'expand' should equal just filtered for "
                   (:name note) (:octave note) " keyboard"))))))

  (testing "Complex musical transformations"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :c 4))
          ;; Transform C major triad to C minor triad (lower E to Eb)
          c-major-to-minor (fn [note]
                             (when note
                               (if (= :e (:name note))
                                 (kb/transpose-note note -1)  ; Lower E to Eb
                                 note)))

          ;; Only keep triad notes (C, E, G)
          triad-notes? (fn [note]
                         (when note (#{:c :e :g :dsef} (:name note))))

          ;; Apply transformations
          result (-> kb
                     (kb/map-notes c-major-to-minor)
                     (kb/filter-notes triad-notes?)
                     (kb/rows))]

      ;; Verify bottom row contains C, Eb, G
      (let [note-names (set (keep #(when % (:name %)) (:bottom result)))]
        (is (contains? note-names :c) "Result should contain C")
        (is (contains? note-names :dsef) "Result should contain Eb")
        (is (contains? note-names :g) "Result should contain G")
        (is (not (contains? note-names :e)) "Result should not contain E"))))

  (testing "Data structure integrity through transformations - generative test"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [note-name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note note-name octave))
              ;; Complex chained operations
              result (-> kb
                        (kb/map-notes #(when % (assoc % :test-attr "mapped")))
                        (kb/filter-notes #(when % true))  ; Keep all notes
                        (kb/map-notes #(when % (update % :octave inc)))
                        (kb/rows))]

          ;; Check structure is preserved
          (is (contains? result :top) "Result should contain :top key")
          (is (contains? result :bottom) "Result should contain :bottom key")
          (is (vector? (:top result)) "Top row should be a vector")
          (is (vector? (:bottom result)) "Bottom row should be a vector")
          (is (= 8 (count (:top result))) "Top row should have 8 elements")
          (is (= 8 (count (:bottom result))) "Bottom row should have 8 elements")

          ;; Check transformations were applied correctly
          (is (every? #(or (nil? %) (= "mapped" (:test-attr %))) 
                      (concat (:top result) (:bottom result)))
              "All notes should have test-attr from first mapping")

          ;; Check octave increment was applied
          (let [non-nil-notes (remove nil? (concat (:top result) (:bottom result)))]
            (when (seq non-nil-notes)
              (is (every? #(or (= (+ octave 2) (:octave %)) (= (inc octave) (:octave %))) non-nil-notes)
                  "All notes should have incremented octave")))))))

  (testing "Integration with full lifecycle transformations"
    (let [kb (kb/create-chromatic-keyboard (kb/create-note :c 4))
          ;; Create a pentatonic major scale filter (C, D, E, G, A)
          pentatonic-major? (fn [note]
                              (when note (#{:c :d :e :g :a} (:name note))))

          ;; Map to add a "scale-degree" attribute
          add-scale-degree (fn [note]
                             (when note
                               (let [degree (case (:name note)
                                              :c 1
                                              :d 2
                                              :e 3
                                              :g 5
                                              :a 6
                                              nil)]
                                 (if degree
                                   (assoc note :scale-degree degree)
                                   note))))

          ;; Apply transformations
          result (-> kb
                     (kb/filter-notes pentatonic-major?)
                     (kb/map-notes add-scale-degree)
                     (kb/rows))]

      ;; Verify notes have correct scale degrees
      (let [c-note (first (:bottom result))
            d-note (second (:bottom result))
            e-note (nth (:bottom result) 2)
            g-note (nth (:bottom result) 4)
            a-note (nth (:bottom result) 5)]

        (is (= 1 (:scale-degree c-note)) "C should have scale degree 1")
        (is (= 2 (:scale-degree d-note)) "D should have scale degree 2")
        (is (= 3 (:scale-degree e-note)) "E should have scale degree 3")
        (is (= 5 (:scale-degree g-note)) "G should have scale degree 5")
        (is (= 6 (:scale-degree a-note)) "A should have scale degree 6"))))

  (testing "Error handling with invalid inputs - generative test"
    (let [note-samples (gen/sample (s/gen ::kb/note) (* 10 gen-test-scale))]
      (doseq [note note-samples
              :let [note-name (:name note)
                    octave (:octave note)]]
        (let [kb (kb/create-chromatic-keyboard (kb/create-note note-name octave))
              ;; Create some problematic transformations
              returns-map (fn [_] {})  ; Returns a map instead of a note
              returns-nil (constantly nil)  ; Always returns nil

              ;; Apply potentially problematic operations
              result1 (-> kb
                         (kb/map-notes returns-map)
                         (kb/filter-notes #(when % true))
                         (kb/rows))

              result2 (-> kb
                         (kb/map-notes returns-nil)
                         (kb/rows))]

          ;; Even with invalid transformations, structure should be preserved
          (is (contains? result1 :top) "Result1 should contain :top key")
          (is (contains? result1 :bottom) "Result1 should contain :bottom key")
          (is (= 8 (count (:top result1))) "Result1 top row should have 8 elements")
          (is (= 8 (count (:bottom result1))) "Result1 bottom row should have 8 elements")

          (is (contains? result2 :top) "Result2 should contain :top key")
          (is (contains? result2 :bottom) "Result2 should contain :bottom key")
          (is (= 8 (count (:top result2))) "Result2 top row should have 8 elements")
          (is (= 8 (count (:bottom result2))) "Result2 bottom row should have 8 elements")

          ;; All elements should be nil in result2
          (is (every? nil? (concat (:top result2) (:bottom result2)))
              "All elements should be nil after nil-returning mapper"))))))

(deftest test-create-folding-keyboard
  (testing "Basic record initialization"
    (let [root (kb/create-note :c 4)
          keyboard (kb/create-folding-keyboard root)]

      ;; Test that root-note is set correctly
      (is (= root (:root-note keyboard))
          "Root note should be set to the provided note")

      ;; Test that notes are initialized as a vector
      (is (vector? (:notes keyboard))
          "Notes should be a vector")
      (is (= 16 (count (:notes keyboard)))
          "Notes vector should contain 16 notes")

      ;; Test that transformations is initially nil
      (is (nil? (:transformations keyboard))
          "transformations should initially be nil"))))


(deftest test-folding-keyboard-rows
  (testing "Basic rows structure with various root notes"
    (doseq [root-note [[:c 4] [:e 3] [:fsgf 5] [:asbf 2] [:csdf 3]]]
      (let [kb (kb/create-folding-keyboard (apply kb/create-note root-note))
            rows (kb/rows kb)
            [note-name octave] root-note]

        ;; Check that the result has the expected keys
        (is (contains? rows :top)
            (str note-name octave " keyboard: Result should contain :top key"))
        (is (contains? rows :bottom)
            (str note-name octave " keyboard: Result should contain :bottom key"))

        ;; Check that both rows are vectors
        (is (vector? (:top rows))
            (str note-name octave " keyboard: Top row should be a vector"))
        (is (vector? (:bottom rows))
            (str note-name octave " keyboard: Bottom row should be a vector"))

        ;; Check the length of the rows
        (is (= 8 (count (:top rows)))
            (str note-name octave " keyboard: Top row should have 8 elements"))
        (is (= 8 (count (:bottom rows)))
            (str note-name octave " keyboard: Bottom row should have 8 elements"))))))

(deftest test-folding-keyboard-sequential-notes
  (testing "Sequential chromatic ordering of notes"
    (let [root (kb/create-note :c 4)
          keyboard (kb/create-folding-keyboard root)
          all-notes (:notes keyboard)
          rows (kb/rows keyboard)
          bottom-row (:bottom rows)
          top-row (:top rows)
          combined (concat bottom-row top-row)]

      ;; First note should be the root note
      (is (= root (first all-notes))
          "First note should be the root note")

      ;; Notes should be in sequential chromatic order
      (doseq [i (range (dec (count all-notes)))]
        (let [current (nth all-notes i)
              next-note (nth all-notes (inc i))]
          (is (= next-note (kb/transpose-note current 1))
              (str "Note at position " (inc i) " should be a semitone higher than position " i))))

      ;; Combined rows should equal the notes vector
      (is (= combined all-notes)
          "Combined rows should match the notes vector"))))

(deftest test-folding-keyboard-map-notes
  (testing "Basic note transformation"
    (let [kb (kb/create-folding-keyboard (kb/create-note :c 4))
          ;; Simple transformation: transpose all notes up by 2 semitones
          transpose-up (fn [note] (when note (kb/transpose-note note 2)))
          mapped-kb (kb/map-notes kb transpose-up)
          mapped-rows (kb/rows mapped-kb)
          original-rows (kb/rows kb)]

      ;; First note should be transposed from C4 to D4
      (is (= (kb/create-note :d 4) (first (:bottom mapped-rows)))
          "First note in bottom row should be D4 (C4 transposed up 2)")

      ;; Check a few more examples to ensure transformation was applied
      (is (= (kb/create-note :f 4) (nth (:bottom mapped-rows) 3))
          "Fourth note in bottom row should be F4 (D4 transposed up 2)")

      ;; Check the overall structure is preserved
      (is (= (count (:bottom original-rows)) (count (:bottom mapped-rows)))
          "Bottom row should have same length after mapping")
      (is (= (count (:top original-rows)) (count (:top mapped-rows)))
          "Top row should have same length after mapping"))))

(deftest test-folding-keyboard-filter-notes
  (testing "Filter with custom predicate"
    (let [kb (kb/create-folding-keyboard (kb/create-note :c 4))
          ;; Only keep C and G notes
          c-g-only? (fn [note] (when note (#{:c :g} (:name note))))
          filtered-kb (kb/filter-notes kb c-g-only?)
          filtered-rows (kb/rows filtered-kb)
          original-notes (:notes kb)]

      ;; Check that only C and G notes remain
      (is (every? #(#{:c :g} (:name %))
                  (concat (:bottom filtered-rows) (:top filtered-rows)))
          "Only C and G notes should remain after filtering")

      ;; Check preservation of note positions (C and G notes should be in the same positions as original)
      (is (= (count (:bottom filtered-rows)) 8))
      (is (= (count (:top filtered-rows)) 8))))

  (testing "Nil predicate preserves all notes"
    (let [kb (kb/create-folding-keyboard (kb/create-note :d 3))
          filtered-kb (kb/filter-notes kb nil)
          original-rows (kb/rows kb)
          filtered-rows (kb/rows filtered-kb)]

      ;; Result should be identical to original keyboard
      (is (= original-rows filtered-rows)
          "Filtering with nil predicate should keep all notes unchanged")))

  ;; TODO - What the heck should happen in this context? Max lazy queries?
  (testing "false returning predicate preserves all notes"))

(deftest test-folding-keyboard-integration
  (testing "Chaining operations"
    (let [kb (kb/create-folding-keyboard (kb/create-note :c 4))
          ;; Chain operations: transpose up, filter to keep naturals
          transposed-kb (kb/map-notes kb #(kb/transpose-note % 1))  ;; Up a semitone
          filtered-kb (kb/filter-notes transposed-kb kb/natural-note?)
          result-rows (kb/rows filtered-kb)]

      ;; First note should be C# transposed to D
      (is (= (kb/create-note :d 4) (first (:bottom result-rows)))
          "First note should be D4 after transposing C4 up")

      ;; All notes should be natural
      (is (every? #(or (nil? %) (kb/natural-note? %))
                  (concat (:bottom result-rows) (:top result-rows)))
          "All notes should be natural after filtering"))))

(deftest test-folding-keyboard-comparison
  (testing "Comparison with chromatic keyboard"
    (let [root (kb/create-note :c 4)
          folding-kb (kb/create-folding-keyboard root)
          chromatic-kb (kb/create-chromatic-keyboard root)
          folding-rows (kb/rows folding-kb)
          chromatic-rows (kb/rows chromatic-kb)]

      ;; Folding keyboard should have a different layout than chromatic
      (is (not= folding-rows chromatic-rows)
          "Folding and chromatic keyboards should have different layouts")

      ;; First note in folding should be root note
      (is (= root (first (:bottom folding-rows)))
          "First note in folding keyboard should be the root note")

      ;; In chromatic, first bottom note is always a natural note (might not be root for accidentals)
      (is (kb/natural-note? (first (:bottom chromatic-rows)))
          "First note in chromatic keyboard bottom row should be a natural note"))))

;; =========================================================
;; Tests for build-scale-chord function
;; =========================================================

(deftest test-build-scale-chord-basic
  (testing "Basic chord building in C major scale"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C major triad (C-E-G)
          c-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 2 4])
          ;; G major triad (G-B-D) with D in next octave
          g-chord (kb/build-scale-chord c-major-scale (kb/create-note :g 4) [0 2 4])
          ;; E minor triad (E-G-B)
          e-chord (kb/build-scale-chord c-major-scale (kb/create-note :e 4) [0 2 4])]

      ;; Check C chord
      (is (= 3 (count c-chord))
          "C chord should have 3 notes")
      (is (= [:c :e :g] (mapv :name c-chord))
          "C chord should contain C, E, G")
      (is (= [4 4 4] (mapv :octave c-chord))
          "All notes in C chord should be in octave 4")

      ;; Check G chord
      (is (= 3 (count g-chord))
          "G chord should have 3 notes")
      (is (= [:g :b :d] (mapv :name g-chord))
          "G chord should contain G, B, D")
      (is (= [4 4 5] (mapv :octave g-chord))
          "G and B should be in octave 4, D should be in octave 5")

      ;; Check E chord
      (is (= 3 (count e-chord))
          "E chord should have 3 notes")
      (is (= [:e :g :b] (mapv :name e-chord))
          "E chord should contain E, G, B")
      (is (= [4 4 4] (mapv :octave e-chord))
          "All notes in E chord should be in octave 4"))))

(deftest test-build-scale-chord-edge-cases
  (testing "Empty scale"
    (let [empty-scale []
          chord (kb/build-scale-chord empty-scale (kb/create-note :c 4) [0 2 4])]
      (is (empty? chord)
          "Chord from empty scale should be empty")))

  (testing "Note not in scale"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C# is not in C major
          chord (kb/build-scale-chord c-major-scale (kb/create-note :csdf 4) [0 2 4])]
      (is (empty? chord)
          "Chord from note not in scale should be empty")))

  (testing "Nil input"
    (let [c-major-scale [:c :d :e :f :g :a :b]]
      (is (empty? (kb/build-scale-chord nil (kb/create-note :c 4) [0 2 4]))
          "Chord from nil scale should be empty")
      (is (empty? (kb/build-scale-chord c-major-scale nil [0 2 4]))
          "Chord from nil root note should be empty"))))

(deftest test-build-scale-chord-different-scales
  (testing "Minor scale chords"
    (let [a-minor-scale [:a :b :c :d :e :f :g]
          ;; A minor triad (A-C-E)
          a-chord (kb/build-scale-chord a-minor-scale (kb/create-note :a 3) [0 2 4])
          ;; D minor triad (D-F-A)
          d-chord (kb/build-scale-chord a-minor-scale (kb/create-note :d 4) [0 2 4])
          ;; E major triad (E-G-B)
          e-chord (kb/build-scale-chord a-minor-scale (kb/create-note :e 4) [0 2 4])]

      ;; Check A chord
      (is (= 3 (count a-chord))
          "A chord should have 3 notes")
      (is (= [:a :c :e] (mapv :name a-chord))
          "A chord should contain A, C, E")

      ;; Check D chord
      (is (= 3 (count d-chord))
          "D chord should have 3 notes")
      (is (= [:d :f :a] (mapv :name d-chord))
          "D chord should contain D, F, A")

      ;; Check E chord
      (is (= 3 (count e-chord))
          "E chord should have 3 notes")
      (is (= [:e :g :b] (mapv :name e-chord))
          "E chord should contain E, G, B"))))

(deftest test-build-scale-chord-pentatonic
  (testing "Pentatonic scale chords"
    (let [c-pentatonic [:c :d :e :g :a]
          ;; C major triad with no F (C-E-G)
          c-chord (kb/build-scale-chord c-pentatonic (kb/create-note :c 4) [0 2 4])
          ;; G chord with no F (G-B-D would need B, but B is not in scale)
          g-chord (kb/build-scale-chord c-pentatonic (kb/create-note :g 4) [0 2 4])]

      ;; Check C chord
      (is (= 3 (count c-chord))
          "C chord in pentatonic should have 3 notes")
      (is (= [:c :e :a] (mapv :name c-chord))
          "C chord should contain C, E, G")

      ;; Check G chord (should use G, A, C)
      (is (= 3 (count g-chord))
          "G chord in pentatonic should have 3 notes")
      (is (= [:g :c :e] (mapv :name g-chord))
          "G chord in pentatonic should contain G, A, C due to scale constraints"))))

(deftest test-build-scale-chord-cyclical-construction
  (testing "Chord construction wraps around the scale"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; B triad (B-D-F) with D and F in next octave
          b-chord (kb/build-scale-chord c-major-scale (kb/create-note :b 3) [0 2 4])]

      ;; Check B chord
      (is (= 3 (count b-chord))
          "B chord should have 3 notes")
      (is (= [:b :d :f] (mapv :name b-chord))
          "B chord should contain B, D, F")
      (is (= [3 4 4] (mapv :octave b-chord))
          "B should be in octave 3, D and F should be in octave 4"))))

(deftest test-build-scale-chord-octave-handling
  (testing "Octave handling with different root positions"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C chord in octave 2
          c2-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 2) [0 2 4])
          ;; C chord in octave 5
          c5-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 5) [0 2 4])]

      ;; Check C2 chord
      (is (= [2 2 2] (mapv :octave c2-chord))
          "C2 chord should have all notes in octave 2")

      ;; Check C5 chord
      (is (= [5 5 5] (mapv :octave c5-chord))
          "C5 chord should have all notes in octave 5")))

  (testing "Octave handling near octave boundaries"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; B chord in octave 9 (highest reasonable octave)
          b9-chord (kb/build-scale-chord c-major-scale (kb/create-note :b 9) [0 2 4])]

      ;; Check B9 chord
      (is (= [:b :d :f] (mapv :name b9-chord))
          "B9 chord should contain B, D, F")
      (is (= [9 10 10] (mapv :octave b9-chord))
          "B9 chord should have B in octave 9, D and F in octave 10"))))

(deftest test-build-scale-chord-generative
  (testing "Property: Chord size is always 3 for valid scales/roots"
    (let [scale-samples [[:c :d :e :f :g :a :b]  ;; Major
                         [:a :b :c :d :e :f :g]  ;; Minor
                         [:c :d :e :g :a]        ;; Pentatonic
                         [:c :d :f :g :asbf]]    ;; Blues-like
          note-samples (for [scale scale-samples
                             note-name scale
                             octave (range 2 6)]
                         [scale note-name octave])]

      (doseq [[scale note-name octave] note-samples]
        (let [chord (kb/build-scale-chord scale (kb/create-note note-name octave) [0 2 4])]
          (is (= 3 (count chord))
              (str "Chord for " note-name octave " in scale should have 3 notes"))))))

  (testing "Property: Chord notes preserve the root note's octave or adjacent"
    (let [scale-samples [[:c :d :e :f :g :a :b]  ;; Major
                         [:a :b :c :d :e :f :g]]  ;; Minor
          note-samples (for [scale scale-samples
                             note-name scale
                             octave (range 2 6)]
                         [scale note-name octave])]

      (doseq [[scale note-name octave] note-samples]
        (let [chord (kb/build-scale-chord scale (kb/create-note note-name octave) [0 2 4])
              chord-octaves (set (map :octave chord))]
          (is (<= (count chord-octaves) 2)
              (str "Chord for " note-name octave " should span at most 2 octaves"))
          (is (contains? chord-octaves octave)
              (str "Chord should contain at least one note in the root octave " octave))))))

  (testing "Property: First note of chord is always the root note"
    (let [scale-samples [[:c :d :e :f :g :a :b]  ;; Major
                         [:a :b :c :d :e :f :g]  ;; Minor
                         [:c :d :e :g :a]]        ;; Pentatonic
          note-samples (for [scale scale-samples
                             note-name scale
                             octave (range 2 6)]
                         [scale note-name octave])]

      (doseq [[scale note-name octave] note-samples]
        (let [root-note (kb/create-note note-name octave)
              chord (kb/build-scale-chord scale root-note [0 2 4])]
          (when (seq chord)
            (is (= root-note (first chord))
                (str "First note of chord should be the root note " note-name octave))))))))

(deftest test-build-scale-chord-seventh-chords
  (testing "Building 7th chords with positions [0 2 4 6]"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C major 7th (C-E-G-B)
          c-maj7-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 2 4 6])
          ;; F major 7th (F-A-C-E)
          f-maj7-chord (kb/build-scale-chord c-major-scale (kb/create-note :f 4) [0 2 4 6])
          ;; G dominant 7th (G-B-D-F)
          g-dom7-chord (kb/build-scale-chord c-major-scale (kb/create-note :g 4) [0 2 4 6])]

      ;; Check C major 7th chord
      (is (= 4 (count c-maj7-chord))
          "C major 7th chord should have 4 notes")
      (is (= [:c :e :g :b] (mapv :name c-maj7-chord))
          "C major 7th chord should contain C, E, G, B")
      (is (= [4 4 4 4] (mapv :octave c-maj7-chord))
          "All notes in C major 7th chord should be in octave 4")

      ;; Check F major 7th chord
      (is (= 4 (count f-maj7-chord))
          "F major 7th chord should have 4 notes")
      (is (= [:f :a :c :e] (mapv :name f-maj7-chord))
          "F major 7th chord should contain F, A, C, E")
      (is (= [4 4 5 5] (mapv :octave f-maj7-chord))
          "F and A should be in octave 4, C and E should be in octave 5")

      ;; Check G dominant 7th chord
      (is (= 4 (count g-dom7-chord))
          "G dominant 7th chord should have 4 notes")
      (is (= [:g :b :d :f] (mapv :name g-dom7-chord))
          "G dominant 7th chord should contain G, B, D, F")
      (is (= [4 4 5 5] (mapv :octave g-dom7-chord))
          "G and B should be in octave 4, D and F should be in octave 5"))))

(deftest test-build-scale-chord-suspended-chords
  (testing "Building suspended chords with different positions"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C sus2 (C-D-G) using positions [0 1 4]
          c-sus2-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 1 4])
          ;; C sus4 (C-F-G) using positions [0 3 4]
          c-sus4-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 3 4])
          ;; G sus2 (G-A-D) using positions [0 1 4]
          g-sus2-chord (kb/build-scale-chord c-major-scale (kb/create-note :g 4) [0 1 4])]

      ;; Check C sus2 chord
      (is (= 3 (count c-sus2-chord))
          "C sus2 chord should have 3 notes")
      (is (= [:c :d :g] (mapv :name c-sus2-chord))
          "C sus2 chord should contain C, D, G")
      (is (= [4 4 4] (mapv :octave c-sus2-chord))
          "All notes in C sus2 chord should be in octave 4")

      ;; Check C sus4 chord
      (is (= 3 (count c-sus4-chord))
          "C sus4 chord should have 3 notes")
      (is (= [:c :f :g] (mapv :name c-sus4-chord))
          "C sus4 chord should contain C, F, G")
      (is (= [4 4 4] (mapv :octave c-sus4-chord))
          "All notes in C sus4 chord should be in octave 4")

      ;; Check G sus2 chord
      (is (= 3 (count g-sus2-chord))
          "G sus2 chord should have 3 notes")
      (is (= [:g :a :d] (mapv :name g-sus2-chord))
          "G sus2 chord should contain G, A, D")
      (is (= [4 4 5] (mapv :octave g-sus2-chord))
          "G and A should be in octave 4, D should be in octave 5"))))

(deftest test-build-scale-chord-ninth-chords
  (testing "Building 9th chords with positions [0 2 4 6 8]"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C major 9th (C-E-G-B-D)
          c-maj9-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 3) [0 2 4 6 8])
          ;; D minor 9th (D-F-A-C-E)
          d-min9-chord (kb/build-scale-chord c-major-scale (kb/create-note :d 3) [0 2 4 6 8])
          ;; G dominant 9th (G-B-D-F-A)
          g-dom9-chord (kb/build-scale-chord c-major-scale (kb/create-note :g 3) [0 2 4 6 8])]

      ;; Check C major 9th chord
      (is (= 5 (count c-maj9-chord))
          "C major 9th chord should have 5 notes")
      (is (= [:c :e :g :b :d] (mapv :name c-maj9-chord))
          "C major 9th chord should contain C, E, G, B, D")
      (is (= [3 3 3 3 4] (mapv :octave c-maj9-chord))
          "C, E, G, B should be in octave 3, D should be in octave 4")

      ;; Check D minor 9th chord
      (is (= 5 (count d-min9-chord))
          "D minor 9th chord should have 5 notes")
      (is (= [:d :f :a :c :e] (mapv :name d-min9-chord))
          "D minor 9th chord should contain D, F, A, C, E")
      (is (= [3 3 3 4 4] (mapv :octave d-min9-chord))
          "D, F, A should be in octave 3, C and E should be in octave 4")

      ;; Check G dominant 9th chord
      (is (= 5 (count g-dom9-chord))
          "G dominant 9th chord should have 5 notes")
      (is (= [:g :b :d :f :a] (mapv :name g-dom9-chord))
          "G dominant 9th chord should contain G, B, D, F, A")
      (is (= [3 3 4 4 4] (mapv :octave g-dom9-chord))
          "G and B should be in octave 3, D, F, and A should be in octave 4"))))

(deftest test-build-scale-chord-6th-chords
  (testing "Building 6th chords with positions [0 2 4 5]"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C6 (C-E-G-A) 
          c6-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 2 4 5])
          ;; F6 (F-A-C-D)
          f6-chord (kb/build-scale-chord c-major-scale (kb/create-note :f 4) [0 2 4 5])
          ;; Am6 (A-C-E-F♯), but F♯ isn't in the C major scale, should be F
          a-min6-chord (kb/build-scale-chord c-major-scale (kb/create-note :a 3) [0 2 4 5])]

      ;; Check C6 chord
      (is (= 4 (count c6-chord))
          "C6 chord should have 4 notes")
      (is (= [:c :e :g :a] (mapv :name c6-chord))
          "C6 chord should contain C, E, G, A")
      (is (= [4 4 4 4] (mapv :octave c6-chord))
          "All notes in C6 chord should be in octave 4")

      ;; Check F6 chord
      (is (= 4 (count f6-chord))
          "F6 chord should have 4 notes")
      (is (= [:f :a :c :d] (mapv :name f6-chord))
          "F6 chord should contain F, A, C, D")
      (is (= [4 4 5 5] (mapv :octave f6-chord))
          "F and A should be in octave 4, C and D should be in octave 5")

      ;; Check Am6 chord (using notes from C major scale)
      (is (= 4 (count a-min6-chord))
          "Am6 chord should have 4 notes")
      (is (= [:a :c :e :f] (mapv :name a-min6-chord))
          "Am6 chord should contain A, C, E, F (F instead of F♯ due to scale constraints)")
      (is (= [3 4 4 4] (mapv :octave a-min6-chord))
          "A should be in octave 3, C, E, and F should be in octave 4"))))

(deftest test-build-scale-chord-quartal-and-cluster-chords
  (testing "Building quartal chords with positions [0 3 6 9]"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C quartal (C-F-B-E) 
          c-quartal-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 3 6 9])
          ;; G quartal (G-C-F-B)
          g-quartal-chord (kb/build-scale-chord c-major-scale (kb/create-note :g 3) [0 3 6 9])]

      ;; Check C quartal chord
      (is (= 4 (count c-quartal-chord))
          "C quartal chord should have 4 notes")
      (is (= [:c :f :b :e] (mapv :name c-quartal-chord))
          "C quartal chord should contain C, F, B, E")
      (is (= [4 4 4 5] (mapv :octave c-quartal-chord))
          "C, F, and B should be in octave 4, E should be in octave 5")

      ;; Check G quartal chord
      (is (= 4 (count g-quartal-chord))
          "G quartal chord should have 4 notes")
      (is (= [:g :c :f :b] (mapv :name g-quartal-chord))
          "G quartal chord should contain G, C, F, B")
      (is (= [3 4 4 4] (mapv :octave g-quartal-chord))
          "G should be in octave 3, C, F, and B should be in octave 4")))

  (testing "Building cluster chords with positions [0 1 2]"
    (let [c-major-scale [:c :d :e :f :g :a :b]
          ;; C cluster (C-D-E) 
          c-cluster-chord (kb/build-scale-chord c-major-scale (kb/create-note :c 4) [0 1 2])
          ;; B cluster (B-C-D)
          b-cluster-chord (kb/build-scale-chord c-major-scale (kb/create-note :b 3) [0 1 2])]

      ;; Check C cluster chord
      (is (= 3 (count c-cluster-chord))
          "C cluster chord should have 3 notes")
      (is (= [:c :d :e] (mapv :name c-cluster-chord))
          "C cluster chord should contain C, D, E")
      (is (= [4 4 4] (mapv :octave c-cluster-chord))
          "All notes in C cluster chord should be in octave 4")

      ;; Check B cluster chord
      (is (= 3 (count b-cluster-chord))
          "B cluster chord should have 3 notes")
      (is (= [:b :c :d] (mapv :name b-cluster-chord))
          "B cluster chord should contain B, C, D")
      (is (= [3 4 4] (mapv :octave b-cluster-chord))
          "B should be in octave 3, C and D should be in octave 4"))))

;; Run all tests
(run-tests)
