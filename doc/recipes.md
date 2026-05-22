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
;;     "--------+----"
;;     " apple  | 12 "]
```

Use `layout-org-*` for Org mode tables:

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        layouts/layout-org-left)
;; => ["| name  | qty |"
;;     "|-------+-----|"
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

## Large Data

The lower-level `core/layout` and the high-level `table/table` are both
**eager**: they build the full output vector before returning. That's
fine up to roughly mid-six-figures of rows; past that point, expect
high heap pressure and eventual `OutOfMemoryError`. For real
multi-million-row workloads, switch to the streaming primitives:

```clojure
(require '[clj-string-layout.core :refer [layout-seq layout-into!]])

(with-open [w (clojure.java.io/writer "out.txt")]
  (layout-into! w
                rows                         ; a lazy seq is fine
                {:col-widths [12 8]          ; required — no full scan
                 :layout {:cols ["[L]  [R]"]}}))
```

What makes this safe:

- **`:col-widths` is supplied.** Without it the engine has to scan
  every row to compute widths before rendering can start, which
  defeats streaming.
- **`layout-seq` is genuinely lazy.** It maps each row to a rendered
  line on demand. With `:col-widths` set, no buffering of input rows
  happens.
- **`layout-into!` writes each rendered line straight to the
  `java.io.Writer` and lets it be GC'd.** Constant memory regardless
  of input size.

### Measured numbers

1 000 000 rows of CSV-shaped data (~37 MB input) on a 2024 laptop:

| Path | Time | Peak heap | Survives `-Xmx 256m`? |
| --- | --- | --- | --- |
| `(core/layout rows cfg)` (eager) | OOM | — | ✗ also fails at `-Xmx 512m` |
| `(table/table {...})` (eager) | OOM | — | ✗ also fails at `-Xmx 512m` |
| **`layout-into!` + `:col-widths` + `:csv` shape** | **2.0 s** | **280 MB** | ✓ |
| **`layout-into!` + `:col-widths` + ASCII-grid shape** | **3.3 s** | **280 MB** | ✓ |

The streaming path runs in constant memory; the eager paths' peak
heap scales with input size and OOMs on big inputs even with several
hundred MB of headroom.

### Row layouts in streaming mode

When the layout contains virtual rule rows (`:rows` in the config),
pass `:row-count` so predicates like `last-row?` can fire without
counting the input first:

```clojure
(layout-seq (map vector ["a" "bb"])
            {:col-widths [3]
             :row-count 2
             :layout {:cols ["[L]"]
                      :rows [["[-]" :apply-for layouts/all-rows?]]}})
;; => ("---" "a  " "---" "bb " "---")
```

### Escaping a lazy input

If you need per-cell escaping (e.g. CSV-quoting untrusted data) on a
lazy row source, use `escape/map-cell-seq` rather than the eager
`escape/map-cells`:

```clojure
(layout-into! w
              (escape/map-cell-seq escape/csv-cell huge-rows)
              {:col-widths [...] :layout {:cols ["{[V]}{,[V]}"
                                                  :repeat-for [pred/first-col?
                                                               pred/not-first-col?]]}})
```

### When you can't know widths up front

If your data is genuinely unknown shape and you still want streaming
output, two practical workarounds:

1. **Two-pass.** Read the input twice — once to compute `:col-widths`,
   once to render. Trivial when the source is a file; usually fine
   when the source is a query you can re-run.
2. **Sample-then-stream.** Read the first N rows, compute widths from
   them, then stream the rest. Widths may be wrong for outlier rows
   downstream — pair with `:overflow :clip` or `:overflow :ellipsis`
   on a column spec to keep the output rectangular.

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

## Streaming To A Writer

Use `layout-into!` to stream layout output to a `java.io.Writer` without
building the full vector first. Combined with `layout-seq`'s `:col-widths`
option this lets large data sets render line by line:

```clojure
(require '[clj-string-layout.core :refer [layout-into!]])

(with-open [w (clojure.java.io/writer "out.txt")]
  (layout-into! w huge-rows {:col-widths [12 8]
                             :layout {:cols ["[L] [R]"]}}))
```

`clj-string-layout.table/table-into!` does the same for the high-level table
API. Each line is written followed by a single `\n`, and the writer is
returned so it can be threaded.
