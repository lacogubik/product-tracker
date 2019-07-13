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

(defn list-records [base-id table]
  (let [{:keys [status body] :as resp} (client/get (str "https://api.airtable.com/v0/" base-id "/" table)
                                          {:headers {"Authorization" (str "Bearer " api-key)}
                                           :throw-exceptions false
                                           :as :json})]
    (if (client/success? resp)
      (let [recs (:records body)]
        (log/debugf "Fetched %s authors." (count recs))
        recs)                                       ;TODO when offset fetch another one
      (log/errorf "Error listing records:%s , %s" status body))))