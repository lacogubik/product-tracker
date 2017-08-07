(ns product-tracker.find
  (:require [reaver :refer [parse extract-from text attr edn]]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [product-tracker.schema :as sch]
            [product-tracker.db :as db]))


(def page-batch 2)


(def wanted-authors #{"albahari"                            ;Snežný človek a ine
                      "andruchovyč"                         ;rekreacie
                      "alexijevičová"                       ;modlidba za cernobyl
                      "bartošová"                           ;Napriek totalite
                      "baláž"                               ;Posledná pevnosť
                      "belyj"                               ;peterburg
                      "borges"                              ;spisy, cela kniznica
                      "canetti"                             ;zaslepeni
                      "cigánová"                            ;Aksal, Spaky v trni, Šampanské, káva, pivo
                      "dušek"                               ;Melon sa vzdy smeje
                      "grečner"                             ;Film ako volny vers
                      "hesse"                               ;Cesta do orientu
                      "jančar"                              ;Drago, Kateřina, páv a jezuita
                      "jakubisko"                           ;Zive striebro
                      "kadlečík"
                      "karous"                              ;Vetřelci a volavky
                      "krajňak"                             ;Carpathia
                      "kupka"                               ;Ruská moderna, Ruská avangarda
                      "kundera"                             ;Nesmrtelnost, Ptákovina, Směšné lásky, O hudbě a románu, Zahradou těch, které mám rád, Slova, pojmy, situace
                      "kameníček"
                      "kiš"                                 ;hrobka borisa davidovica
                      "klíma"                               ;Moje šílené století
                      "lem"                                 ;Solaris, a ine
                      "makine"                              ;Francuzky testament
                      "mathé"                               ;Ján Mathé. Hľadač dobra
                      "matuštík"                            ;Ján Mathé. Hľadač dobra
                      "mitana"                              ;Psie dni
                      "mojžiš"                              ;Voľným okom
                      "myšľanov"                            ;Sadomaso, fetiš a Tony
                      "pišťanek"
                      "pištalo"                             ;Tesla, portrét medzi maskami
                      "platonov"                            ;cevengur
                      "rakús"                               ;Mačacia krajina
                      "rosová"                              ;Male vianoce
                      "sabato"                              ;tunel samoty
                      "saramago"                            ;mesto slepych
                      "singer"                              ;kejklir z lubliny
                      "sloboda"
                      "solženicyn"                          ;polostrov gulag
                      "staviarsky"                          ;Zachytka, Clovek prijemny
                      "strugackij"                          ;Je tazke byt bohom, ine
                      "šimečka"
                      "tallián"                             ;Béla Bartók
                      "tatarka"                             ;Navrávačky, V ne čase, Listy do večnosti, Sám proti noci, Neslovný príbeh, Hovory o kultúre a obcovaní, Kultúra ako obcovanie
                      "vaculík"                             ;jsme v nebi
                      "velikic"                             ;Severna stena, Vysetrovatel
                      "vilikovský"                          ;Pes na ceste, Letmy sneh, Prva a posledna laska
                      "werfel"                              ;Hvězda nenarozených, Barbora neboli zbožnost
                      })

(def stop-processing?* (atom false))


(defn pprint-map->
  [m]
  (clojure.pprint/pprint m)
  m)

(defmulti get-book-data (fn [k _] k))

(defmethod get-book-data :cierne-na-bielom
  [_ page-num]
  (println "Fetching cierne " page-num)
  (let [url "http://www.ciernenabielom.sk/uvod/strana-"]
    (-> (str url (inc page-num) "/")
        slurp
        parse
        (extract-from ".foot-line" [:title :author :url :img-url]
                      "h2 a.nazov" text
                      "h2 a" (fn [item]
                               (-> item
                                   text
                                   second))
                      "h2 a.nazov" (attr :href)
                      ".foot-img-cont a" (attr :href)))))

(defmethod get-book-data :antikvariatik
  [_ page-num]
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


(s/defn get-latest :- [sch/Book]
  [shop :- s/Keyword]
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


(defn find-wanted
  []
  (flatten (for [shop [:cierne-na-bielom :antikvariatik]]
             (let [recent-books (get-latest shop)
                   latest-books (get-last-batch
                                  (db/get-data shop)
                                  recent-books)]
               (log/info "Latest books:")
               (clojure.pprint/pprint latest-books)
               (db/store-data shop (first recent-books))
               (filter (fn [book]
                         (when-let [author (:author book)]
                           (contains? wanted-authors (-> author
                                                         (string/split #" ")
                                                         first
                                                         string/lower-case)))) latest-books)))))
