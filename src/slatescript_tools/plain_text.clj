(ns slatescript-tools.plain-text
  (:require [clojure.string :as str]
            [clojure.xml :as xml]))


(def breaking-tags #{:w:p :w:br})
(def ignore-tags #{:w:sectPr :w:rFonts :w:color :w:w :w:sz})

(declare get-content)

(defn- get-content-from-vector
  "iterates through content vector, eliminates multiple spaces"
  [tag contents]
  (let [clean (->
               (reduce #(str %1 (get-content %2)) "" contents)
               (str/replace #" +" " ")
               (str/replace #"\n " "\n"))]
    (if (some #(= tag %) breaking-tags)
      (str clean "\n")
      clean)))

(defn- space?
  "add special space?"
  [tag attrs contents]
  (and
   (nil? contents)
   (or (= tag :w:spacing) (= attrs {:xml:space "preserve"}))))

(defn- get-content
  "gets content from xml"
  [xml]
  (let [tag (:tag xml)
        attrs (:attrs xml)
        contents (:content xml)
        first-content (first contents)]
    (cond
      (some #(= tag %) ignore-tags) ""
      (string? first-content) first-content
      (space? tag attrs contents) " "
      :else (get-content-from-vector tag contents))))

(defn plain-text
  "gets plain text from xml file"
  [xml-file]
  (->
   xml-file
   xml/parse
   get-content))
