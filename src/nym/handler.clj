(ns nym.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.basic-authentication :refer [basic-authentication-request]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]))

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

(def names
  ["Alpha"
   "Beta"
   "Gamma"
   "Delta"])

(defroutes app-routes
  (GET "/" [] (fn [req] (println req) (response names)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-is-admin)
      (wrap-basic-auth)
      (wrap-json-response)))
