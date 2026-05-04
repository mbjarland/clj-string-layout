(ns clj-string-layout.presets
  "Built-in layout configuration maps for common text, CSV/TSV, Markdown, and HTML tables.

  Presets are plain layout config maps that can be passed to layout, layout-str,
  or layout-seq. They can also be assoc'ed with options such as :width,
  :display-width, :col-widths, or :raw?. Markup and separated-value presets emit
  cell contents verbatim; use clj-string-layout.escape for untrusted data."
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

(defn- plain-layout [align]
  {:layout {:cols [(str "{" (align-token align false) "}"
                        "{  " (align-token align false) "}")
                   :repeat-for [pred/first-col? pred/not-first-col?]]}})

(defn- separated-layout [separator]
  {:layout {:cols [(str "{[V]}{" separator "[V]}")
                   :repeat-for [pred/first-col? pred/not-first-col?]]}})

(defn- psql-cols [align]
  (str " " (align-token align false) " { | " (align-token align false) "}"))

(defn- psql-row []
  "[-]{-+-[-]}")

(defn- psql-layout [align]
  {:layout {:cols [(psql-cols align) :repeat-for [pred/not-first-col?]]
            :rows [[(psql-row) :apply-for pred/second-row?]]}})

(defn- rst-simple-cols []
  "{[L]}{  [L]}")

(defn- rst-simple-rule []
  "{[=]}{  [=]}")

(defn- rst-simple-layout []
  {:layout {:cols [(rst-simple-cols) :repeat-for [pred/first-col? pred/not-first-col?]]
            :rows [[(rst-simple-rule) :apply-for pred/first-row?]
                   [(rst-simple-rule) :apply-for pred/second-row?]
                   [(rst-simple-rule) :apply-for pred/last-row?]]}})

(defn- org-cols [align]
  (let [token (align-token align false)]
    (str "{| " token " }{| " token " }|")))

(defn- org-row []
  "{|[-]}{+[-]}|")

(defn- org-layout [align]
  {:layout {:cols [(org-cols align) :repeat-for [pred/first-col? pred/not-first-col?]]
            :rows [[(org-row) :apply-for pred/second-row?]]}})

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

(def ^:private ascii-grid-chars
  {:cols {:left "|" :sep "|" :right "|"}
   :top {:left "+" :fill "-" :sep "+" :right "+"}
   :middle {:left "+" :fill "-" :sep "+" :right "+"}
   :bottom {:left "+" :fill "-" :sep "+" :right "+"}})

(def layout-plain-left
  "Plain whitespace-separated columns with left-aligned cells.

  Uses two spaces between columns and no virtual header or border rows."
  (plain-layout "L"))

(def layout-plain-center
  "Plain whitespace-separated columns with centered cells.

  Uses two spaces between columns and no virtual header or border rows."
  (plain-layout "C"))

(def layout-plain-right
  "Plain whitespace-separated columns with right-aligned cells.

  Uses two spaces between columns and no virtual header or border rows."
  (plain-layout "R"))

(def layout-tsv
  "Tab-separated values with verbatim cell contents.

  This preset does not escape tabs or line breaks inside cells. It is intended
  for already-normalized data or callers that perform their own escaping."
  (separated-layout "\t"))

(def layout-csv
  "Comma-separated values with verbatim cell contents.

  Escape arbitrary values with clj-string-layout.escape/csv-cell before
  rendering. The layout itself only inserts commas between cells."
  (separated-layout ","))

(def layout-pipe-separated
  "Pipe-separated values with verbatim cell contents and no padding.

  This is useful for compact text protocols or quick human-readable dumps. It is
  not a Markdown table; use the Markdown presets when a header separator row is
  needed."
  (separated-layout "|"))

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

(def layout-ascii-grid-left
  "ASCII +---+ grid table with left-aligned cells.

  Uses only +, -, and | characters and sizes columns from the input data."
  (bordered-layout ascii-grid-chars "L" false))

(def layout-ascii-grid-center
  "ASCII +---+ grid table with centered cells.

  Uses only +, -, and | characters and sizes columns from the input data."
  (bordered-layout ascii-grid-chars "C" false))

(def layout-ascii-grid-right
  "ASCII +---+ grid table with right-aligned cells.

  Uses only +, -, and | characters and sizes columns from the input data."
  (bordered-layout ascii-grid-chars "R" false))

(def layout-ascii-grid-fill-left
  "Width-filling ASCII +---+ grid table with left-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-grid-chars "L" true))

(def layout-ascii-grid-fill-center
  "Width-filling ASCII +---+ grid table with centered cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-grid-chars "C" true))

(def layout-ascii-grid-fill-right
  "Width-filling ASCII +---+ grid table with right-aligned cells.

  Column padding uses fill markers so the rendered table expands toward :width."
  (bordered-layout ascii-grid-chars "R" true))

(def layout-psql-left
  "PostgreSQL psql-style table with left-aligned cells.

  Inserts a separator row after the first data row, making the first input row
  act as a header."
  (psql-layout "L"))

(def layout-psql-right
  "PostgreSQL psql-style table with right-aligned cells.

  Inserts a separator row after the first data row, making the first input row
  act as a header."
  (psql-layout "R"))

(def layout-rst-simple
  "reStructuredText simple table layout.

  Inserts top, header separator, and bottom rules. The first input row acts as a
  header. The output follows the simple-table shape and is best for cells that
  do not contain embedded line breaks."
  (rst-simple-layout))

(def layout-org-left
  "Org mode table with left-aligned cells.

  Inserts a separator row after the first data row, making the first input row
  act as a header."
  (org-layout "L"))

(def layout-org-right
  "Org mode table with right-aligned cells.

  Inserts a separator row after the first data row, making the first input row
  act as a header."
  (org-layout "R"))

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
