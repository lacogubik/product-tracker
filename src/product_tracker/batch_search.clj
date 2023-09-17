(ns product-tracker.batch-search
  (:require
    [clojure.string :as str]
    [product-tracker.airtable :as air]
    [reaver :refer [parse extract-from text attr attrs edn]]
    [taoensso.timbre :as log]))

(defn strip-accents [s]
  (org.apache.commons.lang3.StringUtils/stripAccents s))

(defn clean-str [s]
  (->
    (if (str/blank? s) "" s)
    str/trim
    str/lower-case
    strip-accents))

(defn parse-name [m reverse?]
  (if reverse?
    (clean-str (str (:Surname m) " " (:Firstname m)))
    (clean-str (str (:Firstname m) " " (:Surname m)))))

(defn wanted-authors [reverse-name?]
  (let [res (air/list-records air/authors-table)]
    (->>
      res
      (map (comp :fields))
      (map (fn [m]
             {:author (parse-name m reverse-name?)  :wanted-books (:Books m)}))
      (sort-by :author))))

(defn wanted-books [books]
  (for [book-id books]
    (let [book (air/get-record air/books-table book-id)]
      ;(log/debugf "book: %s" book)
      (-> book
          :fields
          :Name))))

(defn- compute-next-row
  "computes the next row using the prev-row current-element and the other seq"
  [prev-row current-element other-seq pred]
  (reduce
    (fn [row [diagonal above other-element]]
      (let [update-val
            (if (pred other-element current-element)
              ;; if the elements are deemed equivalent according to the predicate
              ;; pred, then no change has taken place to the string, so we are
              ;; going to set it the same value as diagonal (which is the previous edit-distance)
              diagonal

              ;; in the case where the elements are not considered equivalent, then we are going
              ;; to figure out if its a substitution (then there is a change of 1 from the previous
              ;; edit distance) thus the value is diagonal + 1 or if its a deletion, then the value
              ;; is present in the columns, but not in the rows, the edit distance is the edit-distance
              ;; of last of row + 1 (since we will be using vectors, peek is more efficient)
              ;; or it could be a case of insertion, then the value is above+1, and we chose
              ;; the minimum of the three
              (inc (min diagonal above (peek row)))
              )]
        (conj row update-val)))
    ;; we need to initialize the reduce function with the value of a row, since we are
    ;; constructing this row from the previous one, the row is a vector of 1 element which
    ;; consists of 1 + the first element in the previous row (edit distance between the prefix so far
    ;; and an empty string)
    [(inc (first prev-row))]

    ;; for the reduction to go over, we need to provide it with three values, the diagonal
    ;; which is the same as prev-row because it starts from 0, the above, which is the next element
    ;; from the list and finally the element from the other sequence itself.
    (map vector prev-row (next prev-row) other-seq)))

