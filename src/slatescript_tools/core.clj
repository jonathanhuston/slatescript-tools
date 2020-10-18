(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [slatescript-tools.plain-text :refer [plain-text]]
            [slatescript-tools.shell :refer [trim-ext create-xml remove-xml]]
            [slatescript-tools.validate :refer [parens checknums]]))


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
   save-as-txt docx1:          converts docx1 to UTF-8 text
   check-parens docx1:         checks whether parentheses are balanced
   check-numbers docx1 docx2:  checks whether digits are the same in docx1 and docx2"))

(defn -main 
  "launches save-as-txt, check-parens, and check-numbers tools"
  ([]
   (-main "help"))
  ([tool & args]
   (case tool
     "save-as-txt" (validate-args save-as-txt args 1)
     "check-parens" (run! prn (validate-args check-parens args 1))
     "check-numbers" (run! prn (validate-args check-numbers args 2))
     (print-help-text))
   (shutdown-agents)))
