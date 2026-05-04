# clj-string-layout Recipes

This page collects common layouts you can paste into a REPL and adapt.

For syntax details, see the [layout language reference](layout-language.md).

```clojure
(require '[clj-string-layout.core :refer [layout layout-seq layout-str]]
         '[clj-string-layout.escape :as escape]
         '[clj-string-layout.layout :as layouts]
         '[clj-string-layout.predicates :as pred]
         '[clj-string-layout.width :as width])
```

## Simple Columns

Use `[L]`, `[C]`, and `[R]` for left, center, and right alignment.

```clojure
(layout [["name" "qty" "price"]
         ["apple" "12" "$1.50"]
         ["pear" "4" "$2.00"]]
        {:layout {:cols ["[L]  [R]  [R]"]}})
;; => ["name   qty  price"
;;     "apple   12  $1.50"
;;     "pear     4  $2.00"]
```

The same shape is available as a preset:

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-plain-left)
;; => ["name   qty"
;;     "apple  12 "]
```

## CSV And TSV

Use `layout-tsv` for tab-separated output when cell contents are already safe, or
combine it with `escape/tsv-cell` when values may contain tabs or line breaks:

```clojure
(layout [["a" "b" "c"]]
        layouts/layout-tsv)
;; => ["a\tb\tc"]

(layout (escape/map-cells escape/tsv-cell [["a\tb" "line\nbreak"]])
        layouts/layout-tsv)
;; => ["a\\tb\tline\\nbreak"]
```

Use `layout-csv` with `escape/csv-cell` for CSV data:

```clojure
(layout (escape/map-cells escape/csv-cell
                          [["name" "notes"]
                           ["apple" "red, green"]])
        layouts/layout-csv)
;; => ["name,notes"
;;     "apple,\"red, green\""]
```

Use `layout-pipe-separated` for compact pipe-separated output that is not a
Markdown table:

```clojure
(layout [["a" "b" "c"]]
        layouts/layout-pipe-separated)
;; => ["a|b|c"]
```

## Width-Filled Rows

Use `f` where extra width should be distributed.

```clojure
(layout [["left" "right"]]
        {:width 24
         :fill-char \.
         :layout {:cols ["[L]f[R]"]}})
;; => ["left...............right"]
```

Multiple fill markers split the extra width.

```clojure
(layout [["x" "y"]]
        {:width 9
         :fill-char \-
         :layout {:cols ["f[L]f[R]f"]}})
;; => ["--x--y---"]
```

## Variable Column Counts

Repeat groups let one layout handle any number of columns.

```clojure
(layout [["a" "b" "c"]
         ["10" "200" "3"]]
        {:layout {:cols ["|{ [R] |}" :repeat-for [pred/all-cols?]]}})
;; => ["|  a |   b | c |"
;;     "| 10 | 200 | 3 |"]
```

Use multiple repeat groups when the first, interior, and last columns need
different delimiters.

```clojure
(layout [["a" "b" "c"]]
        {:layout {:cols ["{[L]}{, [L]}"
                         :repeat-for [pred/first-col? pred/not-first-col?]]}})
;; => ["a, b, c"]
```

## Box-Drawing Tables

Use a built-in preset when you want a complete table shape.

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-ascii-box-left)
;; => ["┌───────┬─────┐"
;;     "│ name  │ qty │"
;;     "├───────┼─────┤"
;;     "│ apple │ 12  │"
;;     "└───────┴─────┘"]
```

Use `layout-ascii-grid-*` when output must stay in plain ASCII:

```clojure
(layout [["a" "b"]]
        layouts/layout-ascii-grid-center)
;; => ["+---+---+"
;;     "| a | b |"
;;     "+---+---+"]
```

Use `layout-psql-*` for terminal output similar to PostgreSQL's `psql`:

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-psql-left)
;; => [" name   | qty"
;;     "------+----"
;;     " apple  | 12 "]
```

Use `layout-org-*` for Org mode tables:

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-org-left)
;; => ["| name  | qty |"
;;     "|-----+---|"
;;     "| apple | 12  |"]
```

Use `escape/org-cell` when Org cell values may contain `|` or line breaks.

