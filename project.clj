(defproject cierne "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://cierne.herokuapp.com"
  :license {:name "FIXME: choose"
            :url "http://example.com/FIXME"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.2.2"]
                 [ring-basic-authentication "1.0.5"]
                 [environ "0.5.0"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [reaver "0.1.2"]
                 [clj-http "2.1.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.5.0"]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "cierne-standalone.jar"
  :profiles {:production {:env {:production true}}})
