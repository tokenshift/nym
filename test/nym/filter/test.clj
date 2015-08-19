(ns nym.filter.test
  (:require [clojure.test :refer :all]
            [nym.filter :refer :all]))

(deftest test-word-filter
  (testing "simple word filter"
    (let [pred (make-word-filter "test")]
      (are [expected word] (= expected (pred word))
           true  "test"
           true  "testing"
           true  "tested"
           true  "test something else"
           true  "testing another thing"
           false "another test"
           false "yet another test"
           false "atest"
           false "atestb")))
  (testing "asterisk at the end"
    (let [pred (make-word-filter "test*")]
      (are [expected word] (= expected (pred word))
           true  "test"
           true  "testing"
           true  "tested"
           true  "test something else"
           true  "testing another thing"
           false "another test"
           false "yet another test"
           false "atest"
           false "atestb")))
  (testing "asterisk at the beginning"
    (let [pred (make-word-filter "*test")]
      (are [expected word] (= expected (pred word))
           true  "test"
           false "testing"
           false "tested"
           false "test something else"
           false "testing another thing"
           true  "another test"
           true  "yet another test"
           true  "atest"
           false "atestb")))
  (testing "asterisk at both"
    (let [pred (make-word-filter "*test*")]
      (are [expected word] (= expected (pred word))
           true  "test"
           true  "testing"
           true  "tested"
           true  "test something else"
           true  "testing another thing"
           true  "another test"
           true  "yet another test"
           true  "atest"
           true  "atestb"
           false "atesb"
           false "tes"
           false "est")))
  (testing "asterisk in the middle"
    (let [pred (make-word-filter "te*st")]
      (are [expected word] (= expected (pred word))
           true  "test"
           false "testing"
           false "tested"
           false "test something else"
           false "testing another thing"
           false "another test"
           false "yet another test"
           false "atest"
           false "atestb"
           false "atesb"
           false "tes"
           false "est"
           true  "tea timest"
           true  "tewhateverst"
           false "etewhateverst"
           false "tewhateverste")))
  (testing "many asterisks"
    (let [pred (make-word-filter "wha*te*ver*")]
      (are [expected word] (= expected (pred word))
           true "whatever"
           true "whateaver"
           true "what tea version"
           false "awhatever"))))

(deftest test-tags-filter
  (testing "single tag"
    (let [pred (make-tags-filter "Test Tag")]
      (are [expected tags] (= expected (pred tags))
           false []
           false nil
           true  ["Test Tag"]
           false ["TestTag"]
           true  ["Test Tag" "Another Tag"]
           true  ["Whatever" "Foo" "Another Tag" "Test Tag"]
           false ["Test Tag 2"]
           true  #{"Test Tag 3" "Test Tag 2" "Test Tag 1" "Test Tag"}
           false #{"Test Tag 3" "Test Tag 2" "Test Tag 1"})))
  (testing "list of tags"
    (let [pred (make-tags-filter "Tag 1, Tag 2, Tag 3")]
      (are [expected tags] (= expected (pred tags))
           false []
           false ["Tag 1"]
           false ["Tag 1" "Tag 2"]
           false ["Tag 2" "Tag 3"]
           true  ["Tag 1" "Tag 2" "Tag 3"]
           true  ["Tag 0" "Tag 1" "Tag 2" "Tag 3" "Tag 4"])))
  (testing "disjunction"
    (let [pred (make-tags-filter "(or Tag 1, Tag 2, Tag 3)")]
      (are [expected tags] (= expected (pred tags))
           false []
           false ["Tag 0"]
           true  ["Tag 1"]
           true  ["Tag 2"]
           true  ["Tag 3"]
           false ["Tag 4"]
           true  ["Tag 0" "Tag 2" "Tag 4"])))
  (testing "conjunction"
    (let [pred (make-tags-filter "(and Tag 1, Tag 2)")]
      (are [expected tags] (= expected (pred tags))
           false []
           false ["Tag 0"]
           false ["Tag 1"]
           false ["Tag 2"]
           false ["Tag 3"]
           false ["Tag 4"]
           false ["Tag 0" "Tag 1"]
           true  ["Tag 0" "Tag 1" "Tag 2" "Tag 3"])))
  (testing "negation"
    (let [pred (make-tags-filter "(not Tag 1, Tag 2)")]
      (are [expected tags] (= expected (pred tags))
           true  []
           true  ["Tag 0"]
           false ["Tag 1"]
           false ["Tag 2"]
           true  ["Tag 3"]
           false ["Tag 0" "Tag 1"])))
  (testing "everything"
    (let [pred (make-tags-filter "(or (and (not Tag 1, Tag 2) Tag 3, Tag 4) (and Tag 5, Tag 6 (not Tag 7, Tag 8, Tag 9)))")]
      (are [expected tags] (= expected (pred tags))
           false []
           false ["Tag 0"]
           false ["Tag 1"]
           false ["Tag 2"]
           false ["Tag 3"]
           false ["Tag 4"]
           false ["Tag 5"]
           false ["Tag 6"]
           false ["Tag 7"]
           false ["Tag 8"]
           false ["Tag 9"]
           false ["Tag 10"]
           true  ["Tag 3" "Tag 4"]
           true  ["Tag 3" "Tag 4" "Tag 5"]
           false ["Tag 1" "Tag 3" "Tag 4"]
           false ["Tag 2" "Tag 3" "Tag 4"]
           true  ["Tag 5" "Tag 6"]
           false ["Tag 5"]
           false ["Tag 6"]
           false ["Tag 5" "Tag 6" "Tag 7"]
           false ["Tag 5" "Tag 6" "Tag 8"]
           false ["Tag 5" "Tag 6" "Tag 9"]
           false ["Tag 0" "Tag 1" "Tag 2" "Tag 3" "Tag 4" "Tag 5" "Tag 6" "Tag 7" "Tag 8" "Tag 9" "Tag 10"]
           true  ["Tag 0" "Tag 3" "Tag 4" "Tag 5" "Tag 6" "Tag 7" "Tag 8" "Tag 9" "Tag 10"]
           true  ["Tag 0" "Tag 1" "Tag 2" "Tag 3" "Tag 4" "Tag 5" "Tag 6" "Tag 10"]))))
