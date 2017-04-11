(ns cierne.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]
            [cierne.find :refer [find-wanted]]
            [cierne.mail :refer [send-msg]]
            [circleci.rollcage.core :as rollcage]))

;;TODO set proper environemnt and switch to environ
(def r (rollcage/client (env :rollbar-access-token) {:environment "production"}))

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
      (session/wrap-session)
      (basic/wrap-basic-authentication authenticated?)))

(defroutes app
           (ANY "/repl" {:as req}
             (drawbridge req))
           (GET "/" []
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    (pr-str (send-msg (find-wanted)))})
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
      ((if (env :production)
         wrap-error-page
         trace/wrap-stacktrace))))


(defn -main [& args]
  (let [ip (or (env :openshift-clojure-http-ip) "0.0.0.0")
        port (-> (env :openshift-clojure-http-port)
                 (or "8080")
                 Integer/parseInt)]
    (jetty/run-jetty (wrap-app #'app) {:host ip :port port})))