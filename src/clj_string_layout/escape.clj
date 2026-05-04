(ns clj-string-layout.escape
  "Helpers for escaping cell values before rendering markup-oriented layouts.

  The HTML and Markdown presets intentionally emit cell contents verbatim so
  callers can decide whether values are already trusted. Use this namespace to
  escape untrusted or arbitrary data before passing it to layout or layout-seq."
  (:require [clojure.string :as str]))

(defn- cell-string [value]
  (if (nil? value) "" (str value)))

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
      (str/replace #"\r\n?" "\n")
      (str/replace "\\" "\\\\")
      (str/replace "|" "\\|")
      (str/replace "\n" "<br>")))

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
