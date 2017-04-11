(ns product-tracker.find-test
  (:require [clojure.test :refer :all]
            [product-tracker.find :refer :all]))

(def books '({:url "url-book-1"}
              {:url "url-book-2"}
              {:url "url-book-3"}
              {:url "url-book-4"}
              {:url "url-book-5"}))

(def last-book {:url "url-book-3"})

(def new-books '({:url "url-book-1"}
                  {:url "url-book-2"}))

(deftest get-last-batch-returns-correct-items
  (is (= (set new-books) (set (get-last-batch last-book books)))))

(deftest get-last-batch-work-with-no-last-item
  (is (= (set books) (set (get-last-batch nil books)))))