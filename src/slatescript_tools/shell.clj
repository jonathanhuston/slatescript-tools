(ns slatescript-tools.shell
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]))


(defn- bash 
  "executes bash command"
  [command]
  (sh "bash" "-c" command))

(defn trim-ext 
  "trims extension from filename"
  [filename]
  (let [dot (.lastIndexOf filename ".")]
    (if (pos? dot) (subs filename 0 dot) filename)))

(defn- get-root 
  "gets root of filename without extension"
  [filename]
  (->
    filename
    io/file
    .getName
    trim-ext))

; TODO: return map of xml files
(defn create-xml 
  "converts docx into folder with xml files, returns xml filename of body"
  [docx]
  (let [dirname (.getParent (io/file docx))
        folder (get-root docx)]
    (bash (str "unzip -o \"" docx "\" -d \"" dirname "/" folder "\""))
    (str (trim-ext docx) "/word/document.xml")))

(defn remove-xml
  "removes xml folder corresponding to docx"
  [docx]
  (let [folder (trim-ext docx)]
    (bash (str "rm -rf \"" folder "\""))))

(defn create-docx 
  "converts folder with xml files into docx
  deletes folder if delete? flag set (default true)"
  ([folder]
   (create-docx folder true))
  ([folder delete?]
   (let [basename (.getName (io/file folder))
         docx (str "\"" basename ".docx\"")
         backup (str "\"" basename " copy.docx\"")]
     (bash (str "cd \"" folder "\";"
                "zip -r " docx " *;"
                "cp ../" docx " ../" backup ";"
                "mv " docx " ..;"
                "cd .."))
     (when delete? (bash (str "rm -rf \"" folder "\""))))))
