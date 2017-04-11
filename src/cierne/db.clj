(ns cierne.db
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [cierne.schema :as sch]
            [environ.core :refer [env]]))


(def db-uri (env :db-uri))

(s/defn get-db-key
        [shop :- s/Keyword]
        (str db-uri (name shop) ".json"))

(s/defn store-data
  [shop :- s/Keyword
   data :- sch/Book]
  (log/info "Storing  shop:" shop " data:" data)
  (let [{:keys [body status] :as resp} (client/put (get-db-key shop)
                                          {:content-type :json
                                           :body         (generate-string data)})]
    (when (> status 399)
      (log/warn resp))))

(s/defn get-data
  [shop :- s/Keyword]
  (let [{:keys [body status] :as resp} (client/get (get-db-key shop)
                    {:throw-exceptions false})]
    (log/info resp)
    (when (= 200 status)
      (parse-string body true))))