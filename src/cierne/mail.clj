(ns cierne.mail
  (:require [clj-http.client :as client]
            [hiccup.core :refer [html]]
            [cheshire.core :refer [generate-string parse-string]]))



(defn format-email
  [books]
  (html [:div
         (for [{:keys [url img author name]} books]
           [:a {:href url}
            [:img {:src img :style "max-height: 150px;"}]
            [:h4 (str author " - " name)]])]))


(defn send-email
  [books]
  (client/post "https://api.mailgun.net/v3/sandbox00137d7df0fe45dbbfc2624dc0a96dc0.mailgun.org/messages"
             {:basic-auth  "api:key-c51b53b22ab8981828850240bc85eb3e"
              :form-params {:from    "Cierne Na Bielom Notif <postmaster@sandbox00137d7df0fe45dbbfc2624dc0a96dc0.mailgun.org>"
                            :to      "Ladislav Gubik <lacogubik@gmail.com>"
                            :subject (str (count books) " new - " (apply str (interpose ", " (map :author books))))
                            :html    (format-email books)}}))

(defn post-to-slack
  [book-msg]
  (client/post "https://hooks.slack.com/services/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"
            {:content-type :json
             :body         (generate-string book-msg)}))

(defn send-slack
  [books]
  (for [{:keys [url img author name]} books]
    (post-to-slack {:channel     "#knihy"
                    :username    "cierne-na-bielom"
                    :icon_emoji  ":cierne:"
                    :attachments [{:fallback    (str author " - " name)
                                   :pretext     "<@laco>"
                                   :author_name author
                                   :author_link url
                                   :title_link  url
                                   :color       "#36a64f"
                                   :title       name
                                   :image_url   img}]})))


(defn send-msg
  [books]
  (if (not-empty books)
    (do
      (send-email books)
      (send-slack books)
      )
    "No new books."))
