(ns slatescript-tools.core
  (:gen-class)
  (:require [slatescript-tools.plain-text :refer [plain-text]]
            [slatescript-tools.clipboard :refer [paste-clipboard]]
            [slatescript-tools.shell :refer [unzip-docx create-docx]]))

; DEV: generic file name
(def document "resources/word/document.xml")
(def text-file "resources/document.txt")
(def doc1 "resources/doc1.docx")
(def folder "resources/doc1")

; DEV: save as plain-text
(defn -main []
    (create-docx "resources/fma"))

(-main)


; DEV: parse document.xml only
(comment 
  (-> document clojure.xml/parse)
  )

; DEV: display plain-text
(comment
  (->
   document
   plain-text
   println)
  )

; DEV: save plain-text
(comment
  (->>
   document
   plain-text
   (spit text-file))
  )
