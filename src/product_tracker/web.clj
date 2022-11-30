(ns product-tracker.web
  (:require
    [environ.core :refer [env]]
    [product-tracker.find :refer [find-wanted]]
    [product-tracker.batch-search :refer [scan-search]]
    [product-tracker.notification :as n]
    [raven-clj.ring :as rvn]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.stacktrace :as trace]
    [ring.middleware.params :as ring-params]
    [taoensso.timbre :as log]
    [taoensso.timbre.appenders.3rd-party.sentry :as sentry]))

(def sentry-dsn (env :sentry-dsn))

(log/merge-config! {:appenders {:sentry (sentry/sentry-appender sentry-dsn {})}})

(defn process-request [params]
  (if-let [shop (get params "scan")]
    (scan-search (keyword shop))
    (n/send-msg (find-wanted))))

(defn handler [{params :params}]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (pr-str (process-request params))})

(defn wrap-app [app]
  (-> app
      (ring-params/wrap-params)
      (rvn/wrap-sentry sentry-dsn {})
      (trace/wrap-stacktrace)))

(defn -main [& args]
  (let [port (-> (env :port)
                 (or "8080")
                 Integer/parseInt)]
    (Thread/setDefaultUncaughtExceptionHandler
      (reify Thread$UncaughtExceptionHandler
        (uncaughtException [_ thread ex]
          (log/error ex "Uncaught exception on" (.getName thread)))))
    (jetty/run-jetty (wrap-app #'handler) {:port port})))