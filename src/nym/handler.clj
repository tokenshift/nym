(ns nym.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [nym.db :as db]
            [ring.middleware.basic-authentication :refer [basic-authentication-request]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response status]]))

(defn get-name
  "Return an individual name."
  [req]
  (response (db/get-name (:name (:route-params req)))))

(defn put-name
  "Create/update a name."
  [req]
  (db/put-name! (:name (:route-params req))
                (remove nil? [(:tag (:route-params req))]))
  (response (db/get-name (:name (:route-params req)))))

(defn del-name
  "Delete a name."
  [req]
  (response {:success true :deleted (db/del-name! (:name (:route-params req)))}))

(defn del-tag
  "Delete a tag from a name."
  [req]
  (response {:success true :deleted (db/del-tags! (:name (:route-params req))
                                                  [(:tag (:route-params req))])}))

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

(defroutes app-routes
  (GET "/names" [] (response {:success true :names (db/get-names)}))
  (context "/names" []
    (GET "/:name" [] get-name)
    (PUT "/:name" [] (wrap-require-admin put-name))
    (DELETE "/:name" [] (wrap-require-admin del-name))
    (context "/:name" []
      (PUT "/:tag" [] (wrap-require-admin put-name))
      (DELETE "/:tag" [] (wrap-require-admin del-tag))))
  (GET "/tags" [] (response (db/get-tags)))
  (route/not-found (response {:error "NOT FOUND"})))

(def app
  (-> app-routes
      (wrap-is-admin)
      (wrap-basic-auth)
      (wrap-json-response)
      (wrap-log-requests)))
