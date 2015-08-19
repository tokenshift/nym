(ns nym.db
  (:require [clojure.set :refer [difference union]]))

(defprotocol NymDB
  "Storage and retrieval abstraction for words and tags."
  (all-words [this]           "Returns all words, with associated tags.")
  (get-tags  [this word]      "Returns the set of tags for a word, or nil.")
  (add-tags! [this word tags] "Adds tags to a word. The word will be created if
                               it doesn't exist.")
  (del-word! [this word]      "Removes the word and all of its tags.")
  (del-tags! [this word tags] "Removes tags from a word.")
  (all-tags  [this]           "Lists all known tags."))

(defprotocol NymDBControl
  "Additional control functions for a NymDB implementation, used primarily in
  test and development."
  (clear! [this] "Clear out all words and tags from the database."))

; Creates a new in-memory (transient) DB instance."
(defn ->MemDB
  []
  (let [db (atom {})]
    ; The db is a map of words to sets of tags.
    (reify
      NymDB
      (all-words [this]
        (into [] (for [[word tags] @db] {:word word :tags (or tags (hash-set))})))
      (get-tags [this word]
        (get @db word))
      (add-tags! [this word tags]
        (swap! db #(update-in % [word] (fn [old-tags] (union (or old-tags (hash-set)) (set tags))))))
      (del-word! [this word]
        (swap! db #(dissoc % word)))
      (del-tags! [this word tags]
        (swap! db #(update-in % [word] (fn [old-tags] (difference (or old-tags (hash-set)) (set tags))))))
      (all-tags [this]
        (->> @db vals (apply union)))

      NymDBControl
      (clear! [this]
        (reset! db {})))))
