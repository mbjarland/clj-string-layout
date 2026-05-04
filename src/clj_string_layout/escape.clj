(ns clj-string-layout.escape
  (:require [clojure.string :as str]))

(defn- cell-string [value]
  (if (nil? value) "" (str value)))

(defn html
  "Escapes a value for HTML text emitted inside table cells.

  The value is coerced with str, with nil treated as an empty string. Escapes
  &, <, >, double quote, and single quote."
  [value]
  (str/escape (cell-string value)
              {\& "&amp;"
               \< "&lt;"
               \> "&gt;"
               \" "&quot;"
               \' "&#39;"}))

(defn markdown-cell
  "Escapes a value for a Markdown table cell.

  The value is coerced with str, with nil treated as an empty string. Backslash
  and pipe are escaped, and CR/LF line breaks are rendered as <br>."
  [value]
  (-> (cell-string value)
      (str/replace #"\r\n?" "\n")
      (str/replace "\\" "\\\\")
      (str/replace "|" "\\|")
      (str/replace "\n" "<br>")))

(defn map-cells
  "Applies f to every cell in rows and returns a vector of row vectors."
  [f rows]
  (mapv (fn [row]
          (mapv f row))
        rows))

(defn map-cell-seq
  "Lazily applies f to every cell in rows.

  Each realized row is returned as a vector so it can be passed directly to
  layout-seq for large data sets."
  [f rows]
  (map (fn [row]
         (mapv f row))
       rows))
