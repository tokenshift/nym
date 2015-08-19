(ns nym.db.test
  (:require [clojure.test :refer :all]
            [nym.db :refer :all]))

(deftest test-mem-db
  (let [db (->MemDB)]
    (is (empty? (all-words db)))
    (is (empty? (all-tags db)))
    (is (nil? (get-tags db "word1")))

    (add-tags! db "word1" [])
    (is (not (empty? (all-words db))))
    (is (empty? (all-tags db)))
    (some #{"word1"} (map :word (all-words db)))
    (is (not (nil? (get-tags db "word1"))))
    (is (empty? (get-tags db "word1")))

    (add-tags! db "word2" ["tag1" "tag2" "tag3"])
    (is (not (empty? (all-tags db))))
    (is (some #{"tag1"} (all-tags db)))
    (is (some #{"tag2"} (all-tags db)))
    (is (some #{"tag3"} (all-tags db)))
    (is (= #{"tag1" "tag2" "tag3"} (get-tags db "word2")))

    (add-tags! db "word3" ["tag2"])
    (is (= #{"tag2"} (get-tags db "word3")))

    (del-tags! db "word2" ["tag2" "tag3"])
    (is (= #{"tag1"} (get-tags db "word2")))
    (is (some #{"tag1"} (all-tags db)))
    (is (some #{"tag2"} (all-tags db)))
    (is (not (some #{"tag3"} (all-tags db))))

    (del-word! db "word3")
    (is (= #{"tag1"} (get-tags db "word2")))
    (is (some #{"tag1"} (all-tags db)))
    (is (not (some #{"tag2"} (all-tags db))))
    (is (not (some #{"tag3"} (all-tags db))))
    (is (nil? (get-tags db "word3")))

    (clear! db)
    (is (empty? (all-words db)))
    (is (empty? (all-tags db)))))
