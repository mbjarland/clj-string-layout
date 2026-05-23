(ns clj-string-layout.cli
  "Command-line entry point for formatting CSV, TSV, or whitespace input."
  (:require [clj-string-layout.escape :as escape]
            [clj-string-layout.table :as table]
            [clojure.string :as str])
  (:import [java.io BufferedReader InputStreamReader FileInputStream]))

(set! *warn-on-reflection* true)

(def ^:private input-formats #{:csv :tsv :whitespace})

(def ^:private default-options
  {:input :csv
   :format :plain
   :headers? false
   :escape? true})

(defn- option-keyword [value]
  (keyword (cond-> value
             (str/starts-with? value ":") (subs 1))))

(defn- parse-keyword-option [option value allowed]
  (when-not value
    (throw (ex-info (str option " requires a value")
                    {:type :cli-argument-error
                     :option option})))
  (let [value (option-keyword value)]
    (when-not (contains? allowed value)
      (throw (ex-info (str "Unsupported " option " value")
                      {:type :cli-argument-error
                       :option option
                       :value value
                       :allowed allowed})))
    value))

(defn- parse-int-option [option value]
  (when-not value
    (throw (ex-info (str option " requires a value")
                    {:type :cli-argument-error
                     :option option})))
  (try
    (let [n (Long/parseLong value)]
      (when (neg? n)
        (throw (ex-info (str option " must be a non-negative integer")
                        {:type :cli-argument-error
                         :option option
                         :value value})))
      n)
    (catch NumberFormatException _
      (throw (ex-info (str option " must be a non-negative integer")
                      {:type :cli-argument-error
                       :option option
                       :value value})))))

(defn parse-args
  "Parses CLI arguments into an options map.

  This function is public so callers and tests can reuse the CLI argument
  behavior without invoking System/exit."
  [args]
  (loop [args (seq args)
         options default-options]
    (if-not args
      options
      (let [[arg value & more] args]
        (case arg
          "--help" (recur (next args) (assoc options :help? true))
          "-h" (recur (next args) (assoc options :help? true))
          "--" (recur (next args) options)
          "--headers" (recur (next args) (assoc options :headers? true))
          "--no-headers" (recur (next args) (assoc options :headers? false))
          "--no-escape" (recur (next args) (assoc options :escape? false))
          "--input" (recur more (assoc options :input (parse-keyword-option arg value input-formats)))
          "--from" (recur more (assoc options :input (parse-keyword-option arg value input-formats)))
          "--format" (recur more (assoc options :format (parse-keyword-option arg value (table/formats))))
          "--to" (recur more (assoc options :format (parse-keyword-option arg value (table/formats))))
          "--width" (recur more (assoc options :width (parse-int-option arg value)))
          "--fill" (recur (next args) (assoc options :fill? true))
          (cond
            (= "-" arg)
            (if (:file options)
              (throw (ex-info "Only one input file may be supplied"
                              {:type :cli-argument-error
                               :file arg
                               :existing-file (:file options)}))
              (recur (next args) (assoc options :file arg)))

            (str/starts-with? arg "-")
            (throw (ex-info "Unsupported CLI option"
                            {:type :cli-argument-error
                             :option arg}))

            (:file options)
            (throw (ex-info "Only one input file may be supplied"
                            {:type :cli-argument-error
                             :file arg
                             :existing-file (:file options)}))

            :else
            (recur (next args) (assoc options :file arg))))))))

(defn- append-char! [^StringBuilder sb ch]
  (.append sb ch)
  sb)

(defn parse-csv
  "Parses CSV text into row vectors.

  Handles commas, CR/LF row separators, quoted fields, doubled quotes, and line
  breaks inside quoted fields. The parser is intentionally lenient about text
  after closing quotes so command-line use can handle imperfect CSV exports."
  [text]
  (let [text (str text)
        len (.length text)]
    (loop [idx 0
           rows []
           row []
           field (StringBuilder.)
           quoted? false]
      (if (= idx len)
        (if (and (empty? row) (zero? (.length field)))
          rows
          (conj rows (conj row (str field))))
        (let [ch (.charAt text idx)]
          (cond
            quoted?
            (cond
              (and (= \" ch)
                   (< (inc idx) len)
                   (= \" (.charAt text (inc idx))))
              (recur (+ idx 2) rows row (append-char! field ch) true)

              (= \" ch)
              (recur (inc idx) rows row field false)

              :else
              (recur (inc idx) rows row (append-char! field ch) true))

            (and (= \" ch) (zero? (.length field)))
            (recur (inc idx) rows row field true)

            (= \, ch)
            (recur (inc idx) rows (conj row (str field)) (StringBuilder.) false)

            (= \newline ch)
            (recur (inc idx) (conj rows (conj row (str field))) [] (StringBuilder.) false)

            (= \return ch)
            (let [next-idx (if (and (< (inc idx) len)
                                    (= \newline (.charAt text (inc idx))))
                             (+ idx 2)
                             (inc idx))]
              (recur next-idx (conj rows (conj row (str field))) [] (StringBuilder.) false))

            :else
            (recur (inc idx) rows row (append-char! field ch) false)))))))

(defn- parse-separated-lines [separator-pattern text]
  (mapv #(str/split % separator-pattern -1) (str/split-lines (str text))))

(defn- parse-whitespace [text]
  (->> (str/split-lines (str text))
       (keep (fn [line]
               (let [line (str/trim line)]
                 (when-not (str/blank? line)
                   (str/split line #"\s+")))))
       vec))

(defn parse-input
  "Parses input text according to input-format.

  input-format must be one of :csv, :tsv, or :whitespace."
  [input-format text]
  (case input-format
    :csv (parse-csv text)
    :tsv (parse-separated-lines #"\t" text)
    :whitespace (parse-whitespace text)))

;; --- Streaming parsers ----------------------------------------------------
;; Each *-row-seq function reads from a java.io.Reader and yields rows as a
;; lazy seq, suitable for piping through core/layout-into! without buffering
;; the full input. The CLI auto-uses these when the target format does not
;; need column-width computation.

(defn- unbalanced-quotes?
  "True when line has an odd number of double-quote characters, meaning a
  quoted field is still open and the next physical line is part of the same
  logical CSV row."
  [^String line]
  (odd? (count (filter #(= % \") line))))

(defn csv-row-seq
  "Returns a lazy sequence of row vectors read from a java.io.Reader.

  Handles multi-line quoted fields by assembling physical lines until the
  running quote count is balanced, then parsing the assembled logical line
  with parse-csv. The reader is not closed; wrap in with-open at the call
  site."
  [^java.io.Reader r]
  (let [br (if (instance? java.io.BufferedReader r)
             ^java.io.BufferedReader r
             (java.io.BufferedReader. r))]
    (letfn [(step []
              (lazy-seq
                (when-let [first-line (.readLine br)]
                  (let [logical (loop [acc first-line]
                                  (if (unbalanced-quotes? acc)
                                    (if-let [more (.readLine br)]
                                      (recur (str acc "\n" more))
                                      acc)
                                    acc))
                        [row & _] (parse-csv logical)]
                    (cons (or row []) (step))))))]
      (step))))

(defn tsv-row-seq
  "Lazy sequence of tab-separated row vectors read from a java.io.Reader."
  [^java.io.Reader r]
  (let [br (if (instance? java.io.BufferedReader r)
             ^java.io.BufferedReader r
             (java.io.BufferedReader. r))]
    (letfn [(step []
              (lazy-seq
                (when-let [line (.readLine br)]
                  (cons (str/split line #"\t" -1) (step)))))]
      (step))))

(defn whitespace-row-seq
  "Lazy sequence of whitespace-split row vectors read from a java.io.Reader.

  Skips blank lines and trims leading/trailing whitespace, matching the
  eager parse-whitespace behaviour."
  [^java.io.Reader r]
  (let [br (if (instance? java.io.BufferedReader r)
             ^java.io.BufferedReader r
             (java.io.BufferedReader. r))]
    (letfn [(step []
              (lazy-seq
                (when-let [line (.readLine br)]
                  (let [trimmed (str/trim line)]
                    (if (str/blank? trimmed)
                      (step)
                      (cons (str/split trimmed #"\s+") (step)))))))]
      (step))))

(defn row-seq
  "Dispatches to the streaming parser for input-format and returns a lazy
  sequence of row vectors read from r."
  [input-format ^java.io.Reader r]
  (case input-format
    :csv (csv-row-seq r)
    :tsv (tsv-row-seq r)
    :whitespace (whitespace-row-seq r)))

(defn render
  "Renders parsed input text according to CLI-style options.

  Options are the same keys returned by parse-args, plus optional :width
  (integer target width forwarded to fill-aware formats) and :display-width
  (a function from string to display width, useful when the caller invokes
  render programmatically with ANSI or wide-glyph data). Returns a vector of
  output lines and does not print or exit."
  [{:keys [input format headers? escape? width display-width fill?]
    :or {input :csv format :plain escape? true}}
   text]
  (let [rows (parse-input input text)]
    (when (empty? rows)
      (throw (ex-info "Input contains no rows"
                      {:type :cli-input-error
                       :input input})))
    (let [[headers rows] (if headers? [(first rows) (subvec rows 1)] [nil rows])]
      (table/table (cond-> {:format format
                            :rows rows
                            :escape? escape?}
                     headers (assoc :headers headers)
                     width (assoc :width width)
                     fill? (assoc :fill? true)
                     display-width (assoc :display-width display-width))))))

(defn- format-list [values]
  (str/join ", " (map name (sort-by name values))))

(defn usage
  "Returns the human-readable usage string for the CLI.

  Reflects whatever input and output format names are currently registered
  by clj-string-layout.cli/input-formats and clj-string-layout.table/formats."
  []
  (str "Usage: clojure -M:cli -- [options] [file|-]\n\n"
       "Reads stdin by default and writes formatted table lines to stdout.\n\n"
       "Options:\n"
       "  --input, --from FORMAT   Input format: " (format-list input-formats) " (default: csv)\n"
       "  --format, --to FORMAT    Output format: " (format-list (table/formats)) " (default: plain)\n"
       "  --width N                Target total width (paired with --fill)\n"
       "  --fill                   Expand fill-aware formats toward --width\n"
       "  --headers                Treat the first input row as headers\n"
       "  --no-headers             Treat every input row as data\n"
       "  --no-escape              Disable output-format escaping\n"
       "  -h, --help               Show this help\n"))

(defn- input-text [{:keys [file]}]
  (if (and file (not= "-" file))
    (slurp file)
    (slurp *in*)))

(defn- open-reader ^java.io.BufferedReader [{:keys [file]}]
  (if (and file (not= "-" file))
    (BufferedReader. (InputStreamReader. (FileInputStream. ^String file) "UTF-8"))
    (BufferedReader. *in*)))

;; Width-free output formats can stream row-by-row without buffering the
;; input or computing column widths. The CLI auto-uses this path on these
;; formats, removing the 1M-row OOM cliff documented in doc/cli.md. Going
;; through the layout engine would still force a full row scan to compute
;; (unused) widths, so for these three formats we emit directly.
(def ^:private streamable-formats
  {:csv  {:separator ","   :escape escape/csv-cell}
   :tsv  {:separator "\t"  :escape escape/tsv-cell}
   :pipe {:separator "|"   :escape identity}})

(defn- write-separated-row [^java.io.Writer w ^String sep escape-fn row]
  (loop [first? true items row]
    (when (seq items)
      (when-not first? (.write w sep))
      (.write w ^String (escape-fn (first items)))
      (recur false (next items))))
  (.write w "\n"))

(defn- stream-render
  "Streams input through the formatter row-by-row, writing to *out*.

  Returns nil. Only invoked for keys of streamable-formats; other formats
  fall back to the eager render+println loop. Bypasses the layout engine
  for these three formats because their cells are emitted verbatim with a
  fixed separator — no width computation is needed and going through the
  engine would force a full row scan."
  [{:keys [input format escape?] :as options}]
  (let [{:keys [separator escape]} (streamable-formats format)
        escape-fn (if (false? escape?) identity escape)
        ^java.io.Writer out *out*]
    (with-open [r (open-reader options)]
      (doseq [row (row-seq input r)]
        (write-separated-row out separator escape-fn row))
      (.flush out))))

(defn -main [& args]
  (try
    (let [options (parse-args args)]
      (cond
        (:help? options)
        (println (usage))

        (contains? streamable-formats (:format options))
        (stream-render options)

        :else
        (doseq [line (render options (input-text options))]
          (println line))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (ex-message e)))
      (System/exit 2))))
