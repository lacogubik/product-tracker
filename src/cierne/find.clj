(ns cierne.find
  (:require [reaver :refer [parse extract-from text attr edn]]
            [clojure.string :as string]
            [cierne.db :as db]))

(def url "http://www.ciernenabielom.sk/uvod/strana-")
(def page-batch 2)


(def wanted-authors #{"andruchovyč"
                      "andrejev"
                      "alighieri"                           ;bozska komedia
                      "belyj"                               ;peterburg
                      "beckett"                             ;molloy
                      "bernanos"                            ;dennik vidieckeho knaza
                      "bondy"
                      "borges"                              ;spisy, cela kniznica
                      "canetti"                             ;zaslepeni
                      "carpentier"                          ;stratene kroky
                      "dostojevskij"
                      "dušek"
                      "földvári"
                      "gombrowicz"                          ;kosmos
                      "jančar"                              ;Drago, Kateřina, páv a jezuita
                      "karous"
                      "kameníček"
                      "kiš"                                 ;encyklopedia mrtvych, hrobka borisa davidovica
                      "lasica"
                      "márai"
                      "marquez"
                      "mitana"
                      "morante"                             ;pribeh v dejinach
                      "musil"                               ;muz bez  vlastnosti
                      "nabokov"                             ;pozvanie na popravu, lolita
                      "pecka"
                      "pišťanek"
                      "platonov"                            ;cevengur
                      "rakús"                               ;Mačacia krajina
                      "satinský"
                      "sabato"                              ;tunel samoty
                      "saramago"                            ;mesto slepych
                      "sartre"                              ;hnus a ine
                      "singer"                              ;kejklir z lubliny
                      "sloboda"
                      "solženicyn"                          ;prvy kruh, polostrov gulag
                      "staviarsky"
                      "šimečka"
                      "suk"                                 ;Labyrintem revoluce
                      "štrpka"
                      "tatarka"
                      "tournier"                            ;kral tmy
                      "ulická"                              ;medea a jej deti
                      "vaculík"                             ;morcata
                      "vilikovský"
                      })

(def stop-processing?* (atom false))


(defn pprint-map->
  [m]
  (clojure.pprint/pprint m)
  m)

(defn get-book-data
  [n]
  (-> (str url (inc n) "/")
      slurp
      parse
      (extract-from ".foot-line" [:name :author :url :img] "h2 a.nazov" text "h2 a" text "h2 a.nazov" (attr :href) ".foot-img-cont a" (attr :href))))

(defn get-wanted-books
  [n]
  (println "Fetching page " n)
  (let [data (get-book-data n)]
    (->>
      data
      (map #(assoc % :author (second (:author %))))
      pprint-map->)))


(defn get-latest
  []
  (flatten (for [n (range page-batch)]
             (get-wanted-books n))))

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


(defn find-wanted
  []
  (let [recent-books (get-latest)
        latest-books (get-last-batch (db/get-data) recent-books)]
    (print "[latest-books]:")
    (clojure.pprint/pprint latest-books)
    (db/store-data (first recent-books))
    (filter #(contains? wanted-authors (-> (:author %)
                                           (string/split #" ")
                                           first
                                           string/lower-case)) latest-books)))
