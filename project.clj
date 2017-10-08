(defproject product-tracker "1.0.0-SNAPSHOT"
  :url "https://product-tracker.herokuapp.com/"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.2.2"]
                 [environ "1.1.0"]
                 [reaver "0.1.2"]
                 [clj-http "2.1.0"]
                 [prismatic/schema "1.1.3"]
                 [com.taoensso/timbre "4.10.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.5.0"]
                 [raven-clj "1.5.0"]]

  :min-lein-version "2.7.0"
  :plugins [[lein-environ "1.1.0"]]
  :main product-tracker.web
  :uberjar-name "product-tracker-standalone.jar"

  :profiles {:dev {:env {:environment "dev"}}})
