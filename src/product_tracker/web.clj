(ns product-tracker.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [product-tracker.find :refer [find-wanted]]
            [product-tracker.notification :as n]
            [raven-clj.ring :as rvn]
            [taoensso.timbre.appenders.3rd-party.sentry :as sentry]
            [taoensso.timbre :as log]))

(def sentry-dsn (env :sentry-dsn))
(def environment (env :environment))

(log/merge-config! {:appenders {:sentry (sentry/sentry-appender sentry-dsn {:environment environment})}})

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

(defn wrap-app [app]
  (-> app
      (rvn/wrap-sentry sentry-dsn {:environment environment})
      ((if (= "production" environment)
         wrap-error-page
         trace/wrap-stacktrace))
      (handler/site)))


(defn -main [& args]
  (let [port (-> (env :port)
                 (or "8080")
                 Integer/parseInt)]
    (println "ENV:" environment)
    (Thread/setDefaultUncaughtExceptionHandler
      (reify Thread$UncaughtExceptionHandler
        (uncaughtException [_ thread ex]
          (log/error ex "Uncaught exception on" (.getName thread)))))
    (jetty/run-jetty (wrap-app #'app) {:port port})))