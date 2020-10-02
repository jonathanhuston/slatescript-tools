(ns slatescript-tools.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.xml :as xml]))

(def breaking-tags #{:w:p :w:br})
(def ignore-tags #{:w:sectPr :w:rFonts :w:color :w:w :w:sz})

(declare get-content)

(defn get-content-from-vector
  "iterates through content vector, eliminates multiple spaces"
  [acc tag contents]
  (let [clean (->
               (str/join "" (map #(get-content "" %) contents))
               (str/replace #" +" " ")
               (str/replace #"\n " "\n"))]
    (if (some #(= tag %) breaking-tags)
      (str acc clean "\n")
      (str acc clean))))

(defn space? 
  "add special space?"
  [tag attrs contents]
  (and 
   (nil? contents)
   (or (= tag :w:spacing) (= attrs {:xml:space "preserve"}))))

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
        (space? tag attrs contents) (str acc " ")
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