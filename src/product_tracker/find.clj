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
                      "aurelius"                            ;Meditace, hovory k sobe
                      "bartošová"                           ;Napriek totalite
                      "baláž"                               ;Posledná pevnosť
                      "belyj"                               ;peterburg
                      "bene"                                ;korporacio
                      "borges"                              ;spisy, cela kniznica
                      "canetti"                             ;zaslepeni
                      "cigánová"                            ;Aksal, Šampanské, káva, pivo
                      "dušek"                               ;Melon sa vzdy smeje
                      "eco"                                 ;Tajemný plamen královny Loany
                      "frisch"                              ;Homo Faber
                      "glukhovsky"                          ;Poviedky o Rusku
                      "grečner"                             ;Film ako volny vers
                      "havran"                              ;Analfabet
                      "hesse"                               ;Cesta do orientu
                      "jančar"                              ;Drago, Kateřina, páv a jezuita
                      "jakubisko"                           ;Zive striebro
                      "kadlečík"
                      "karous"                              ;Vetřelci a volavky
                      "kepplová"                            ;Reflux
                      "krajňak"                             ;Carpathia
                      "králik"                              ;Stručný etymologický slovník slovenčiny
                      "kupka"                               ;Ruská moderna, Ruská avangarda
                      "kundera"                             ;Nesmrtelnost, Ptákovina, Směšné lásky, O hudbě a románu, Zahradou těch, které mám rád, Slova, pojmy, situace, Kniha smíchu a zapomnění
                      "kameníček"
                      "kiš"                                 ;hrobka borisa davidovica
                      "klíma"                               ;Moje šílené století
                      "kompaníková"                         ;Piata loď, Na sutoku
                      "krištúfek"                           ;Tela
                      "lyrik"                               ;Naničhodný poet
                      "makine"                              ;Francuzky testament
                      "mathé"                               ;Ján Mathé. Hľadač dobra
                      "matuštík"                            ;Ján Mathé. Hľadač dobra
                      "mccarthy"                            ;Krvavý poledník aneb Večerní červánky na západě
                      "mitana"                              ;Psie dni, Patagonia
                      "mišima"                              ;Zlatý pavilon
                      "mojžiš"                              ;Voľným okom
                      "montefiore"                          ;Romanovovci 1613-1918
                      "nadas"                               ;Kniha pamati
                      "paz"                                 ;Každodenný oheň
                      "pamuk"                               ;Jmenuji se Červená
                      "pišťanek"                            ;Rivers of Babylon
                      "pištalo"                             ;Tesla, portrét medzi maskami
                      "platonov"                            ;cevengur
                      "rakús"                               ;Mačacia krajina
                      "rejmer"                              ;Bukurešť. Prach a krev
                      "rosová"                              ;Male vianoce
                      "sabato"                              ;tunel samoty
                      "singer"                              ;kejklir z lubliny
                      "sloboda"
                      "solženicyn"                          ;polostrov gulag
                      "stasiuk"                             ;Východ (slovenské vydanie)
                      "steinbeck"                           ;O mysiach a ludoch
                      "staviarsky"                          ;Rinaldova cesta
                      "sudor"                               ;Fedor Gál. Ešte raz a naposledy. Koniec príbehu
                      "šimečka"                             ;Hladanie obav, listy z vazenia
                      "stach"                               ;Kafka / Rané roky 1883-1911
                      "štrasser"                            ;Osem vytrvalých
                      "štrpka"                              ;Basne 3
                      "tallián"                             ;Béla Bartók
                      "tatarka"                             ;Navrávačky, Listy do večnosti, Neslovný príbeh, Hovory o kultúre a obcovaní, Kultúra ako obcovanie
                      "ulická"                              ;Jakubov rebrík
                      "urc"                                 ;Neviditeľné dejiny dokumentaristov
                      "ursíny"                              ;Ahoj tato, milý Kubo, 6 x Dežo Ursiny - 2 DVD
                      "ursiny"
                      "vaculík"                             ;jsme v nebi, Cesta na Praděd
                      "velikic"                             ;Severna stena, Vysetrovatel
                      "vilikovský"                          ;Pes na ceste, Prva a posledna laska
                      "weidermann"                          ;Ostende 1936. Léto přátelství
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
