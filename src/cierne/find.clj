(ns cierne.find
  (:require [reaver :refer [parse extract-from text attr edn]]
            [clojure.string :as string]
            [cierne.db :as db]))

(def url "http://www.ciernenabielom.sk/uvod/strana-")
(def page-batch 2)


(def wanted-authors #{"andruchovyč"                         ;rekreacie
                      "alexijevičová"                       ;zinkovy chlapci, vojna nema zensku tvar, modlidba za cernobyl
                      "bartošová"                           ;Napriek totalite
                      "baláž"                               ;Posledná pevnosť
                      "belyj"                               ;peterburg
                      "borges"                              ;spisy, cela kniznica
                      "canetti"                             ;zaslepeni
                      "carpentier"                          ;stratene kroky
                      "dušek"                               ;Melon sa vzdy smeje
                      "grečner"                             ;Film ako volny vers
                      "jančar"                              ;Drago, Kateřina, páv a jezuita
                      "jakubisko"                           ;Zive striebro
                      "kadlečík"
                      "karous"                              ;Vetřelci a volavky
                      "krajňak"                             ;Carpathia
                      "kupka"                               ;Ruská moderna, Ruská avangarda
                      "kundera"                             ;Nesmrtelnost, Ptákovina, Směšné lásky, O hudbě a románu, Zahradou těch, které mám rád, Slova, pojmy, situace
                      "kameníček"
                      "kiš"                                 ;hrobka borisa davidovica
                      "lem"                                 ;Solaris, a ine
                      "makine"                              ;Francuzky testament
                      "mathé"                               ;Ján Mathé. Hľadač dobra
                      "matuštík"                            ;Ján Mathé. Hľadač dobra
                      "mitana"                              ;Psie dni
                      "mojžiš"                              ;Voľným okom
                      "myšľanov"                            ;Sadomaso, fetiš a Tony
                      "pišťanek"
                      "platonov"                            ;cevengur
                      "rakús"                               ;Mačacia krajina
                      "rosová"                              ;Male vianoce
                      "sabato"                              ;tunel samoty
                      "saramago"                            ;mesto slepych
                      "singer"                              ;kejklir z lubliny
                      "sloboda"
                      "solženicyn"                          ;polostrov gulag
                      "staviarsky"                          ;Zachytka, Kale topanky, Clovek prijemny
                      "strugackij"                          ;Je tazke byt bohom, ine
                      "šimečka"
                      "tatarka"                             ;Navrávačky, Písačky pre milovanú Lutéciu, V ne čase, Listy do večnosti, Sám proti noci, Neslovný príbeh, Hovory o kultúre a obcovaní, Kultúra ako obcovanie
                      "vaculík"                             ;jsme v nebi
                      "vilikovský"                          ;Pes na ceste, Letmy sneh, Prva a posledna laska
                      "werfel"                              ;Hvězda nenarozených, Barbora neboli zbožnost
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


;(defn contains-book?
;  [last-book new-page]
;  (reduce (fn [book-exists? book]
;            (if (= (:url last-book) (:url book))
;              true
;              book-exists?)) new-page))
;
;(defn get-latest2
;  []
;  (let [last-book (db/get-data)]
;    (loop [n 0
;           pages '()]
;      (let [new-page (get-wanted-books n)]
;        (if (or (contains-book? last-book new-page) (> n page-batch))
;          (conj pages new-page)
;          (recur (inc n) (conj pages new-page)))))))

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
