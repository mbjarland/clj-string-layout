(ns clj-string-layout.layout
  (:require [clj-string-layout.predicates :as pred]))

;; Predicate aliases are kept here so existing callers can continue requiring
;; only clj-string-layout.layout for built-in layouts and predicates.
(def first-row? pred/first-row?)
(def not-first-row? pred/not-first-row?)
(def second-row? pred/second-row?)
(def last-row? pred/last-row?)
(def not-last-row? pred/not-last-row?)
(def interior-row? pred/interior-row?)
(def not-interior-row? pred/not-interior-row?)
(def all-rows? pred/all-rows?)

(def first-col? pred/first-col?)
(def not-first-col? pred/not-first-col?)
(def second-col? pred/second-col?)
(def last-col? pred/last-col?)
(def not-last-col? pred/not-last-col?)
(def interior-col? pred/interior-col?)
(def not-interior-col? pred/not-interior-col?)
(def all-cols? pred/all-cols?)

(defn- align-token [align fill?]
  (str "[" align (when fill? "f") "]"))

(defn- row-token [ch fill?]
  (str "[" ch (when fill? "f") "]"))

(defn- bordered-cols [{:keys [left sep right]} align fill?]
  (let [token (align-token align fill?)]
    (str left "{ " token " " sep "} " token " " right)))

(defn- bordered-row [{:keys [left fill sep right]} fill?]
  (let [token (row-token fill fill?)]
    (str left "{" fill token fill sep "}" fill token fill right)))

(defn- bordered-layout [chars align fill?]
  {:layout {:cols [(bordered-cols (:cols chars) align fill?)
                   :repeat-for [all-cols?]]
            :rows [[(bordered-row (:top chars) fill?) :apply-for first-row?]
                   [(bordered-row (:middle chars) fill?) :apply-for interior-row?]
                   [(bordered-row (:bottom chars) fill?) :apply-for last-row?]]}})

(def ^:private ascii-box-chars
  {:cols {:left "│" :sep "│" :right "│"}
   :top {:left "┌" :fill "─" :sep "┬" :right "┐"}
   :middle {:left "├" :fill "─" :sep "┼" :right "┤"}
   :bottom {:left "└" :fill "─" :sep "┴" :right "┘"}})

(def ^:private norton-commander-chars
  {:cols {:left "║" :sep "│" :right "║"}
   :top {:left "╔" :fill "═" :sep "╤" :right "╗"}
   :middle {:left "╟" :fill "─" :sep "┼" :right "╢"}
   :bottom {:left "╚" :fill "═" :sep "╧" :right "╝"}})

(def layout-ascii-box-left (bordered-layout ascii-box-chars "L" false))
(def layout-ascii-box-center (bordered-layout ascii-box-chars "C" false))
(def layout-ascii-box-right (bordered-layout ascii-box-chars "R" false))

(def layout-ascii-box-fill-left (bordered-layout ascii-box-chars "L" true))
(def layout-ascii-box-fill-center (bordered-layout ascii-box-chars "C" true))
(def layout-ascii-box-fill-right (bordered-layout ascii-box-chars "R" true))

(def layout-norton-commander-left
  (bordered-layout norton-commander-chars "L" false))
(def layout-norton-commander-center
  (bordered-layout norton-commander-chars "C" false))
(def layout-norton-commander-right
  (bordered-layout norton-commander-chars "R" false))

(def layout-norton-commander-fill-left
  (bordered-layout norton-commander-chars "L" true))
(def layout-norton-commander-fill-center
  (bordered-layout norton-commander-chars "C" true))
(def layout-norton-commander-fill-right
  (bordered-layout norton-commander-chars "R" true))

(defn- markdown-cols [align fill?]
  (str "|{ " (align-token align fill?) " |}"))

(defn- markdown-row [align fill?]
  (let [token (row-token "-" fill?)]
    (case align
      "L" (str "|{:" token " |}")
      "C" (str "|{:" token ":|}")
      "R" (str "|{ " token ":|}"))))

(defn- markdown-layout [align fill?]
  {:layout {:cols [(markdown-cols align fill?) :repeat-for [all-cols?]]
            :rows [[(markdown-row align fill?) :apply-for second-row?]]}})

(def layout-markdown-left (markdown-layout "L" false))
(def layout-markdown-center (markdown-layout "C" false))
(def layout-markdown-right (markdown-layout "R" false))

(def layout-markdown-fill-left (markdown-layout "L" true))
(def layout-markdown-fill-center (markdown-layout "C" true))
(def layout-markdown-fill-right (markdown-layout "R" true))

(defn- html-table-layout [align]
  {:layout {:cols [(str "  <tr>{<td>[" align "]</td>}</tr>")
                   :repeat-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table>" :apply-for last-row?]]}})

(def layout-html-table (html-table-layout "V"))
(def layout-html-table-readable (html-table-layout "L"))
