(ns product-tracker.airtable
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clj-http.client :as client]
    [environ.core :refer [env]]
    [taoensso.timbre :as log]))

(def api-key (env :airtable-api-key))
(def base-id (env :airtable-base-id))
(def authors-table "Authors")
(def books-table "Books")
(def recent-table "Recent")

(defn list-records [table]
  (let [{:keys [status body] :as resp} (client/get (str "https://api.airtable.com/v0/" base-id "/" table)
                                                   {:headers          {"Authorization" (str "Bearer " api-key)}
                                                    :throw-exceptions false
                                                    :as               :json
                                                    :cookie-policy :none})]
    (if (client/success? resp)
      (let [recs (:records body)]
        (log/debugf "Fetched %s records from %s table." (count recs) table)
        recs)                                               ;TODO when offset fetch another one
      (log/errorf "Error listing records:%s , %s" status body))))

(defn get-record [table record-id]
  (let [{:keys [status body] :as resp} (client/get (str "https://api.airtable.com/v0/" base-id "/" table "/" record-id)
                                                   {:headers          {"Authorization" (str "Bearer " api-key)}
                                                    :throw-exceptions false
                                                    :as               :json
                                                    :cookie-policy :none})]
    (if (client/success? resp)
      (let [recs (:records body)]
        (log/debugf "Fetched %s record from %s table." (:id body) table)
        body)
      (log/errorf "Error listing records:%s , %s" status body))))


(defn update-record [table data]
  ;(clojure.pprint/pprint (generate-string data))
  (let [{:keys [status body] :as resp} (client/put (str "https://api.airtable.com/v0/" base-id "/" table)
                                                   {:headers          {"Authorization" (str "Bearer " api-key)}
                                                    :body             (generate-string data)
                                                    :content-type     :json
                                                    :throw-exceptions false
                                                    :as               :json})]
    (if (client/success? resp)
      (let [recs (:records body)]
        (log/debugf "Updated record")
        recs)
      (log/errorf "Error updating record:%s , %s" status body))))

(defn create-record [table data]
  ;(clojure.pprint/pprint (generate-string data))
  (let [{:keys [status body] :as resp} (client/post (str "https://api.airtable.com/v0/" base-id "/" table)
                                                    {:headers          {"Authorization" (str "Bearer " api-key)}
                                                     :body             (generate-string data)
                                                     :content-type     :json
                                                     :throw-exceptions false
                                                     :as               :json})]
    (if (client/success? resp)
      (let [recs (:records body)]
        (log/debugf "Created new record")
        recs)
      (log/errorf "Error creating record:%s , %s" status body))))



(defn find-recent-shop [shop]
  (->>
    (list-records recent-table)
    (filter #(= (name shop) (-> % :fields :Shop)))
    ))

(defn get-latest-shop-state [shop]
  (some->>
    (find-recent-shop shop)
    first
    (:fields)
    (:Latest)
    read-string
    ))

(defn store-shop-state [shop data]
  (let [current (find-recent-shop shop)
        recent-id (when (seq current)
                    (-> current first :id))
        data {:records [(merge (when recent-id
                                 {:id recent-id})
                               {:fields {"Shop"   (name shop)
                                         "Latest" (pr-str data)}})]}]
    (if recent-id
      (update-record recent-table data)
      (create-record recent-table data))))


