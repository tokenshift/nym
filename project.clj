(defproject nym "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[environ "1.0.0"]
                 [korma "0.4.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.3.1"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-json "0.4.0"]
                 [ring-basic-authentication "1.0.5"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler nym.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
