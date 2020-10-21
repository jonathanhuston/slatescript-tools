(ns slatescript-tools.check-parens
  (:require [slatescript-tools.plain-text :refer [plain-text]]))


(defn- acc-doubles
  "given remaining parens, accumulate vector of consecutive parens of same type"
  [p type index acc]
  (cond
    (nil? p) acc                                          ; docx with no parens
    (= (count p) 1) (if (and (= type \() (= (first p) \()) ; base case
                      (conj acc (inc index))              ; last paren open
                      acc)                                ; last paren closed
    (not= type (first p)) (recur (rest p) type index acc)  ; skip type
    (= (first p) (second p)) (recur (rest p) type (inc index) (conj acc (inc index))) ; double
    :else (recur (rest p) type (inc index) acc)))         ; single

(defn- find-unbalanced
  "given locations of all parens, returns {:open i :closed j} of indexes of unbalanced parens"
  [all-parens]
  {:open (acc-doubles all-parens \( 0 [])
   :closed (acc-doubles all-parens \) 1 [])})

(defn check-parens
  "given xml file, returns {:open i :closed j} of indexes of unbalanced parens"
  [xml-file]
  (->>
   xml-file
   plain-text
   (filter #(some #{%} [\( \)]))
   seq
   find-unbalanced))
