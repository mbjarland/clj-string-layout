(ns clj-string-layout.presets
  "Built-in layout configuration maps for common text, Markdown, and HTML tables.

  Presets are plain layout config maps that can be passed to layout, layout-str,
  or layout-seq. They can also be assoc'ed with options such as :width,
  :display-width, :col-widths, or :raw?. HTML and Markdown presets emit cell
  contents verbatim; use clj-string-layout.escape for untrusted data."
  (:require [clj-string-layout.predicates :as pred]))

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
                   :repeat-for [pred/all-cols?]]
            :rows [[(bordered-row (:top chars) fill?) :apply-for pred/first-row?]
                   [(bordered-row (:middle chars) fill?)
                    :apply-for pred/interior-row?]
                   [(bordered-row (:bottom chars) fill?)
                    :apply-for pred/last-row?]]}})

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

(def layout-ascii-box-left
  "Unicode box-drawing table with left-aligned cells.

  Uses single-line box characters and sizes columns from the input data."
  (bordered-layout ascii-box-chars "L" false))

(def layout-ascii-box-center
  "Unicode box-drawing table with centered cells.

  Uses single-line box characters and sizes columns from the input data."
  (bordered-layout ascii-box-chars "C" false))

(def layout-ascii-box-right
  "Unicode box-drawing table with right-aligned cells.

  Uses single-line box characters and sizes columns from the input data."
  (bordered-layout ascii-box-chars "R" false))

(def layout-ascii-box-fill-left
  "Width-filling Unicode box-drawing table with left-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-box-chars "L" true))

(def layout-ascii-box-fill-center
  "Width-filling Unicode box-drawing table with centered cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-box-chars "C" true))

(def layout-ascii-box-fill-right
  "Width-filling Unicode box-drawing table with right-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-box-chars "R" true))

(def layout-norton-commander-left
  "Norton Commander-style table with left-aligned cells.

  Uses double outer borders and single interior separators."
  (bordered-layout norton-commander-chars "L" false))

(def layout-norton-commander-center
  "Norton Commander-style table with centered cells.

  Uses double outer borders and single interior separators."
  (bordered-layout norton-commander-chars "C" false))

(def layout-norton-commander-right
  "Norton Commander-style table with right-aligned cells.

  Uses double outer borders and single interior separators."
  (bordered-layout norton-commander-chars "R" false))

(def layout-norton-commander-fill-left
  "Width-filling Norton Commander-style table with left-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout norton-commander-chars "L" true))

(def layout-norton-commander-fill-center
  "Width-filling Norton Commander-style table with centered cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout norton-commander-chars "C" true))

(def layout-norton-commander-fill-right
  "Width-filling Norton Commander-style table with right-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
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
  {:layout {:cols [(markdown-cols align fill?) :repeat-for [pred/all-cols?]]
            :rows [[(markdown-row align fill?) :apply-for pred/second-row?]]}})

(def layout-markdown-left
  "Markdown pipe table with left-aligned columns.

  Cell contents are emitted verbatim; escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "L" false))

(def layout-markdown-center
  "Markdown pipe table with centered columns.

  Cell contents are emitted verbatim; escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "C" false))

(def layout-markdown-right
  "Markdown pipe table with right-aligned columns.

  Cell contents are emitted verbatim; escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "R" false))

(def layout-markdown-fill-left
  "Width-filling Markdown pipe table with left-aligned columns.

  Fill markers expand cell padding toward :width. Escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "L" true))

(def layout-markdown-fill-center
  "Width-filling Markdown pipe table with centered columns.

  Fill markers expand cell padding toward :width. Escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "C" true))

(def layout-markdown-fill-right
  "Width-filling Markdown pipe table with right-aligned columns.

  Fill markers expand cell padding toward :width. Escape untrusted values with
  clj-string-layout.escape/markdown-cell before rendering."
  (markdown-layout "R" true))

(defn- html-table-layout [align]
  {:layout {:cols [(str "  <tr>{<td>[" align "]</td>}</tr>")
                   :repeat-for [pred/all-cols?]]
            :rows [["<table>" :apply-for pred/first-row?]
                   ["</table>" :apply-for pred/last-row?]]}})

(def layout-html-table
  "HTML table skeleton that emits compact rows with verbatim cell contents.

  Produces <table>, one <tr> per input row, and </table>. Cell values are placed
  inside <td> elements without escaping; use clj-string-layout.escape/html for
  untrusted values."
  (html-table-layout "V"))

(def layout-html-table-readable
  "HTML table skeleton with left-aligned cell contents in the generated source.

  Produces the same tags as layout-html-table, but pads cells in the source for
  readability. Cell values are not escaped automatically."
  (html-table-layout "L"))
