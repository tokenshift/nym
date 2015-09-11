(ns nym.service.test
  (:require [clojure.test :refer :all]
            [nym.db :refer [->MemDB] :as db]
            [nym.service :refer :all]))

(deftest test-nym-service
  (testing "get-word"
    (let [db (->MemDB)
          svc (->NymServiceImpl db)]
      (db/add-tags! db "word1" ["tag1" "tag2" "tag3"])
      (db/add-tags! db "word2" [])
      (db/add-tags! db "word3" nil)
      (is (= "word1" (-> (get-word svc "word1") :body :word)))
      (is (= #{"tag1" "tag2" "tag3"}
             (-> (get-word svc "word1") :body :tags)))
      (is (= "word2" (-> (get-word svc "word2") :body :word)))
      (is (empty? (-> (get-word svc "word2") :body :tags)))
      (is (= "word3" (-> (get-word svc "word3") :body :word)))
      (is (empty? (-> (get-word svc "word3") :body :tags)))))
  (testing "get-words"
    (let [db (->MemDB)
          svc (->NymServiceImpl db)]
      (is (:success (:body (get-words svc {}))))
      (is (= 0 (:count (:body (get-words svc {})))))
      (is (empty? (:words (:body (get-words svc {})))))
      (doseq [x (range 0 10)]
        (db/add-tags! db (str "word" x) [(str "tag" x)]))
      (is (= 10 (:count (:body (get-words svc {})))))
      (is (some #{"word0"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word1"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word2"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word3"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word4"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word5"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word6"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word7"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word8"} (map :word (-> (get-words svc {}) :body :words))))
      (is (some #{"word9"} (map :word (-> (get-words svc {}) :body :words)))))
    (testing "limit and offset"
      (let [db (->MemDB)
            svc (->NymServiceImpl db)]
        (doseq [x (range 0 50)]
          (db/add-tags! db (format "word%02d" x) [(format "tag%02d" x)]))
        (= 50 (:count (:body (get-words svc {}))))
        ; Limit defaults to 10
        (is (= 10 (count (-> (get-words svc {}) :body :words))))
        (is (= 10 (:limit (:body (get-words svc {})))))
        ; Offset defaults to 0
        (is (= 0 (:offset (:body (get-words svc {})))))
        (is (some #{"word00"} (map :word (-> (get-words svc {}) :body :words))))
        (is (not (some #{"word10"} (map :word (-> (get-words svc {}) :body :words)))))
        ; Both can be changed
        (is (= 5 (:limit (:body (get-words svc {"limit" 5})))))
        (is (= 5 (count (-> (get-words svc {"limit" 5}) :body :words))))
        (is (not (some #{"word00"} (map :word (-> (get-words svc {"offset" 1}) :body :words)))))
        (is (= 1 (:offset (:body (get-words svc {"offset" 1})))))
        (is (some #{"word01"} (map :word (-> (get-words svc {"offset" 1}) :body :words))))
        (is (some #{"word10"} (map :word (-> (get-words svc {"offset" 1}) :body :words))))
        (is (not (some #{"word11"} (map :word (-> (get-words svc {"offset" 1}) :body :words)))))
        ; Offset + limit past the end works fine.
        (is (= 50 (:count (:body (get-words svc {"limit" 100})))))
        (is (= 50 (count (:words (:body (get-words svc {"limit" 100}))))))
        (is (= 50 (:count (:body (get-words svc {"offset" 60})))))
        (is (= 0 (count (:words (:body (get-words svc {"offset" 60}))))))))
    (testing "query"
      (let [db (->MemDB)
            svc (->NymServiceImpl db)]
        (db/add-tags! db "test" [])
        (db/add-tags! db "testing" [])
        (db/add-tags! db "tested" [])
        (db/add-tags! db "test something else" [])
        (db/add-tags! db "testing another thing" [])
        (db/add-tags! db "another test" [])
        (db/add-tags! db "yet another test" [])
        (db/add-tags! db "atest" [])
        (db/add-tags! db "atestb" [])
        (is (= 5 (:count (:body (get-words svc {"query" "test"})))))
        (is (= 5 (count (:words (:body (get-words svc {"query" "test"}))))))
        (is (= 5 (:count (:body (get-words svc {"query" "test" "offset" 3})))))
        (is (= 2 (count (:words (:body (get-words svc {"query" "test" "offset" 3}))))))
        (is (some #{"test"} (map :word (-> (get-words svc {"query" "test"}) :body :words))))
        (is (some #{"testing"} (map :word (-> (get-words svc {"query" "test"}) :body :words))))
        (is (some #{"tested"} (map :word (-> (get-words svc {"query" "test"}) :body :words))))
        (is (some #{"test something else"} (map :word (-> (get-words svc {"query" "test"}) :body :words))))
        (is (some #{"testing another thing"} (map :word (-> (get-words svc {"query" "test"}) :body :words))))))
    (testing "tags"
      (let [db (->MemDB)
            svc (->NymServiceImpl db)
            tag-filter "(or (and (not Tag 1, Tag 2) Tag 3, Tag 4) (and Tag 5, Tag 6 (not Tag 7, Tag 8, Tag 9)))"]
        (db/add-tags! db "word1" ["Tag 5" "Tag 6" "Tag 7"])
        (db/add-tags! db "word2" ["Tag 0" "Tag 3" "Tag 4" "Tag 5" "Tag 6" "Tag 7" "Tag 8" "Tag 9" "Tag 10"])
        (db/add-tags! db "word3" ["Tag 6"])
        (is (= 1 (count (-> (get-words svc {"tags" tag-filter}) :body :words))))
        (is (some #{"word2"} (map :word (-> (get-words svc {"tags" tag-filter}) :body :words)))))))
  (testing "random-word"
    (let [db (->MemDB)
          svc (->NymServiceImpl db)]
      (db/add-tags! db "word1" [])
      (db/add-tags! db "word2" [])
      (db/add-tags! db "word3" [])
      (is (not (nil? (:words (:body (random-words svc {}))))))
      (is (= 3 (count (:words (:body (random-words svc {}))))))
      (is (every? #{"word1" "word2" "word3"} (map :word (:words (:body (random-words svc {}))))))
      (is (= 3 (:count (:body (random-words svc {}))))))))
