(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.xml :as xml]))


(declare get-content)

(defn get-content-from-vector
  "iterates through content vector"
  [acc tag contents]
  (let [joined (clojure.string/join "" (map #(get-content "" %) contents))]
    (if (= tag :w:p)
      (str acc joined "\n")
      (str acc joined))))

(defn get-content 
  "gets content from xml, adds to acc"
  [acc xml]
  (let [tag (:tag xml)
        contents (:content xml)
        first-content (first contents)]
    (case tag
      (:w:sectPr :w:rPr) acc
      (if (string? first-content)
        (str acc first-content)
        (get-content-from-vector acc tag contents)))))

(defn plain-text
  "gets plain text from document-xml"
  [document-xml]
  (->>
   document-xml
   xml/parse
   (get-content "")))




; DEV: generic file name
(def document "resources/word/document.xml")

; DEV: print plain-text
(defn -main []
 (->
  document
  plain-text
  print))

(-main)


; DEV: parse document.html
(comment 
  (-> document xml/parse)
  )

