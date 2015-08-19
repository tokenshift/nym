(ns nym.db
  (:require [clojure.set :refer [difference union]]
            [me.raynes.fs :as fs]))

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

(defn ->MemDB
  "Creates a new in-memory (transient) DB instance."
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

(defn persist-db!
  "Save the contents of the database to the specified file.


  The storage format is a simple text file of alternating words & tags, so that
  it can also be easily populated by bash scripts and external crawlers."
  [db filename]
  (let [tempfile (fs/temp-file "PersistedMemDB")]
    (with-open [writer (clojure.java.io/writer tempfile)]
      (doseq [{:keys [word tags]} (all-words db)]
        (doseq [tag tags]
          (.write writer word)
          (.write writer "\n")
          (.write writer tag)
          (.write writer "\n"))))
    (fs/copy tempfile filename)))

(defn load-db!
  "Load the contents of the database from the specified file."
  [db filename]
  (with-open [reader (clojure.java.io/reader filename)]
    (doseq [[word tag] (->> (line-seq reader) (partition 2))]
      (add-tags! db word [tag]))))

(defn ->PersistedMemDB
  "Creates an in-memory DB instance that saves all updates to a file, and can
  load its initial state from that same file."
  [filename]
  (let [db (->MemDB)
        persister (agent nil)
        persist! (fn [_] (persist-db! db filename))]
    ; Create the file if it doesn't exist.
    (when-not (fs/exists? filename) (fs/touch filename))
    ; Load all existing entries into the DB.
    (load-db! db filename)
    ; Return the DB, wrapped with functionality to update the file backup on
    ; any change.
    (reify
      NymDB
      (all-words [this] (all-words db))
      (get-tags [this word] (get-tags db word))
      (add-tags! [this word tags] (add-tags! db word tags) (send-off persister persist!))
      (del-word! [this word] (del-word! db word) (send-off persister persist!))
      (del-tags! [this word tags] (del-tags! db word tags) (send-off persister persist!))
      (all-tags [this] (all-tags db))

      NymDBControl
      (clear! [this] (clear! db) (send-off persister persist!)))))
