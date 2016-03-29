(ns cierne.mail
  (:require [clj-http.client :as client]
            [hiccup.core :refer [html]]))



(defn format-email
  [books]
  (html [:div
         (for [book books]
           [:a {:href (:url book)}
            [:img {:src (:img book) :style "max-height: 150px;"}]
            [:h4 (str (:author book) " - " (:name book))]])]))


(defn send-email
  [books]
  (if (not-empty books)
    (client/post "https://api.mailgun.net/v3/sandbox00137d7df0fe45dbbfc2624dc0a96dc0.mailgun.org/messages"
                 {:basic-auth  "api:key-c51b53b22ab8981828850240bc85eb3e"
                  :form-params {:from    "Cierne Na Bielom Notif <postmaster@sandbox00137d7df0fe45dbbfc2624dc0a96dc0.mailgun.org>"
                                :to      "Ladislav Gubik <lacogubik@gmail.com>"
                                :subject (str (count books) " new - " (apply str (interpose ", " (map :author books))))
                                :html    (format-email books)}})
    "No new books."))