(defn levenshtein-distance
  "Levenshtein Distance - http://en.wikipedia.org/wiki/Levenshtein_distance
  In information theory and computer science, the Levenshtein distance is a metric for measuring the amount of difference
 between two sequences. This is a functional implementation of the levenshtein edit
distance with as little mutability as possible.
Still maintains the O(n*m) guarantee.
"
  [a b & {p :predicate  :or {p =}}]
  (peek
    (reduce
      ;; we use a simple reduction to convert the previous row into the next-row  using the
      ;; compute-next-row which takes a current element, the previous-row computed so far
      ;; and the predicate to compare for equality.
      (fn [prev-row current-element]
        (compute-next-row prev-row current-element b p))

      ;; we need to initialize the prev-row with the edit distance between the various prefixes of
      ;; b and the empty string.
      (map #(identity %2) (cons nil b) (range))
      a)))

(defn rel-score [str1 str2]
  (let [lev-score (levenshtein-distance str1 str2)
        max-str (max (count str1) (count str2))]
    (float (/ lev-score max-str))))

(defn compare-books [books wanted-books]
  (for [b books]
    (let [book-name (clean-str (:title b))
          scores (->>
                   wanted-books
                   (map clean-str)
                   (map (partial rel-score book-name)))]
      (assoc b :scores scores))))

(defmulti get-book-data (fn [k _] k))

(defmethod get-book-data :cierne-na-bielom
  [_ entry]
  (let [base-url "https://www.ciernenabielom.sk/"
        author (str/replace (:author entry) " " "-")
        wanted-books (vec (wanted-books (:wanted-books entry)))
        books (-> (str base-url "knihy/autor/" author)
                  slurp
                  parse
                  (extract-from ".produkt" [:title :author :url :price]
                                ".info a h2" text
                                ".info h3 a" text
                                ".info a" (fn [m]
                                            (->>
                                              (attr (first m) :href)
                                              (str base-url)))
                                ".price-cart span" text))]
    {:author (:author entry)
     :books  (compare-books books wanted-books)
     :wanted-books wanted-books}))

(defmethod get-book-data :vona-knihy
  [_ entry]
  (Thread/sleep (* 200 (inc (rand-int 5))))
  (let [base-url "https://www.vona-knihy.sk"
        author (str/replace (:author entry) " " "+")
        wanted-books (vec (wanted-books (:wanted-books entry)))
        books (-> (str base-url "/search-engine.htm?slovo=" author "&search_submit=&hledatjak=2")
                  slurp
                  parse
                  (extract-from ".product" [:title :stock :url]
                                ".productTitleContent a" text
                                ".stock_yes" attrs
                                ".img_box a" (fn [m]
                                               (->> (attr m :href)
                                                    (str base-url)))))]
    {:author (:author entry)
     :books  (compare-books (filter (comp :stock) books) wanted-books)
     :wanted-books wanted-books}))

(defmethod get-book-data :knihobot
  [_ entry]
  (let [base-url "https://knihobot.sk"
        _ (log/debugf "entry in knihobot: %s" entry)
        author2 (str/replace (:author entry) " " "%20")
        wanted-books (vec (wanted-books (:wanted-books entry)))
        books (-> (str base-url "/p/q/" author2)
                  (slurp :encoding "utf-8")
                  parse
                  (extract-from ".product-list__item" [:title :stock :price :url]
                                "h4 a" text
                                "div > div > div > span" text
                                ".product-list-item__price span" text
                                "h4 a" (fn [m]
                                                              (->> (attr m :href)
                                                                   (str base-url)))
                                ))]
    (log/debugf "books: %s" (vec (take 3 (vec books))))
    {:author (:author entry)
     :books  (compare-books (filter #(not (= "Vypredaná" (:stock %))) books)  wanted-books)
     :wanted-books wanted-books}))

(defmethod get-book-data :antikvariatik
  [_ entry]
  (let [base-url "https://www.antikvariatik.sk/"
        author (first (str/split (:author entry) #" "))
        wanted-books (vec (wanted-books (:wanted-books entry)))
        _ (log/debugf "search: %s" (str base-url "?podstranka=vyhladat&autor=" author))
        books (-> (str base-url "?podstranka=vyhladat&autor=" author)
                  (slurp :encoding "utf-8")
                  parse
                  (extract-from ".index_obsah_vnutri_kniha" [:title :author :url :price]
                                "h2.index_obsah_vnutri_kniha_nazov a" text
                                "div.index_obsah_vnutri_kniha_autor a" text
                                "h2.index_obsah_vnutri_kniha_nazov a" (fn [m]
                                                                        (->> (attr m :href)
                                                                             (str base-url)))
                                "div.index_obsah_vnutri_kniha_cena" text))]
    {:author (:author entry)
     :books  (compare-books books wanted-books)
     :wanted-books wanted-books}))

(defmethod get-book-data :ulovknihu
  [_ entry]
  (let [base-url "https://ulovknihu.cz/hledat?region%5B%5D=15&region%5B%5D=16&region%5B%5D=17&region%5B%5D=18&region%5B%5D=19&region%5B%5D=20&region%5B%5D=21&region%5B%5D=22&stone=0&also_sold=0&price_min=&price_max=&sort=2&q="
        author (second (str/split (:author entry) #" "))
        wanted-books (vec (wanted-books (:wanted-books entry)))
        _ (log/debugf "search: %s" (str base-url author))
        books (-> (str base-url author)
                  (slurp :encoding "utf-8")
                  parse
                  (extract-from ".searchList__product" [:title :author :url :price :vendor]
                                ".searchList__product__info h2 a" #(-> % text (str/split #"\(") first)
                                ".searchList__product__info__autor a" text
                                ".searchList__product__info h2 a" (fn [m] (attr m :href))
                                ".searchList__product__vendor__bottom__price" #(-> % text (str/split #"\(") first)
                                ".searchList__product__vendor__link" text))]
    (log/debugf "books: %s" (vec (take 3 (vec books))))
    {:author (:author entry)
     :books  (compare-books books wanted-books)
     :wanted-books wanted-books}))

;https://ulovknihu.cz/hledat?q=ivan%20kadlecik&region%5B%5D=15&region%5B%5D=16&region%5B%5D=17&region%5B%5D=18&region%5B%5D=19&region%5B%5D=20&region%5B%5D=21&region%5B%5D=22&stone=0&also_sold=0&price_min=&price_max=&sort=2


(defn scan-search [shop]
  (log/debugf "shop: %s" shop)
  (let [entries (wanted-authors (contains?  #{:antikvariatik :cierne-na-bielom} shop))]
    (for [entry entries]
      (get-book-data shop entry))))