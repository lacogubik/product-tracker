(ns cierne.find
  (:require [reaver :refer [parse extract-from text attr edn]]
            [clojure.string :as string]
            [cierne.db :as db]))

(def url "http://www.ciernenabielom.sk/uvod/strana-")
(def page-batch 2)


(def wanted-authors #{"andruchovyč"                         ;rekreacie, moskoviada atd
                      "alexijevičová"                       ;zinkovy chlapci, vojna nema zensku tvar, modlidba za cernobyl
                      "andrejev"                            ;Propast a jine povidky, poviedka o siedmych obesenych
                      "bartošová"                           ;Napriek totalite
                      "baláž"                               ;Posledná pevnosť
                      "belyj"                               ;peterburg
                      "beckett"                             ;molloy, Prvá láska a iné prózy
                      "bellow"                              ;Sebrané povídky I. & II.
                      "bernanos"                            ;dennik vidieckeho knaza
                      "borges"                              ;spisy, cela kniznica
                      "canetti"                             ;zaslepeni
                      "carpentier"                          ;stratene kroky
                      "cinger"                              ;Voskovec a Werich. Dialogy přes železnou oponu
                      "dostojevskij"                        ;Bratia Karamazovci
                      "dušek"                               ;Melon sa vzdy smeje
                      "földvári"                            ;...až pod čiernu zem
                      "gombrowicz"                          ;kosmos, pornografia
                      "grečner"                             ;Film ako volny vers
                      "hesse"                               ;Stepný vlk, + ine
                      "hofstadter"                          ;Gödel, Escher, Bach. Existenciální gordická balada
                      "jančar"                              ;Drago, Kateřina, páv a jezuita
                      "jakubisko"                           ;Zive striebro
                      "karous"                              ;Vetřelci a volavky
                      "krajňak"                             ;Carpathia
                      "kupka"                               ;Ruská moderna, Ruská avangarda
                      "kameníček"
                      "kiš"                                 ;encyklopedia mrtvych, hrobka borisa davidovica
                      "lasica"
                      "makine"                              ;Francuzky testament
                      "mathé"                               ;Ján Mathé. Hľadač dobra
                      "matuštík"                            ;Ján Mathé. Hľadač dobra
                      "márai"                               ;Spoveď mešťana
                      "marquez"
                      "mitana"                              ;Psie dni
                      "mojžiš"                              ;Voľným okom
                      "morante"                             ;pribeh v dejinach
                      "myšľanov"                            ;Sadomaso, fetiš a Tony
                      "musil"                               ;muz bez  vlastnosti
                      "nabokov"                             ;pozvanie na popravu, lolita
                      "pain"                                ;more love cajky
                      "pišťanek"
                      "platonov"                            ;cevengur
                      "ragač"                               ;August 68
                      "rakús"                               ;Mačacia krajina
                      "rosová"                              ;Male vianoce
                      "satinský"
                      "sabato"                              ;tunel samoty
                      "saramago"                            ;mesto slepych
                      "sartre"                              ;nevolnost, hnus a ine
                      "singer"                              ;kejklir z lubliny
                      "sloboda"
                      "solženicyn"                          ;prvy kruh, polostrov gulag
                      "staviarsky"                          ;Zachytka, Kale topanky, Clovek prijemny
                      "šimečka"
                      "štrpka"                              ;Rukojemnik
                      "tatarka"                             ;Navrávačky, Písačky pre milovanú Lutéciu
                      "tournier"                            ;kral tmy/kral duchov,
                      "ulická"                              ;medea a jej deti
                      "vaculík"                             ;morcata, jsme v nebi
                      "vilikovský"                          ;Vlastnty zivotopis zla, Pes na ceste, Letmy sneh, Prva a posledna laska
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
