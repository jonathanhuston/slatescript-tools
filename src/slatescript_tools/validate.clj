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
  (let [paragraphs (str/split text #"\n\n+")]
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
  [source-chunk target-chunk chunk-num api-key]
  (let [prompt (build-prompt source-chunk target-chunk)]
    (str "## Chunk " chunk-num "\n\n"
         (call-claude-api prompt api-key)
         "\n\n")))

(defn- validate-chunked
  "validates translation by analyzing chunks separately"
  [source-text target-text api-key]
  (let [source-chunks (chunk-text source-text 20000)
        target-chunks (chunk-text target-text 20000)
        num-chunks (max (count source-chunks) (count target-chunks))]
    (str "# Translation Validation Report (Chunked Analysis)\n\n"
         "*Note: Document was analyzed in " num-chunks " chunk(s) due to length.*\n\n"
         (apply str
                (for [i (range num-chunks)]
                  (validate-chunk-pair
                   (get source-chunks i "")
                   (get target-chunks i "")
                   (inc i)
                   api-key))))))

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
