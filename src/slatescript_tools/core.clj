(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.xml :as xml]))

(declare get-content)

(defn get-content-from-vector
  "iterates through content vector"
  [acc content-vector]
  (if (nil? content-vector)
    (str acc "\n")
    (str acc (clojure.string/join "" (map #(get-content "" %) content-vector)))))

(defn get-content 
  "gets content from xml, adds to acc"
  [acc xml]
  (let [contents (:content xml)
        first-elem (first contents)]
    (if (string? first-elem)
      (str acc first-elem "\n")
      (get-content-from-vector acc contents))))

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
(-> document xml/parse)

