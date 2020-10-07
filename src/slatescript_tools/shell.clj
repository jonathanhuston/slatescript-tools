(ns slatescript-tools.shell
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]))

(defn- bash 
  "executes bash command"
  [command]
  (sh "bash" "-c" command))

(defn- trim-ext 
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

(defn unzip-docx 
  "converts docx into folder with xml files"
  [docx]
  (let [dirname (.getParent (io/file docx))
        folder (get-root docx)]
    (bash (str "unzip -o \"" docx "\" -d \"" dirname "/" folder "\""))))

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
