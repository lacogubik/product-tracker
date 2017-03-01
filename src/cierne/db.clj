(ns cierne.db
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [cierne.schema :as sch]))


(def previous-key "last-books-")
(def orchestrate-auth-key "f6b998f9-39bd-4bc1-a0a6-c06b775fbe65:")
(def orchestrate-collection-uri "https://api.orchestrate.io/v0/books/")

(s/defn get-db-key
  [shop :- s/Keyword]
  (str orchestrate-collection-uri previous-key (name shop)))

(s/defn store-data
  [shop :- s/Keyword
   data :- sch/Book]
  (log/info "Storing shop:" shop " data:" data)
  (let [{:keys [body status] :as resp} (client/put (get-db-key shop)
                                          {:basic-auth   orchestrate-auth-key
                                           :content-type :json
                                           :body         (generate-string data)})]
    (when (> status 399)
      (log/warn resp))))

(s/defn get-data
  [shop :- s/Keyword]
  (let [{:keys [body status] :as resp} (client/get (get-db-key shop)
                    {:basic-auth orchestrate-auth-key
                     :throw-exceptions false})]
    (log/info resp)
    (when (= 200 status)
      (parse-string body true))))