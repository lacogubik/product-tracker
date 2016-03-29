(ns cierne.db
  (:require [clj-http.client :as client]
            [hiccup.core :refer [html]]
            [cheshire.core :refer [generate-string parse-string]]))


(def previous-key "last-books")
(def orchestrate-auth-key "f6b998f9-39bd-4bc1-a0a6-c06b775fbe65:")
(def orchestrate-collection-uri "https://api.orchestrate.io/v0/books/")

(defn store-data
  [data]
  (println "[store-data] Storing:" data)
  (client/put (str orchestrate-collection-uri previous-key)
              {:basic-auth   orchestrate-auth-key
               :content-type :json
               :body         (generate-string data)}))

(defn get-data
  []
  (let [{:keys [body status]} (client/get (str orchestrate-collection-uri previous-key)
                    {:basic-auth orchestrate-auth-key
                     :throw-exceptions false})]
    (println "[get-data] Status:" status)
    (println "[get-data] Body:" body)
    (when (= 200 status)
      (parse-string body true))))