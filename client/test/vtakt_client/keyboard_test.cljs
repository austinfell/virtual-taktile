(ns vtakt-client.keyboard-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [vtakt-client.components.keyboard :as kb]))

(deftest test-scale-group-generation
  (testing "Makes empty list with all roots for empty interval list"
    (is
     (=
      {:asbf [] :gsaf [] :e [] :g [] :c [] :fsgf [] :b [] :d [] :f [] :csdf [] :dsef [] :a []}
      (kb/create-scale-group []))))
  (testing "Makes empty list with all roots for nil interval list"
    (is (thrown? js/Error (kb/create-scale-group nil))))
  (testing "Makes single element list with all roots for single note scale"
    (is
     (=
      {:asbf [:asbf]
       :gsaf [:gsaf]
       :e [:e]
       :g [:g]
       :c [:c]
       :fsgf [:fsgf]
       :b [:b]
       :d [:d]
       :f [:f]
       :csdf [:csdf]
       :dsef [:dsef]
       :a [:a]}
      (kb/create-scale-group [0]))))
  (testing "Handles repeated elements"
    (is
     (=
      {:asbf [:asbf :asbf]
       :gsaf [:gsaf :gsaf]
       :e [:e :e]
       :g [:g :g]
       :c [:c :c]
       :fsgf [:fsgf :fsgf]
       :b [:b :b]
       :d [:d :d]
       :f [:f :f]
       :csdf [:csdf :csdf]
       :dsef [:dsef :dsef]
       :a [:a :a]}
      (kb/create-scale-group [0 0]))))
  (testing "Makes double element list with all roots and subsequent for two note scale"
    (is
     (=
      {:asbf [:asbf :b]
       :gsaf [:gsaf :a]
       :e [:e :f]
       :g [:g :gsaf]
       :c [:c :csdf]
       :fsgf [:fsgf :g]
       :b [:b :c]
       :d [:d :dsef]
       :f [:f :fsgf]
       :csdf [:csdf :d]
       :dsef [:dsef :e]
       :a [:a :asbf]}
      (kb/create-scale-group [0 1]))))
  (testing "Makes double element list with all roots and subsequent for two note scale but second element is an octave higher than root"
    (is
     (=
      {:asbf [:asbf :b]
       :gsaf [:gsaf :a]
       :e [:e :f]
       :g [:g :gsaf]
       :c [:c :csdf]
       :fsgf [:fsgf :g]
       :b [:b :c]
       :d [:d :dsef]
       :f [:f :fsgf]
       :csdf [:csdf :d]
       :dsef [:dsef :e]
       :a [:a :asbf]}
      (kb/create-scale-group [0 13]))))
  (testing "Should properly generate structure of all major scales but it is an octave up"
    (is
     (=
      {:asbf [:asbf :c :d :dsef :f :g :a]
       :gsaf [:gsaf :asbf :c :csdf :dsef :f :g]
       :e [:e :fsgf :gsaf :a :b :csdf :dsef]
       :g [:g :a :b :c :d :e :fsgf]
       :c [:c :d :e :f :g :a :b]
       :fsgf [:fsgf :gsaf :asbf :b :csdf :dsef :f]
       :b [:b :csdf :dsef :e :fsgf :gsaf :asbf]
       :d [:d :e :fsgf :g :a :b :csdf]
       :f [:f :g :a :asbf :c :d :e]
       :csdf [:csdf :dsef :f :fsgf :gsaf :asbf :c]
       :dsef [:dsef :f :g :gsaf :asbf :c :d]
       :a [:a :b :csdf :d :e :fsgf :gsaf]}
      (kb/create-scale-group [12 14 16 17 19 21 23]))))
  (testing "Should properly generate structure of all major scales but some intervals are an octave up"
    (is
     (=
      {:asbf [:asbf :c :d :dsef :f :g :a]
       :gsaf [:gsaf :asbf :c :csdf :dsef :f :g]
       :e [:e :fsgf :gsaf :a :b :csdf :dsef]
       :g [:g :a :b :c :d :e :fsgf]
       :c [:c :d :e :f :g :a :b]
       :fsgf [:fsgf :gsaf :asbf :b :csdf :dsef :f]
       :b [:b :csdf :dsef :e :fsgf :gsaf :asbf]
       :d [:d :e :fsgf :g :a :b :csdf]
       :f [:f :g :a :asbf :c :d :e]
       :csdf [:csdf :dsef :f :fsgf :gsaf :asbf :c]
       :dsef [:dsef :f :g :gsaf :asbf :c :d]
       :a [:a :b :csdf :d :e :fsgf :gsaf]}
      (kb/create-scale-group [0 14 4 17 7 21 11]))))
  (testing "Should properly generate structure of all chromatic scales"
    (is
     (=
      {:asbf [:asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf :a]
       :gsaf [:gsaf :a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g]
       :e [:e :f :fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef]
       :g [:g :gsaf :a :asbf :b :c :csdf :d :dsef :e :f :fsgf]
       :c [:c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b]
       :fsgf [:fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef :e :f]
       :b [:b :c :csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf]
       :d [:d :dsef :e :f :fsgf :g :gsaf :a :asbf :b :c :csdf]
       :f [:f :fsgf :g :gsaf :a :asbf :b :c :csdf :d :dsef :e]
       :csdf [:csdf :d :dsef :e :f :fsgf :g :gsaf :a :asbf :b :c]
       :dsef [:dsef :e :f :fsgf :g :gsaf :a :asbf :b :c :csdf :d]
       :a [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf]}
      (kb/create-scale-group [0 1 2 3 4 5 6 7 8 9 10 11]))))
  (testing "Should properly generate structure of all major scales"
    (is
     (=
      {:asbf [:asbf :c :d :dsef :f :g :a]
       :gsaf [:gsaf :asbf :c :csdf :dsef :f :g]
       :e [:e :fsgf :gsaf :a :b :csdf :dsef]
       :g [:g :a :b :c :d :e :fsgf]
       :c [:c :d :e :f :g :a :b]
       :fsgf [:fsgf :gsaf :asbf :b :csdf :dsef :f]
       :b [:b :csdf :dsef :e :fsgf :gsaf :asbf]
       :d [:d :e :fsgf :g :a :b :csdf]
       :f [:f :g :a :asbf :c :d :e]
       :csdf [:csdf :dsef :f :fsgf :gsaf :asbf :c]
       :dsef [:dsef :f :g :gsaf :asbf :c :d]
       :a [:a :b :csdf :d :e :fsgf :gsaf]}
      (kb/create-scale-group [0 2 4 5 7 9 11]))))
  (testing "Handles negative values by throwing an error"
    (is (thrown? js/Error (kb/create-scale-group [-1]))))
  (testing "Throws exceptions if there is even a single invalid interval in the list"
    (is (thrown? js/Error (kb/create-scale-group [1 -1 2]))))
  (testing "Throws exception if numeric has decimals"
    (is (thrown? js/Error (kb/create-scale-group [1.5]))))
  (testing "Throws exception if non-numeric items entered"
    (is (thrown? js/Error (kb/create-scale-group ["a" "b" "c"]))))
  (testing "Should properly generate structure of all minor scales"
    (is
     (=
      {:asbf [:asbf :c :csdf :dsef :f :fsgf :gsaf]
       :gsaf [:gsaf :asbf :b :csdf :dsef :e :fsgf]
       :e [:e :fsgf :g :a :b :c :d]
       :g [:g :a :asbf :c :d :dsef :f]
       :c [:c :d :dsef :f :g :gsaf :asbf]
       :fsgf [:fsgf :gsaf :a :b :csdf :d :e]
       :b [:b :csdf :d :e :fsgf :g :a]
       :d [:d :e :f :g :a :asbf :c]
       :f [:f :g :gsaf :asbf :c :csdf :dsef]
       :csdf [:csdf :dsef :e :fsgf :gsaf :a :b]
       :dsef [:dsef :f :fsgf :gsaf :asbf :b :csdf]
       :a [:a :b :c :d :e :f :g]}
      (kb/create-scale-group [0 2 3 5 7 8 10])))))
