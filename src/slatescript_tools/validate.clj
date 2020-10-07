(ns slatescript-tools.validate
  (:require [slatescript-tools.plain-text :refer [plain-text]]))


(defn- acc-doubles
  "given remaining parens, accumulate vector of consecutive parens of same type"
  [p type index acc]
  (cond
    (= (count p) 1) (if (and (= type \() (= (first p) \()) ; last paren open
                      (conj acc (inc index))
                      acc)
    (not= type (first p)) (recur (rest p) type index acc) ; skip type
    (= (first p) (second p)) (recur (rest p) type (inc index) (conj acc (inc index))) ; double
    :else (recur (rest p) type (inc index) acc))) ; single


(defn- find-unbalanced
  "given locations of all parens, returns map of indexes of unbalanced parens"
  [all-parens]
  {:open (acc-doubles all-parens \( 0 []), 
   :closed (acc-doubles all-parens \) 1 [])})

(defn parens 
  "finds unbalanced parens in xml file"
  [xml-file]
  (->>
   xml-file
   plain-text
   (filter #(some #{%} [\( \)]))
   find-unbalanced))
