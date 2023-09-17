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
    [taoensso.timbre.appenders.3rd-party.sentry :as sentry]
    [hiccup.core :as h]
    [clojure.string :as str]))

(def sentry-dsn (env :sentry-dsn))

(log/merge-config! {:appenders {:sentry (sentry/sentry-appender sentry-dsn {})}})

(defn gen-prices [price]
  (log/debugf "price split: %s" (str/split price #"K"))
  (log/debugf "first: %s" (str/trim (first (str/split price #"K"))))
  (let [czk-value (Integer/parseInt (str/trim (first (str/split price #"K"))))]
    (str (/ czk-value 24) "€ (" price ")")))

(defn mark-row [book wanted-books]
  (cond
    (or (contains? (set (map str/lower-case wanted-books)) "anything")
        (seq (filter #(< % 0.1) (:scores book)))) "success"
    (seq (filter #(< % 0.4) (:scores book))) "warning"
    :else ""))

(defn gen-table [sq]
  (log/debugf "number of rows: %s" (first sq))
  (h/html
    [:div
     [:meta {:charset "utf-8"}]
     [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css"}]
     [:div.container
      [:div.row
       [:div
        [:h2 (str "Found books for " (count (filter #(seq (:books %)) sq)) " authors (total " (count sq) ")")] ]
       [:div {:class ""}
        (for [entry sq]
          (when (seq (:books entry))
          ;  (log/debugf "table entry: %s" entry)
            [:div
             [:h4 (str/capitalize (:author entry))]
             [:h5 (str/join ", " (:wanted-books entry))]
             [:table {:class "table"}
              [:tr [:th "Book"] [:th "Scores"] [:th "Price"]]
              (for [book (sort-by :title (:books entry))]
                [:tr {:class (mark-row book (:wanted-books entry))}
                 [:td [:a {:href (:url book)} (:title book)]]
                 [:td (str/join ", " (map #(format "%.2f" % ) (:scores book)))]
                 [:td (:price book)]
                 (when (:vendor book)
                   [:td (:vendor book)])])]]
            )
          )]]]]))

(defn process-request [params]
  (if-let [shop (get params "scan")]
    (gen-table (scan-search (keyword shop)))
    (pr-str (n/send-msg (find-wanted)))))

(defn handler [{params :params}]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (process-request params)})

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