(ns slatescript-tools.check-numbers
  (:require [clojure.string :refer [trim]]
            [clojure.data :refer [diff]]
            [slatescript-tools.plain-text :refer [plain-text]]))


(def slice 10)

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

(defn check-numbers
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
