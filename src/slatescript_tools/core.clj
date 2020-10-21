(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [slatescript-tools.shell :refer [trim-ext create-xml remove-xml]]
            [slatescript-tools.plain-text :refer [plain-text]]
            [slatescript-tools.check-parens :refer [check-parens]]
            [slatescript-tools.check-numbers :refer [check-numbers]]))


(defn txt
  "save body of docx as plain text"
  [docx]
  (->>
   docx
   create-xml
   plain-text
   (spit (str (trim-ext docx) ".txt")))
  (remove-xml docx))

(defn parens
  "check whether parens are balanced in docx"
  [docx]
  (let [result 
        (->
         docx
         create-xml
         check-parens)]
    (remove-xml docx)
    result))

(defn checknums
  "checks whether numbers are identical in two docx"
  [docx1 docx2]
  (let [xml1 (create-xml docx1)
        xml2 (create-xml docx2)
        result (check-numbers xml1 xml2)]
    (remove-xml docx1)
    (remove-xml docx2)
    result))

(defn- validate-args
  "validates command line arguments"
  [tool args num]
  (let [arity (count args)
        sing-plur (if (= num 1) "argument," "arguments,")]
    (cond 
      (not= arity num) (println "Should be" num sing-plur arity "passed.")
      (not-every? true? (map #(.exists (io/file %)) args)) (println "File not found.")
      :else (apply tool args))))

(defn- print-help-text 
  "print CLI help text"
  []
  (println 
"Tools for validating translated Word documents

Usage:
   slatescript-tools <tool> docx1 [docx2]
            
Tools:
   txt docx1:              converts docx1 to UTF-8 text
   parens docx1:           checks whether parentheses are balanced
   checknums docx1 docx2:  checks whether digits are the same in docx1 and docx2"))

(defn -main 
  "launches txt, parens, and checknums tools"
  ([]
   (-main "help"))
  ([tool & args]
   (case tool
     "txt" (validate-args txt args 1)
     "parens" (run! prn (validate-args parens args 1))
     "checknums" (run! prn (validate-args checknums args 2))
     (print-help-text))
   (shutdown-agents)))
