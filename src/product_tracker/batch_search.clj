(ns product-tracker.batch-search
  (:require
    [clojure.string :as str]
    [product-tracker.airtable :as air]
    [reaver :refer [parse extract-from text attr attrs edn]]
    [taoensso.timbre :as log]))

(defn strip-accents [s]
  (org.apache.commons.lang3.StringUtils/stripAccents s))

(defn wanted-authors []
  (->>
    (air/list-records air/authors-table)
    (map (comp :fields))
    (map (fn [m]
           (str (:Firstname m) " " (:Surname m))))
    (map str/trim)
    (remove str/blank?)
    (map (comp str/lower-case strip-accents))
    ))

(defmulti get-book-data (fn [k _] k))

(defmethod get-book-data :cierne-na-bielom
  [_ page-num]
  (println "Fetching cierne " page-num)
  (let [base-url "https://www.ciernenabielom.sk/"
        url (str base-url "uvod/strana-")]
    (Thread/sleep (* 200 (inc (rand-int 5))))
    (-> (str url (inc page-num) "/")
        slurp
        parse
        (extract-from ".produkt" [:title :author :url :img-url]
                      ".info a h2" text
                      ".info h3 a" text
                      ".info a" (fn [m]
                                  (->>
                                    (attr (first m) :href)
                                    (str base-url)))
                      ".foto a" (fn [m]
                                  (->> (attr m :href)
                                       (str base-url)))))))

(defmethod get-book-data :vona-knihy
  [_ author]
  ;(println "Searching vona knihy:" author)
  (Thread/sleep (* 200 (inc (rand-int 5))))
  (let [base-url "https://www.vona-knihy.sk/"
        author (str/replace author " " "+")
        books (-> (str "https://www.vona-knihy.sk/search-engine.htm?slovo=" author "&search_submit=&hledatjak=2")
                  slurp
                  parse
                  (extract-from ".product" [:title :stock :url]
                                ".productTitleContent a" text
                                ".stock_yes" attrs
                                ".img_box a" (fn [m]
                                               (->> (attr m :href)
                                                    (str base-url)))
                                ))]
    (->> books
         (filter (comp :stock))
         (map (fn [m]
                (str (:title m) ", " (:url m)))))
    ))

(defmethod get-book-data :knihobot
  [_ author]
  (Thread/sleep (* 200 (inc (rand-int 5))))
  (let [base-url "https://knihobot.cz"
        author2 (str/replace author " " "%20")
        books (-> (str "https://knihobot.cz/p/q/" author2)
                  slurp
                  parse
                  (extract-from ".product-list-item" [:title :price :url]
                                ".product-list-item__title-link" text
                                ".product-list-item__price span" text
                                ".product-list-item__title-link" (fn [m]
                                               (->> (attr m :href)
                                                    (str base-url)))
                                ))]
    (->> books
         (map (fn [m]
                (str author ", " (:title m) ", " (:price m) ", " (:url m)))))
    ))

(defn scan-search [shop]
  (sort (flatten (let [authors (wanted-authors)]
             (for [author authors]
               (get-book-data shop author))))))