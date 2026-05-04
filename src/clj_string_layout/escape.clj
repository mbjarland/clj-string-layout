(ns clj-string-layout.escape
  "Helpers for escaping cell values before rendering markup-oriented layouts.

  The HTML and Markdown presets intentionally emit cell contents verbatim so
  callers can decide whether values are already trusted. Use this namespace to
  escape untrusted or arbitrary data before passing it to layout or layout-seq."
  (:require [clojure.string :as str]))

(defn- cell-string [value]
  (if (nil? value) "" (str value)))

(defn- normalize-newlines [value]
  (str/replace value #"\r\n?" "\n"))

(defn html
  "Escapes a value for HTML text content inside table cells.

  nil is treated as an empty string and all other values are coerced with str.
  Escapes &, <, >, double quote, and single quote. This helper is intended for
  text content, not for constructing HTML attributes or URLs."
  [value]
  (str/escape (cell-string value)
              {\& "&amp;"
               \< "&lt;"
               \> "&gt;"
               \" "&quot;"
               \' "&#39;"}))

(defn markdown-cell
  "Escapes a value for a Markdown table cell.

  nil is treated as an empty string and all other values are coerced with str.
  Backslashes and pipes are escaped, and CR/LF line breaks are rendered as
  <br>. This keeps generated Markdown tables structurally valid when cell values
  contain table delimiters or line breaks."
  [value]
  (-> (cell-string value)
      normalize-newlines
      (str/replace "\\" "\\\\")
      (str/replace "|" "\\|")
      (str/replace "\n" "<br>")))

(defn csv-cell
  "Escapes a value for an RFC 4180-style CSV cell.

  nil is treated as an empty string and all other values are coerced with str.
  Values containing comma, double quote, CR, or LF are wrapped in double quotes,
  and embedded double quotes are doubled."
  [value]
  (let [value (cell-string value)]
    (if (some #(str/includes? value %) ["," "\"" "\r" "\n"])
      (str "\"" (str/replace value "\"" "\"\"") "\"")
      value)))

(defn tsv-cell
  "Escapes a value for a single-line TSV cell.

  nil is treated as an empty string and all other values are coerced with str.
  Backslashes, tabs, CR, and LF are rendered as visible backslash escapes so cell
  values cannot add columns or rows to the generated TSV output."
  [value]
  (-> (cell-string value)
      (str/replace "\\" "\\\\")
      normalize-newlines
      (str/replace "\t" "\\t")
      (str/replace "\n" "\\n")))

(defn org-cell
  "Escapes a value for an Org mode table cell.

  nil is treated as an empty string and all other values are coerced with str.
  Pipes are rendered as `\\vert{}` and CR/LF line breaks are rendered as `<br>` so
  values cannot split the table structure."
  [value]
  (-> (cell-string value)
      normalize-newlines
      (str/replace "|" "\\vert{}")
      (str/replace "\n" "<br>")))

(defn rst-cell
  "Escapes a value for a reStructuredText simple-table cell.

  nil is treated as an empty string and all other values are coerced with str.
  Backslashes are doubled and CR/LF line breaks are collapsed to spaces so values
  stay inside one physical table row."
  [value]
  (-> (cell-string value)
      normalize-newlines
      (str/replace "\\" "\\\\")
      (str/replace "\n" " ")))

(defn- log-safe-char [ch]
  (case ch
    \\ "\\\\"
    \tab "\\t"
    \newline "\\n"
    \return "\\r"
    (if (or (Character/isISOControl ch)
            (= \u2028 ch)
            (= \u2029 ch))
      (format "\\u%04X" (int ch))
      (str ch))))

(defn log-safe
  "Escapes a value for single-line log output.

  nil is treated as an empty string and all other values are coerced with str.
  Backslashes, tabs, line breaks, ISO control characters, and Unicode
  line/paragraph separators are rendered as visible escape sequences."
  [value]
  (apply str (map log-safe-char (cell-string value))))

(defn map-cells
  "Applies f eagerly to every cell in rows.

  Returns a vector of row vectors suitable for layout, layout-str, or other
  eager consumers. Use map-cell-seq instead when the input is large or lazy."
  [f rows]
  (mapv (fn [row]
          (mapv f row))
        rows))

(defn map-cell-seq
  "Lazily applies f to every cell in rows.

  Each realized row is returned as a vector, so the result can be passed directly
  to layout-seq for large data sets. The outer sequence is lazy; each row is
  transformed when that row is consumed."
  [f rows]
  (map (fn [row]
         (mapv f row))
       rows))
