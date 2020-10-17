(ns slatescript-tools.validate
  (:require [clojure.string :refer [trim]]
            [clojure.data :refer [diff]]
            [slatescript-tools.plain-text :refer [plain-text]]))


; maximum slice for checknums
(def slice 10)

; parens helpers
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
  {:open (acc-doubles all-parens \( 0 []), 
   :closed (acc-doubles all-parens \) 1 [])})

(defn parens 
  "given xml file, returns {:open i :closed j} of indexes of unbalanced parens"
  [xml-file]
  (->>
   xml-file
   plain-text
   (filter #(some #{%} [\( \)]))
   seq
   find-unbalanced))

; checknums helpers
(defn- is-digit? 
  "checks whether c is a digit"
  [c]
  (<= (int \0) (int c) (int \9)))

(defn- safesubs 
  "gets maximum possible substring"
  [s start end]
    (subs s (max start 0) (min end (count s))))

(defn- get-slice
  "gets substring surrounding position +/- slice characters"
  [s pos]
  (safesubs s (- pos slice) (+ pos slice)))

(defn- indexed-digits
  "given string, returns list of digits with string indexes"
  [s]
  (->>
   s
   (map-indexed vector)
   (filter #(is-digit? (second %)))))

(defn- mismatched-positions
  "creates list of mismatched positions in acc"
  [digits diffs acc]
  (cond 
    (empty? digits) acc
    (nil? (first diffs)) (recur (rest digits) (rest diffs) acc)
    (= 1 (count (first diffs))) (recur (rest digits) (rest diffs) acc)
    :else (recur (rest digits) (rest diffs) (conj acc (first digits)))))

(defn- get-context
  "given mismatched positions, returns context of mismatch in string"
  [pos s]
  (->>
   pos
   (map #(trim (get-slice s (first %))))
   dedupe)
  )

(defn checknums
  "compares two xml files, returns [[slices1] [slices2]] of unmatched digits"
  [xml1 xml2]
  (let [s1 (plain-text xml1)
        s2 (plain-text xml2)
        digits1 (indexed-digits s1)
        digits2 (indexed-digits s2)
        [diff1 diff2] (diff digits1 digits2)
        pos1 (mismatched-positions digits1 diff1 [])
        pos2 (mismatched-positions digits2 diff2 [])
        context1 (get-context pos1 s1)
        context2 (get-context pos2 s2)]
    [context1 context2]))