Use `layout-rst-simple` for reStructuredText simple tables:

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-rst-simple)
;; => ["=====  ==="
;;     "name   qty"
;;     "=====  ==="
;;     "apple  12 "
;;     "=====  ==="]
```

Use `escape/rst-cell` when simple-table values may contain backslashes or line
breaks.

The `*-fill-*` presets consume `:width`.

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        (assoc layouts/layout-ascii-box-fill-center :width 30))
```

## Markdown Tables

Markdown cells are emitted verbatim, so escape data first when values may
contain `|`, backslashes, or line breaks.

```clojure
(layout (escape/map-cells escape/markdown-cell
                          [["name" "notes"]
                           ["apple" "red|green"]])
        layouts/layout-markdown-left)
;; => ["| name  | notes      |"
;;     "|:----- |:---------- |"
;;     "| apple | red\\|green |"]
```

## HTML Tables

HTML cells are emitted verbatim by design. Escape user-provided values first.

```clojure
(layout (escape/map-cells escape/html
                          [["<Alice>" "tea & cake"]])
        layouts/layout-html-table)
;; => ["<table>"
;;     "  <tr><td>&lt;Alice&gt;</td><td>tea &amp; cake</td></tr>"
;;     "</table>"]
```

## Wide Glyphs

By default, widths use `count`. Pass `width/unicode-width` when terminal display
width differs from string length because values contain wide glyphs.

```clojure
(layout [["界" "x"]
         ["ab" "yy"]]
        {:display-width width/unicode-width
         :layout {:cols ["[R] [L]"]}})
;; => ["界 x "
;;     "ab yy"]
```

## ANSI Colored Output

Use `width/ansi-width` when values contain terminal color/style sequences that
should not count as visible columns. If the colored values may also contain wide
glyphs, use `width/terminal-width` instead.

```clojure
(def red (str "\u001B[31m" "red" "\u001B[0m"))

(layout [[red "x"]
         ["blue" "yy"]]
        {:display-width width/ansi-width
         :layout {:cols ["[L] [L]"]}})
;; The first row displays as "red  x " with "red" colored red.
```

## Raw Output For Styling

Use `:raw? true` to get row pieces before they are joined. This is useful when a
later step needs to color cells or otherwise decorate pieces.

```clojure
(layout [["a" "b"]]
        {:raw? true
         :layout {:cols ["| [L] | [R] |"]}})
;; => [["| " "a" " | " "b" " |"]]
```

## Large Data Sets

Exact automatic widths require a full scan of the data. If your data set is
large and the schema widths are known, use `layout-seq` with `:col-widths` so
output can be consumed row-by-row. If you need escaping for a large lazy input,
use `escape/map-cell-seq` rather than `escape/map-cells`.

```clojure
(def huge-rows
  (map (fn [n] [(str "item-" n) (str n)])
       (range)))

(take 3
      (layout-seq huge-rows
                  {:col-widths [12 8]
                   :layout {:cols ["[L] [R]"]}}))
;; => ("item-0              0"
;;     "item-1              1"
;;     "item-2              2")
```

When row layouts are present, pass `:row-count` for finite data so predicates
can identify the last virtual row without counting the input.

```clojure
(layout-seq (map vector ["a" "bb"])
            {:col-widths [3]
             :row-count 2
             :layout {:cols ["[L]"]
                      :rows [["[-]" :apply-for layouts/all-rows?]]}})
;; => ("---" "a  " "---" "bb " "---")
```

## Custom Split Characters

String input is split by `:row-split-char` and `:word-split-char`.

```clojure
(layout "name|qty;apple|12"
        {:word-split-char \|
         :row-split-char \;
         :layout {:cols ["[L] [R]"]}})
;; => ["name  qty"
;;     "apple  12"]
```

## Diagnostics

Use diagnostics while writing custom layout strings.

```clojure
(clj-string-layout.core/parse-layout "[L]f[R]")

(clj-string-layout.core/explain-layout "[x]")
;; => {:valid? false, :message "...", :data {:type :layout-parse-error, ...}}
```

## Single String Output

Use `layout-str` when the consumer wants one newline-delimited string.

```clojure
(layout-str [["a" "b"]
             ["aa" "bb"]]
            {:layout {:cols ["[L] [R]"]}})
;; => "a   b\naa bb"
```
