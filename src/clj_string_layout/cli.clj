(ns clj-string-layout.cli
  "Command-line entry point for formatting CSV, TSV, or whitespace input."
  (:require [clj-string-layout.table :as table]
            [clojure.string :as str]))

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

(defn render
  "Renders parsed input text according to CLI-style options.

  Options are the same keys returned by parse-args. Returns a vector of output
  lines and does not print or exit."
  [{:keys [input format headers? escape?]
    :or {input :csv format :plain escape? true}
    :as options}
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
                     (:display-width options) (assoc :display-width (:display-width options)))))))

(defn- format-list [values]
  (str/join ", " (map name (sort-by name values))))

(defn usage []
  (str "Usage: clojure -M:cli -- [options] [file|-]\n\n"
       "Reads stdin by default and writes formatted table lines to stdout.\n\n"
       "Options:\n"
       "  --input, --from FORMAT   Input format: " (format-list input-formats) " (default: csv)\n"
       "  --format, --to FORMAT    Output format: " (format-list (table/formats)) " (default: plain)\n"
       "  --headers                Treat the first input row as headers\n"
       "  --no-headers             Treat every input row as data\n"
       "  --no-escape              Disable output-format escaping\n"
       "  -h, --help               Show this help\n"))

(defn- input-text [{:keys [file]}]
  (if (and file (not= "-" file))
    (slurp file)
    (slurp *in*)))

(defn -main [& args]
  (try
    (let [options (parse-args args)]
      (if (:help? options)
        (println (usage))
        (doseq [line (render options (input-text options))]
          (println line))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (ex-message e)))
      (System/exit 2))))
