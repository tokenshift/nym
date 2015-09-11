(ns nym.service
  (:require [clojure.data.json :as json]
            [nym.db :as db]
            [nym.filter :refer [make-word-filter make-tags-filter]]
            [ring.util.response :refer [response status]]))

(defprotocol NymService
  "REST endpoints for working with words and tags."
  (get-word [this word]       "Returns a word with any associated tags.")
  (get-words [this options]   "Returns a list of words with associated tags.
                              Supported options:
                              * :offset, :limit
                                Control paging. Offset is 0-based.
                              * :query
                                A glob-style string to match words by. For example,
                                'word*' would match 'word', 'words', or 'wordy',
                                but not 'unword'.
                              * :tags
                                Filters returned words by tags. Accepts an
                                S-expression grammar of boolean operations on tags.")
  (random-words [this options] "Returns random words from the dictionary.
                                Accepts the same :query, :tags, and :limit
                                options as get-words (but not :offset).")
  (random-word-stream [this options] "Returns an infinite sequence of random
                                      words from the dictionary. Accepts the
                                      same :query and :tags options as get-words
                                      (but not :limit and :offset).")
  (del-word [this word]       "Removes a word (and all of its tags) from the dictionary.")
  (del-tags [this word tags]  "Removes the listed tags from a word.")
  (put-word [this word tags]  "Adds a word and its tags to the dictionary.")
  (get-tags [this]            "Lists all known tags."))

(defn- get-db-words
  "Retrieves words from the DB, handling offset, limit, query and tag filters."
  [nym-db {:strs [offset limit query tags] :or {offset 0}}]
  (let [word-filter (if query (make-word-filter query) (constantly true))
        tags-filter (if tags  (make-tags-filter tags)  (constantly true))
        words (->> (db/all-words nym-db)
                   (filter #(word-filter (:word %)))
                   (filter #(tags-filter (:tags %)))
                   (sort-by :word))
        word-count (count words)
        words (drop (Integer. offset) words)
        words (if limit (take (Integer. limit) words) words)]
    {:count word-count
     :offset offset
     :limit limit
     :words words}))

; Stores and retrieves word and tag information using the provided NymDB.
(defrecord NymServiceImpl [nym-db]
  NymService
  (get-word [this word]
    (if-let [tags (db/get-tags nym-db word)]
      (response {:success true
                 :word word
                 :tags tags})
      (status (response {:success false
                         :error "NOT FOUND"})
              404)))
  (get-words [this {:strs [offset limit query tags] :or {offset 0 limit 10} :as params}]
    (response (assoc (get-db-words nym-db (assoc params "limit" limit))
                     :success true)))
  (random-word-stream [this params]
    (let [{:keys [count words]} (get-db-words nym-db (select-keys params ["query" "tags"]))]
      (response (repeatedly #(str (json/write-str (when (> count 0) (nth words (rand-int count)))) "\n")))))
  (random-words [this {:strs [limit query tags] :or {limit 10} :as params}]
    (let [{:keys [count words]} (get-db-words nym-db (select-keys params ["query" "tags"]))]
      (response {:count count
                 :limit (Integer. limit)
                 :words (take (Integer. limit) (shuffle words))
                 :success true})))
  (del-word [this word]
    (db/del-word! nym-db word)
    (response {:success true}))
  (del-tags [this word tags]
    (db/del-tags! nym-db word tags)
    (response {:success true}))
  (put-word [this word tags]
    (db/add-tags! nym-db word tags)
    (response {:success true}))
  (get-tags [this]
    (response {:success true
               :tags (db/all-tags nym-db)})))
