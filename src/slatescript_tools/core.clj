(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.xml :as xml]))

(def breaking-tags #{:w:p :w:br})
(def ignore-tags #{:w:sectPr :w:rPr})

(declare get-content)

(defn get-content-from-vector
  "iterates through content vector"
  [acc tag contents]
  (let [joined (str/join "" (map #(get-content "" %) contents))]
    (if (some #(= tag %) breaking-tags)
      (str acc joined "\n")
      (str acc joined))))

(defn get-content 
  "gets content from xml, adds to acc"
  [acc xml]
  (let [tag (:tag xml)
        attrs (:attrs xml)
        contents (:content xml)
        first-content (first contents)]
    (if (some #(= tag %) ignore-tags)
      acc
      (cond
        (string? first-content) (str acc first-content)
        (and (nil? contents) (= attrs {:xml:space "preserve"})) (str acc " ")
        :else (get-content-from-vector acc tag contents)))))

(defn plain-text
  "gets plain text from document-xml"
  [document-xml]
  (->>
   document-xml
   xml/parse
   (get-content "")))




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


; DEV: parse document.html only
(comment 
  (-> document xml/parse)
  )

; DEV: display plain-text
(comment
  (->
   document
   plain-text
   println)
  )