(ns vtakt-client.keyboard-test
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [vtakt-client.components.keyboard :as kb]
            [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as tcp]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))


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
    (is (thrown? js/Error (kb/create-scale-group nil)))))

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

(deftest test-error-handling
  (testing "Invalid interval inputs throw appropriate errors"
    (are [intervals] (thrown? js/Error (kb/create-scale-group intervals))
      [-1]           ;; Negative value
      [1 -1 2]       ;; Contains a negative value
      [1.5]          ;; Decimal value
      ["a" "b" "c"]  ;; Non-numeric values
      )))

(deftest generative-scale-group-tests
  (testing "Generative tests for create-scale-group function"
    (let [check-results (stest/check `kb/create-scale-group
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 10}})]
      (is (true? (get-in (first check-results) [:clojure.spec.test.check/ret :pass?]))
          (str "Failed with: " (-> check-results first :failure))))))

(def common-scales
  {:major [0 2 4 5 7 9 11]
   :minor [0 2 3 5 7 8 10]
   :pentatonic-major [0 2 4 7 9]
   :pentatonic-minor [0 3 5 7 10]
   :blues [0 3 5 6 7 10]
   :chromatic [0 1 2 3 4 5 6 7 8 9 10 11]})

(s/def ::musical-intervals
  (s/with-gen ::intervals
    (fn []
      (gen/frequency
       [[10 (gen/elements (vals common-scales))]
        [3  (gen/vector (gen/choose 0 11) 3 8)]
        [1  (gen/vector (gen/choose 0 24) 3 8)]]))))

(deftest test-expected-attributes-of-create-scale-group
  (testing "Make sure across random sample of input that attributes apply."
    (let [samples (gen/sample (s/gen ::musical-intervals) 10)]
      (doseq [[i sample] (map-indexed vector samples)]
        (let [result (kb/create-scale-group sample)]
          (doseq [[root scale] result]
            (is (= (count scale) (count sample)))))))))
