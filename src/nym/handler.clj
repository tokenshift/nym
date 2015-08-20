(ns nym.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [nym.db :refer [->PersistedMemDB]]
            [nym.service :refer :all]
            [pandect.algo.sha512 :refer [sha512]]
            [ring.middleware.basic-authentication :refer [basic-authentication-request]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [response status]]))

(defn wrap-basic-auth
  [handler]
  (fn [req]
    (handler (basic-authentication-request req vector))))

(defn wrap-is-admin
  [handler]
  (fn [req]
    (handler (assoc req :is-admin
                    (and (= (:admin-username env)
                            (first (:basic-authentication req)))
                         (= (:admin-password-hash env)
                            (sha512 (str (second (:basic-authentication req))
                                         (:admin-password-salt env)))))))))

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
    (log/info (:request-method req)
              (if (:query-string req)
                (str (:uri req) "?" (:query-string req))
                (:uri req)))
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
  (let [db (->PersistedMemDB (:name-file env))
        nym-service (->NymServiceImpl db)]
    (-> (app-routes nym-service)
        (wrap-is-admin)
        (wrap-basic-auth)
        (wrap-params)
        (wrap-json-response)
        (wrap-log-requests))))

; App singleton.
(def app (new-app))
