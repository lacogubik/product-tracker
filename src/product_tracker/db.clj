(ns product-tracker.db
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clj-http.client :as client]
    [environ.core :refer [env]]
    [taoensso.timbre :as log]))

(def db-uri (env :db-uri))

(defn get-db-key [shop]
  (str db-uri (name shop) ".json"))

(defn store-data
  [shop data]
  (log/info "Storing  shop:" shop " data:" data)
  (let [{:keys [body status] :as resp} (client/put (get-db-key shop)
                                                   {:content-type :json
                                                    :body         (generate-string data)})]
    (when (> status 399)
      (log/warn resp))))

(defn get-data
  [shop]
  (let [{:keys [body status] :as resp} (client/get (get-db-key shop))]
    (log/info resp)
    (when (= 200 status)
      (parse-string body true))))