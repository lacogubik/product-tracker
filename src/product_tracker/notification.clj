(ns product-tracker.notification
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clj-http.client :as client]
    [environ.core :refer [env]]
    [hiccup.core :refer [html]]))

(def mail-domain (env :mail-domain))
(def mail-key (env :mail-key))
(def mail-to (env :mail-to))
(def slack-hook-uri (env :slack-hook-uri))

(defn format-email
  [books]
  (html [:div
         (doall (for [{:keys [url img-url author title]} books]
                  [:a {:href url}
                   [:img {:src img-url :style "max-height: 150px;"}]
                   [:h4 (str author " - " title)]]))]))

(defn send-email
  [books]
  (let [uri (str "https://api.mailgun.net/v3/" mail-domain "/messages")]
    (client/post uri
                 {:basic-auth  mail-key
                  :form-params {:from    (str "Poduct tracker Notif <postmaster@" mail-domain ">")
                                :to      (str "<" mail-to ">")
                                :subject (str (apply str (interpose ", " (map :author books))))
                                :html    (format-email books)}})))

(defn post-to-slack
  [book-msg]
  (client/post slack-hook-uri
               {:content-type :json
                :body         (generate-string book-msg)}))

(defn send-slack
  [books]
  (doall (for [{:keys [url img-url author title]} books]
           (post-to-slack {:channel     "#knihy"
                           :username    "product-tracker"
                           :icon_emoji  ":cierne:"
                           :attachments [{:fallback    (str author " - " title)
                                          :pretext     "<@laco>"
                                          :author_name author
                                          :author_link url
                                          :title_link  url
                                          :color       "#36a64f"
                                          :title       title
                                          :image_url   img-url}]}))))

(defn send-msg
  [books]
  (when (seq books)
    (send-email books)
    ;(send-slack books)
    )
  books)
