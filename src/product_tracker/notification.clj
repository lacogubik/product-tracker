(ns product-tracker.notification
  (:require [clj-http.client :as client]
            [hiccup.core :refer [html]]
            [cheshire.core :refer [generate-string parse-string]]
            [product-tracker.schema :as sch]
            [schema.core :as s]
            [environ.core :refer [env]]))

(def mail-domain (env :mail-domain))
(def mail-key (env :mail-key))
(def mail-to (env :mail-to))
(def slack-hook-uri (env :slack-hook-uri))

(s/defn format-email
  [books :- [sch/Book]]
  (html [:div
         (doall (for [{:keys [url img-url author title]} books]
                  [:a {:href url}
                   [:img {:src img-url :style "max-height: 150px;"}]
                   [:h4 (str author " - " title)]]))]))


(s/defn send-email
  [books :- [sch/Book]]
  (let [uri (str "https://api.mailgun.net/v3/" mail-domain "/messages")]
    (client/post uri
                 {:basic-auth  mail-key
                  :form-params {:from    (str "Poduct tracker Notif <postmaster@" mail-domain ">")
                                :to      (str "<" mail-to ">")
                                :subject (str (count books) " new - " (apply str (interpose ", " (map :author books))))
                                :html    (format-email books)}})))

(defn post-to-slack
  [book-msg]
  (client/post slack-hook-uri
               {:content-type :json
                :body         (generate-string book-msg)}))

(s/defn send-slack
  [books :- [sch/Book]]
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


(s/defn send-msg
  [books :- [sch/Book]]
  (when (seq books)
    (send-email books)
    (send-slack books))
  books)
