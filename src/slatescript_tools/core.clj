(ns slatescript-tools.core
  (:gen-class)
  (:require [slatescript-tools.plain-text :refer [plain-text]]
            [slatescript-tools.shell :refer [trim-ext create-xml remove-xml create-docx]]
            [slatescript-tools.validate :refer [parens]]))


; DEV
(defn- parse-xml
  "parse xml file only"
  [xml-file]
  (clojure.xml/parse xml-file))

; DEV
(defn- display-plain-text
  "given xml file, display plain text"
  [xml-file]
  (->
   xml-file
   plain-text
   println))


(defn save-as-txt
  "save body of docx as plain text"
  [docx]
  (->>
   docx
   create-xml
   plain-text
   (spit (str (trim-ext docx) ".txt")))
  (remove-xml docx))

(defn check-parens
  "check whether parens are balanced in docx"
  [docx]
  (->
   docx
   create-xml
   parens
   println)
  (remove-xml docx))

(defn -main []
  (check-parens "resources/mixed.docx"))

(-main)
