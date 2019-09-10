(ns product-tracker.find
  (:require
    [clojure.string :as str]
    [product-tracker.airtable :as air]
    [product-tracker.db :as db]
    [reaver :refer [parse extract-from text attr edn]]
    [taoensso.timbre :as log]))

(def page-batch 2)

(defn strip-accents [s]
  (org.apache.commons.lang3.StringUtils/stripAccents s))

(def wanted-authors #{
                      ;"alexijevič"                          ;Poslední svedkovia
                      ;"andruchovyč"                         ;rekreacie
                      ;"åsbrink"                             ;1947
                      ;"asbrink"                             ;1947
                      ;"aurelius"                            ;Meditace, hovory k sobe
                      ;"bacová"                              ;SLOVENSKÝ RODINNÝ DOM 2000 - 2015
                      ;"bartošová"                           ;Napriek totalite
                      ;"baláž"                               ;Posledná pevnosť
                      ;"bán"                                 ;Slon na Zemplíne
                      ;"belyj"                               ;peterburg, Večné volanie
                      ;"bene"                                ;korporacio
                      ;"canetti"                             ;zaslepeni
                      ;"cigánová"                            ;Aksal, Šampanské, káva, pivo
                      ;"domoslawski"                         ;Vylúčení
                      ;"domosławski"                         ;Vylúčení
                      ;"dušek"                               ;Melon sa vzdy smeje
                      ;"eco"                                 ;Tajemný plamen královny Loany
                      ;"filimonov"                           ;Andrej Tarkovskij
                      ;"frisch"                              ;Homo Faber
                      ;"glukhovsky"                          ;Poviedky o Rusku
                      ;"grečner"                             ;Film ako volny vers
                      ;"havran"                              ;Analfabet
                      ;"hesse"                               ;Cesta do orientu
                      ;"herr"                                ;Depese
                      ;"jančar"                              ;Drago, Kateřina, páv a jezuita
                      ;"jakubisko"                           ;Zive striebro
                      ;"kadlečík"                            ;Z rečí v nížinách - Tváre a oslovenia - Epištoly
                      ;"kapuściński"                         ;eben
                      ;"kapuscinski"                         ;eben
                      ;"karous"                              ;Vetřelci a volavky
                      ;"kepplová"                            ;Reflux
                      ;"králik"                              ;Stručný etymologický slovník slovenčiny
                      ;"krastev"                             ;Čo príde po Európe?
                      ;"kupka"                               ;Ruská moderna, Ruská avangarda
                      ;"kundera"                             ;Nesmrtelnost, Ptákovina, Směšné lásky, O hudbě a románu, Zahradou těch, které mám rád, Slova, pojmy, situace, Kniha smíchu a zapomnění
                      ;"kuznial"                             ;Papusa
                      "kameníček"
                      ;"kiš"                                 ;hrobka borisa davidovica
                      ;"klíma"                               ;Moje šílené století (1. 2.), Soudce z milosti (Stojí, stojí šibenička)
                      ;"kompaníková"                         ;Piata loď, Na sutoku
                      ;"krištúfek"                           ;Tela
                      ;"kryštopa"                            ;Ukrajina v měřítku 1:1
                      ;"kuźniak"                             ;Papusa
                      ;"kuzniak"                             ;Papusa
                      ;"lem"                                 ; Pribehy pilota pirxe
                      ;"lyrik"                               ;Naničhodný poet
                      ;"makine"                              ;Francuzky testament
                      "mathé"                               ;Ján Mathé. Hľadač dobra
                      "matuštík"                            ;Ján Mathé. Hľadač dobra
                      ;"mccarthy"                            ;Krvavý poledník aneb Večerní červánky na západě
                      ;"mitana"                              ;Psie dni, Patagonia
                      ;"mišima"                              ;Zlatý pavilon
                      ;"mojžiš"                              ;Voľným okom
                      ;"montefiore"                          ;Romanovovci 1613-1918
                      ;"mňačko"                              ;Smrt sa vola Engelchen
                      ;"nadas"                               ;Kniha pamati
                      ;"osorgin"                             ;Příběh jedné moskevské ulice
                      ;"paz"                                 ;Každodenný oheň
                      ;"pamuk"                               ;Jmenuji se Červená
                      ;"pišťanek"                            ;Rivers of Babylon
                      "pištalo"                             ;Tesla, portrét medzi maskami
                      ;"platonov"                            ;cevengur
                      ;"rakús"                               ;Mačacia krajina
                      "rejmer"                              ;Bukurešť. Prach a krev
                      "rosová"                              ;Male vianoce
                      "satinský"                            ;Gundžovníky
                      ;"seierstad"                           ;Kníhkupec z Kábulu
                      ;"singer"                              ;kejklir z lubliny
                      ;"sloboda"
                      ;"solženicyn"                          ;polostrov gulag
                      ;"stasiuk"                             ;Východ
                      "steinbeck"                           ;O mysiach a ludoch
                      "staviarsky"                          ;Rinaldova cesta
                      "stravinski"                          ;Hudobná poetika. Kronika môjho života
                      "sudor"                               ;Fedor Gál. Ešte raz a naposledy. Koniec príbehu
                      "ščeblykin"                           ;Jsme jako oni - Rozhovor s Martinem M. Šimečkou o liberálech, pokrytcích a fašistech
                      "šimečka"                             ;Hladanie obav, listy z vazenia
                      "stach"                               ;Kafka / Rané roky 1883-1911
                      "štrasser"                            ;Osem vytrvalých
                      "štrpka"                              ;Basne 3
                      ;"szablowski"                          ;Vrah z mesta marhul
                      "tallián"                             ;Béla Bartók
                      ;"tatarka"                             ;Navrávačky, Listy do večnosti, Neslovný príbeh, Hovory o kultúre a obcovaní, Kultúra ako obcovanie
                      ;  "ulická"                              ;Jakubov rebrík
                      "urc"                                 ;Neviditeľné dejiny dokumentaristov
                      "ursíny"                              ;Ahoj tato, milý Kubo, 6 x Dežo Ursiny - 2 DVD
                      "ursiny"
                      ;"vaculík"                             ;jsme v nebi, Cesta na Praděd
                      ;"velikic"                             ;Severna stena, Vysetrovatel
                      ;"vilikovský"                          ;Pes na ceste
                      "weidermann"                          ;Ostende 1936. Léto přátelství
                      "werfel"                              ;Hvězda nenarozených, Barbora neboli zbožnost
                      "zajíček"                             ;Jsme jako oni - Rozhovor s Martinem M. Šimečkou o liberálech, pokrytcích a fašistech
                      })

(defn wanted-authors []
  (->>
    (air/list-records air/base-id air/authors-table)
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
                                  (db/get-data shop)
                                  recent-books)]
               (log/info "Latest books:")
               (clojure.pprint/pprint latest-books)
               (db/store-data shop (first recent-books))
               (filter-authors (wanted-authors) latest-books)))))