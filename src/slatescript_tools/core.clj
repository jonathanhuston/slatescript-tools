(ns slatescript-tools.core
  (:gen-class)
  (:require [slatescript-tools.plain-text :refer [plain-text]]
            [slatescript-tools.shell :refer [trim-ext create-xml remove-xml create-docx]]
            [slatescript-tools.validate :refer [parens checknums]]))


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
   plain-text))


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
  (let [result 
        (->
         docx
         create-xml
         parens)]
    (remove-xml docx)
    result))

(defn check-numbers
  "checks whether numbers are identical in two docx"
  [docx1 docx2]
  (let [xml1 (create-xml docx1)
        xml2 (create-xml docx2)
        result (checknums xml1 xml2)]
    (remove-xml docx1)
    (remove-xml docx2)
    result))

(defn -main 
  "launches save-as-txt, check-parens, and check-numbers tools"
  [tool & args]
  (case tool
    "save-as-txt" (save-as-txt (first args))
    "check-parens" (prn (check-parens (first args)))
    "check-numbers" (run! prn (check-numbers (first args) (second args)))
    (println "Usage: slatescript-tools <save-as-txt|check-parens|check-numbers> docx1 [docx2]")
  ))
