(ns cierne.find
  (:require [reaver :refer [parse extract-from text attr edn]]
            [clojure.string :as string]))

(def url "http://www.ciernenabielom.sk/uvod/strana-")


(def wanted-authors #{"andruchovyč" "andrejev"
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
                      "mlynárik"
                      "mitana"
                      "morante"                             ;pribeh v dejinach
                      "musil"                               ;muz bez  vlastnosti
                      "nabokov"                             ;pozvanie na popravu, prednasky o ruskej literature, lolita
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
                      "solženicyn"                          ;prvy kruh, polostrovie gulag
                      "staviarsky"
                      "šimečka"
                      "suk"                                 ;Labyrintem revoluce
                      "štrpka" "tatarka"
                      "tournier"                            ;kral tmy
                      "ulická"                              ;medea a jej deti
                      "vaculík"                             ;morcata
                      "vilikovský"
                      })



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
      (map #(assoc % :author (second (:author %)))))))


(defn get-latest
  []
  (flatten (for [n (range 10)]
             (get-wanted-books n))))


(defn find-wanted
  []
  (->> (get-latest)
       (filter #(contains? wanted-authors (-> (:author %)
                                              (string/split #" ")
                                              first
                                              string/lower-case)))))
