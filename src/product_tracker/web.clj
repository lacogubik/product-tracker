(ns product-tracker.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [product-tracker.find :refer [find-wanted]]
            [product-tracker.notification :as n]
            [circleci.rollcage.core :as rollcage]))

(def r (rollcage/client (env :rollbar-access-token) {:environment (or (env :environment) "dev")
                                                     :host        (env :openshift-app-dns)}))

(defroutes app
           (GET "/" []
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    (pr-str (n/send-msg (find-wanted)))})
           (ANY "*" []
             (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn wrap-rollbar [handler]
  (if-not r
    handler
    (fn [req]
      (try
        (handler req)
        (catch Exception e
          (rollcage/error r e (select-keys req [:uri]))
          (throw e))))))

(defn wrap-app [app]
  (-> app
      wrap-rollbar
      ((if (= "production" (env :environment))
         wrap-error-page
         trace/wrap-stacktrace))))


(defn -main [& args]
  (let [ip (or (env :openshift-clojure-http-ip) "0.0.0.0")
        port (-> (env :openshift-clojure-http-port)
                 (or "8080")
                 Integer/parseInt)]
    (jetty/run-jetty (wrap-app #'app) {:host ip :port port})))