(ns nym.service
  (:require [nym.db :as db]
            [ring.util.response :refer [response status]]))

(defprotocol NymService
  "REST endpoints for working with words and tags."
  (get-word [this word]       "Returns a word with any associated tags.")
  (get-words [this options]   "Returns a list of words with associated tags.
                              Supported options:
                              * :offset, :limit (NOT YET IMPLEMENTED)
                                Control paging. Offset is 0-based.
                              * :query (NOT YET IMPLEMENTED)
                                A glob-style string to match words by. For example,
                                'word*' would match 'word', 'words', or 'wordy',
                                but not 'unword'.
                              * :tags (NOT YET IMPLEMENTED)
                                Filters returned words by tags. Accepts a TBD grammar
                                of boolean operations on tags.")
  (random-word [this options] "Returns a single random word from the dictionary.
                               Accepts the same :query and :tags options as
                               `get-words` (but not :limit and :offset).")
  (del-word [this word]       "Removes a word (and all of its tags) from the dictionary.")
  (del-tags [this word tags]  "Removes the listed tags from a word.")
  (put-word [this word tags]  "Adds a word and its tags to the dictionary.")
  (get-tags [this]            "Lists all known tags."))

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
    (response {:success true
               :words (->> (db/all-words nym-db)
                           (sort-by :word)
                           (drop (Integer. offset))
                           (take (Integer. limit)))}))
  (random-word [this options]
    (let [words (db/all-words nym-db)
          word  (nth words (rand-int (count words)))]
      (if word
        (response {:success true
                   :word word})
        (status (response {:success false
                           :error "NO DATA"})
                500))))
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
