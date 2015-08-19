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
