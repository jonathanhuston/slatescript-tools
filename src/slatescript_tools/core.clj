(ns slatescript-tools.core
  (:gen-class)
  (:require [slatescript-tools.plain-text :refer [plain-text]]))

; DEV: generic file name
(def document "resources/word/document.xml")
(def text-file "resources/document.txt")

; DEV: save as plain-text
(defn -main []
 (->>
  document
  plain-text
  (spit text-file)))

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