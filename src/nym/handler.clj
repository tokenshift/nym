(ns nym.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [nym.db :as db]
            [ring.middleware.basic-authentication :refer [basic-authentication-request]]
            [ring.middleware.json :refer [wrap-json-response]]
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
  (get-words [this {:keys [offset limit query tags]}]
    (let [words (db/all-words nym-db)
          words (if offset (drop (Integer. offset) words) words)
          words (if limit  (take (Integer. limit) words) words)]
      (response {:success true
                 :words words})))
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

(defn wrap-basic-auth
  [handler]
  (fn [req]
    (handler (basic-authentication-request req vector))))

(defn wrap-is-admin
  [handler]
  (fn [req]
    (handler (assoc req :is-admin (and (= (:admin-username env)
                                          (first (:basic-authentication req)))
                                       (= (:admin-password env)
                                          (second (:basic-authentication req))))))))

(defn wrap-require-admin
  "Returns a 401 or 403 if the user isn't an admin."
  [handler]
  (fn [req]
    (cond (:is-admin req) (handler req)
          (:basic-authentication req) (status (response {:error "FORBIDDEN"}) 403)
          :else (status (response {:error "UNAUTHORIZED"}) 401))))

(defn wrap-log-requests
  [handler]
  (fn [req]
    (log/info (:request-method req) (:uri req))
    (handler req)))


(defn app-routes
  "Constructs a handler wrapping a NymService implementation."
  [nym-service]
  (routes
    (GET "/" {params :params} (random-word nym-service params))
    (GET "/words" {params :params} (get-words nym-service params))
    (context "/words" []
      (GET "/:word" [word] (get-word nym-service word))
      (PUT "/:word" [word] (wrap-require-admin (fn [req] (put-word nym-service word []))))
      (DELETE "/:word" [word] (wrap-require-admin (fn [req] (del-word nym-service word))))
      (context "/:word" [word]
        (PUT "/:tag" [tag] (wrap-require-admin (fn [req] (put-word nym-service word [tag]))))
        (DELETE "/:tag" [tag] (wrap-require-admin (fn [req] (del-tags nym-service word [tag]))))))
    (GET "/tags" [] (get-tags nym-service))
    (route/not-found (response {:error "NOT FOUND"}))))

(defn new-app
  []
  (let [db (db/->PersistedMemDB "names.txt")
        nym-service (->NymServiceImpl db)]
    (-> (app-routes nym-service)
        (wrap-is-admin)
        (wrap-basic-auth)
        (wrap-json-response)
        (wrap-log-requests))))

; App singleton.
(def app (new-app))
