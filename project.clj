(defproject nym "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[environ "1.0.0"]
                 [instaparse "1.4.1"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-json "0.4.0"]
                 [ring-basic-authentication "1.0.5"]
                 [pandect "0.5.3"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler nym.handler/app}
  :uberjar-name "sprint-poker-standalone.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})