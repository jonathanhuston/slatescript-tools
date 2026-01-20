(ns slatescript-tools.validate
  (:require [slatescript-tools.plain-text :refer [plain-text]]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def ^:private api-endpoint "https://api.anthropic.com/v1/messages")
(def ^:private model "claude-sonnet-4-5-20250929")
(def ^:private max-tokens 16000)

(defn- get-api-key
  "retrieves Anthropic API key from environment variable"
  []
  (or (System/getenv "ANTHROPIC_API_KEY")
      (throw (Exception. "ANTHROPIC_API_KEY environment variable not set"))))

(defn- build-prompt
  "builds the prompt for Claude to analyze translation omissions"
  [source-text target-text]
  (str "I need you to analyze a translation for completeness. Compare the SOURCE text with the TARGET text and identify any information, details, or nuances from the SOURCE that are missing or omitted in the TARGET.\n\n"
       "Please provide your analysis in markdown format with the following structure:\n\n"
       "# Translation Validation Report\n\n"
       "## Summary\n"
       "[Brief overview of translation quality]\n\n"
       "## Omissions Found\n"
       "[List each omission with:\n"
       "- What was omitted\n"
       "- Where it appears in the source\n"
       "- Why it matters (if significant)]\n\n"
       "If no omissions are found, state that clearly.\n\n"
       "---\n\n"
       "**SOURCE TEXT:**\n\n"
       source-text
       "\n\n---\n\n"
       "**TARGET TEXT:**\n\n"
       target-text))

(defn- chunk-text
  "splits text into chunks of approximately max-chars, breaking at paragraph boundaries"
  [text max-chars]
  (let [paragraphs (str/split text #"\n+")]
    (loop [chunks []
           current-chunk []
           current-size 0
           remaining paragraphs]
      (if (empty? remaining)
        (if (empty? current-chunk)
          chunks
          (conj chunks (str/join "\n\n" current-chunk)))
        (let [para (first remaining)
              para-size (count para)]
          (if (and (> current-size 0)
                   (> (+ current-size para-size) max-chars))
            (recur (conj chunks (str/join "\n\n" current-chunk))
                   [para]
                   para-size
                   (rest remaining))
            (recur chunks
                   (conj current-chunk para)
                   (+ current-size para-size)
                   (rest remaining))))))))

;; Heading patterns for structural chunking
(def ^:private heading-patterns
  [#"^([A-Z]\.[\d.]+)\s+"                  ; A.1. or A.2.1 or B.2.1.1
   #"^([A-Z])\s+[A-Z]"                     ; A Energiegewinnung (single letter followed by title)
   #"^(\d+\.[\d.]*)\s+"                    ; 1. or 1.2. or 1.2.3.
   #"^([IVXLC]+\.)\s+"                     ; I. II. III. IV. etc.
   #"^([a-z]\))\s+"                        ; a) b) c)
   #"^(\(\d+\))\s+"                        ; (1) (2) (3)
   #"(?i)^(section\s+\d+)"                 ; Section 1, Section 2
   #"(?i)^(chapter\s+\d+)"                 ; Chapter 1, Chapter 2
   #"(?i)^(part\s+[IVXLC\d]+)"             ; Part I, Part 1
   #"(?i)^(article\s+\d+)"                 ; Article 1, Article 2
   #"(?i)^(§\s*\d+)"])                     ; § 1, § 2

(defn- strip-formatting-prefixes
  "removes Word formatting artifacts from beginning of text"
  [text]
  (-> text
      str/trim
      ;; Remove common Word alignment/formatting words that get included
      (str/replace #"^(left|right|center|justify|both)+" "")
      str/trim))

(defn- extract-heading-id
  "extracts a normalized identifier from a heading line, or nil if not a heading"
  [line]
  (let [cleaned (strip-formatting-prefixes line)]
    (when (< (count cleaned) 100)  ; headings are typically short
      (some (fn [pattern]
              (when-let [match (re-find pattern cleaned)]
                (-> (if (vector? match) (second match) match)
                    str/lower-case
                    (str/replace #"\s+" "")
                    (str/replace #"\.$" ""))))
            heading-patterns))))

(defn- find-headings
  "finds all headings in text with their paragraph indices and identifiers
   returns: [{:index n :id \"1.2\" :text \"1.2 Introduction\"} ...]"
  [text]
  (let [paragraphs (str/split text #"\n+")]
    (->> paragraphs
         (map-indexed (fn [idx para]
                        (when-let [id (extract-heading-id para)]
                          {:index idx :id id :text (subs para 0 (min 60 (count para)))})))
         (filter some?)
         vec)))

(defn- match-headings
  "finds headings that appear in both source and target documents
   returns: sequence of {:id :source-index :target-index} for matched headings"
  [source-headings target-headings]
  (let [target-by-id (group-by :id target-headings)]
    (->> source-headings
         (keep (fn [{:keys [id index]}]
                 (when-let [target-match (first (get target-by-id id))]
                   {:id id
                    :source-index index
                    :target-index (:index target-match)})))
         ;; ensure headings are in order (by source index)
         (sort-by :source-index))))

(defn- select-chunk-boundaries
  "selects which matched headings to use as chunk boundaries based on target size
   aims for chunks of approximately target-chars characters"
  [matched-headings source-paragraphs target-paragraphs target-chars]
  (if (empty? matched-headings)
    []
    (loop [boundaries [0]  ; start with beginning
           remaining matched-headings
           last-source-idx 0
           accumulated-chars 0]
      (if (empty? remaining)
        boundaries
        (let [{:keys [source-index target-index]} (first remaining)
              ;; estimate chars from last boundary to this heading
              source-slice (->> source-paragraphs
                                (drop last-source-idx)
                                (take (- source-index last-source-idx))
                                (str/join "\n\n"))
              target-slice (->> target-paragraphs
                                (drop last-source-idx)
                                (take (- target-index last-source-idx))
                                (str/join "\n\n"))
              slice-chars (+ (count source-slice) (count target-slice))
              new-accumulated (+ accumulated-chars slice-chars)]
          (if (>= new-accumulated target-chars)
            ;; this heading becomes a chunk boundary
            (recur (conj boundaries source-index)
                   (rest remaining)
                   source-index
                   0)
            ;; keep accumulating
            (recur boundaries
                   (rest remaining)
                   last-source-idx
                   new-accumulated)))))))

(defn- chunk-by-structure
  "chunks source and target texts at matched heading boundaries
   returns: [[source-chunk1 target-chunk1] [source-chunk2 target-chunk2] ...]"
  [source-text target-text target-chars-per-chunk]
  (let [source-paragraphs (str/split source-text #"\n+")
        target-paragraphs (str/split target-text #"\n+")
        source-headings (find-headings source-text)
        target-headings (find-headings target-text)
        matched (match-headings source-headings target-headings)]
    (if (< (count matched) 2)
      ;; not enough structural markers, return nil to signal fallback
      nil
      (let [;; build map of source heading index -> target heading index
            source-to-target (->> matched
                                  (map (fn [{:keys [source-index target-index]}]
                                         [source-index target-index]))
                                  (into {}))
            boundaries (select-chunk-boundaries matched source-paragraphs
                                                target-paragraphs target-chars-per-chunk)
            ;; add end boundaries
            boundaries (conj boundaries (count source-paragraphs))]
        (->> (partition 2 1 boundaries)
             (map (fn [[start end]]
                    (let [;; find corresponding target indices
                          target-start (get source-to-target start 0)
                          target-end (get source-to-target end (count target-paragraphs))
                          source-chunk (->> source-paragraphs
                                            (drop start)
                                            (take (- end start))
                                            (str/join "\n\n"))
                          target-chunk (->> target-paragraphs
                                            (drop target-start)
                                            (take (- target-end target-start))
                                            (str/join "\n\n"))]
                      [source-chunk target-chunk])))
             vec)))))

(defn- should-chunk?
  "determines if texts should be chunked based on combined length"
  [source-text target-text]
  (> (+ (count source-text) (count target-text)) 50000))

(defn- call-claude-api
  "makes API call to Claude and returns the response text"
  [prompt api-key]
  (try
    (let [response (http/post api-endpoint
                              {:headers {"x-api-key" api-key
                                         "anthropic-version" "2023-06-01"
                                         "content-type" "application/json"}
                               :body (json/generate-string
                                      {:model model
                                       :max_tokens max-tokens
                                       :messages [{:role "user"
                                                   :content prompt}]})
                               :throw-exceptions true})
          body (json/parse-string (:body response) true)]
      (-> body :content first :text))
    (catch Exception e
      (throw (Exception. (str "API call failed: " (.getMessage e)))))))

(defn- validate-chunk-pair
  "validates a single pair of source and target chunks"
  [source-chunk target-chunk chunk-num total-chunks api-key]
  (println (str "Analyzing chunk " chunk-num " of " total-chunks "..."))
  (let [prompt (build-prompt source-chunk target-chunk)]
    (str "## Chunk " chunk-num " of " total-chunks "\n\n"
         (call-claude-api prompt api-key)
         "\n\n")))

(defn- validate-with-chunks
  "validates using provided chunk pairs"
  [chunk-pairs api-key chunking-method]
  (let [num-chunks (count chunk-pairs)]
    (str "# Translation Validation Report (Chunked Analysis)\n\n"
         "*Note: Document was analyzed in " num-chunks " chunk(s) using " chunking-method ".*\n\n"
         (apply str
                (map-indexed
                 (fn [i [source-chunk target-chunk]]
                   (validate-chunk-pair source-chunk target-chunk (inc i) num-chunks api-key))
                 chunk-pairs)))))

(defn- validate-chunked
  "validates translation by analyzing chunks separately
   tries structural chunking first, falls back to paragraph-based"
  [source-text target-text api-key]
  (if-let [structural-chunks (chunk-by-structure source-text target-text 40000)]
    (do
      (println (str "Found " (count structural-chunks) " structural chunk(s) based on headings..."))
      (validate-with-chunks structural-chunks api-key "structural alignment (headings)"))
    (do
      (println "No structural markers found, using paragraph-based chunking...")
      (let [source-chunks (chunk-text source-text 20000)
            target-chunks (chunk-text target-text 20000)
            num-chunks (max (count source-chunks) (count target-chunks))
            chunk-pairs (for [i (range num-chunks)]
                          [(get source-chunks i "")
                           (get target-chunks i "")])]
        (validate-with-chunks chunk-pairs api-key "paragraph boundaries")))))

(defn- validate-whole
  "validates translation by analyzing entire documents"
  [source-text target-text api-key]
  (let [prompt (build-prompt source-text target-text)]
    (call-claude-api prompt api-key)))

(defn validate
  "validates docx2 translation against docx1 source using Claude API
   xml1: path to source document.xml
   xml2: path to target document.xml
   returns: markdown-formatted validation report"
  [xml1 xml2]
  (let [source-text (plain-text xml1)
        target-text (plain-text xml2)
        api-key (get-api-key)]
    (if (should-chunk? source-text target-text)
      (validate-chunked source-text target-text api-key)
      (validate-whole source-text target-text api-key))))
