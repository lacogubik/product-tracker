(ns product-tracker.find
  (:require
    [clojure.string :as str]
    [product-tracker.airtable :as air]
    [reaver :refer [parse extract-from text attr edn]]
    [taoensso.timbre :as log]))

(def page-batch 2)

(defn strip-accents [s]
  (org.apache.commons.lang3.StringUtils/stripAccents s))

(defn wanted-authors []
  (->>
    (air/list-records air/authors-table)
    (map (comp :Surname :fields))
    (remove str/blank?)
    (map (comp str/lower-case strip-accents))
    set))

(def stop-processing?* (atom false))

(defn pprint-map->
  [m]
  (clojure.pprint/pprint m)
  m)

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

(defmethod get-book-data :antikvariatik
  [_ page-num]
  (println "Fetching antikvariatik:" page-num)
  (let [base-url "http://www.antikvariatik.sk/"]
    (-> (str base-url "?podstranka=novinky&zoradenie=&poradie=&start=" (* page-num 50))
        slurp
        parse
        (extract-from ".index_obsah_vnutri_kniha" [:title :author :url :img-url]
                      "h2.index_obsah_vnutri_kniha_nazov a" text
                      "div.index_obsah_vnutri_kniha_autor a" text
                      "h2.index_obsah_vnutri_kniha_nazov a" (fn [m]
                                                              (->> (attr m :href)
                                                                   (str base-url)))
                      "div.index_obsah_vnutri_kniha_obrazok a img" (attr :src)))))

(defn get-latest
  [shop]
  (flatten (for [n (range page-batch)]
             (get-book-data shop n))))

(defn filter-old-books
  [last-item filtered-items item]
  ;(println "Filter:" last-item filtered-items item @stop-processing?*)
  (if (or @stop-processing?* (= (:url last-item) (:url item)))
    (do
      (reset! stop-processing?* true)
      filtered-items)
    (conj filtered-items item)))

(defn get-last-batch
  [last-item new-col]
  (reset! stop-processing?* false)
  (reduce (partial filter-old-books last-item) '() new-col))

(defn filter-authors [wanted-authors books]
  (filter (fn [book]
            (when-let [author (:author book)]
              (try
                (contains? wanted-authors (-> author
                                              (str/split #" ")
                                              first
                                              strip-accents
                                              str/lower-case))
                (catch Exception e
                  nil)))) books))


(defn find-wanted []
  (flatten (for [shop [:cierne-na-bielom #_:antikvariatik]]
             (let [recent-books (get-latest shop)
                   latest-books (get-last-batch
                                  (air/get-latest-shop-state shop)
                                  recent-books)]
               (when-not (seq recent-books)
                 (log/error "No books fetched, maybe layout has changed"))
               (log/info "Latest books:")
               (clojure.pprint/pprint latest-books)
               (air/store-shop-state shop (first recent-books))
               (filter-authors (wanted-authors) latest-books)))))