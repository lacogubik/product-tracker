(defproject product-tracker "1.0.0-SNAPSHOT"
  :url "https://product-tracker.herokuapp.com/"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.8.1"]
                 [clj-http "3.10.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [environ "1.1.0"]
                 [hiccup "1.0.5"]
                 [raven-clj "1.5.2"]
                 [reaver "0.1.2"]
                 [ring "1.7.1"]]

  :min-lein-version "2.7.0"
  :main product-tracker.web
  :uberjar-name "product-tracker-standalone.jar")
