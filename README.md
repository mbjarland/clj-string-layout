# clj-string-layout

[![CI Status](https://github.com/mbjarland/clj-string-layout/actions/workflows/ci.yml/badge.svg)](https://github.com/mbjarland/clj-string-layout/actions)
[![Clojars](https://img.shields.io/clojars/v/com.github.mbjarland/clj-string-layout.svg)](https://clojars.org/com.github.mbjarland/clj-string-layout)
[![License](https://img.shields.io/badge/license-EPL--1.0-blue.svg)](LICENSE)

`clj-string-layout` is a small Clojure library for turning rows of strings into aligned text layouts: simple columns, box-drawing tables, Markdown tables, HTML table snippets, and custom formats defined with a compact layout language.

The core idea is that column layouts describe how each data cell is aligned, while row layouts describe virtual rows inserted around or between the data rows. Repeating layout groups make the same layout work for any number of columns.

For more copy-and-paste examples, see the [recipe book](doc/recipes.md).

## Installation

Add the library to `deps.edn`:

```clojure
{:deps {com.github.mbjarland/clj-string-layout {:mvn/version "1.0.3"}}}
```

Require the namespaces you need:

```clojure
(require '[clj-string-layout.core :refer [layout layout-seq]]
         '[clj-string-layout.escape :as escape]
         '[clj-string-layout.layout :as layouts]
         '[clj-string-layout.predicates :as pred])
```

The library is tested on Java 11, 17, and 21. Java 11 is the intended minimum runtime.

## Quick Start

Input can be a string split into rows and words:

```clojure
(def data (str "Alice, why is\n"
               "a raven like\n"
               "a writing desk?"))

(layout data {:layout {:cols ["[L] [L] [L]"]}})
;; => ["Alice, why     is   "
;;     "a      raven   like "
;;     "a      writing desk?"]
```

Input can also be a vector of rows when you want exact control over cell boundaries:

```clojure
(layout [["Alice," "why" "is"]
         ["a" "raven" "like"]
         ["a" "writing" "desk?"]]
        {:layout {:cols ["[L] [L] [L]"]}})
```

Use a built-in layout for common output formats:

```clojure
(layout data layouts/layout-ascii-box-center)
;; => ["┌────────┬─────────┬───────┐"
;;     "│ Alice, │   why   │   is  │"
;;     "├────────┼─────────┼───────┤"
;;     "│    a   │  raven  │  like │"
;;     "├────────┼─────────┼───────┤"
;;     "│    a   │ writing │ desk? │"
;;     "└────────┴─────────┴───────┘"]
```

## Layout Config

`layout` takes rows and a layout config map.

```clojure
{:align-char      \space
 :fill-char       \space
 :word-split-char \space
 :row-split-char  \newline
 :display-width   count
 :col-widths      nil
 :row-count       nil
 :width           80
 :raw?            false
 :layout {:cols ["[L] [C] [R]"]
          :rows [["|[-]|[-]|[-]|" :apply-for layouts/first-row?]]}}
```

Options:

| Key | Default | Meaning |
| --- | --- | --- |
| `:layout` | required | A map with `:cols` and optional `:rows`. |
| `:width` | `80` | Target width when the layout contains fill markers. Layouts can still exceed this if the data and literals are wider. |
| `:align-char` | space | Character used to pad aligned data cells. |
| `:fill-char` | space | Character used for `f` fill markers unless overridden by a row layout. |
| `:word-split-char` | space | Character used to split string input into words. |
| `:row-split-char` | newline | Character used to split string input into rows. |
| `:display-width` | `count` | Function from string to display width. Override this for terminal-width-aware alignment of wide glyphs. |
| `:col-widths` | `nil` | Optional explicit column display widths. Useful for fixed schemas and streaming large data sets. |
| `:row-count` | `nil` | Optional data row count for lazy output with row layouts. |
| `:raw?` | `false` | Return each output row as a vector of pieces instead of joined strings. Useful when post-processing cells, for example adding ANSI colors. |

By default, widths are measured with Clojure's `count`, preserving plain string
length behavior. For monospace terminal output containing wide glyphs, pass a
`:display-width` function that returns a non-negative integer for each string.
The function is used for cell values, literal delimiters, padding, and fill
width calculations. Alignment and fill characters should occupy one display
column.

Use `layout-seq` with `:col-widths` for large data sets when the schema widths
are known ahead of time. Without explicit widths, exact alignment still needs to
scan all rows before the first output row can be rendered. If the layout inserts
virtual rows, pass `:row-count` as well so row predicates can identify the last
virtual row without counting the input. Use `escape/map-cell-seq` instead of
`escape/map-cells` when escaping a large lazy input.

## The Layout Language

Layout strings are made from four pieces:

| Piece | Example | Meaning |
| --- | --- | --- |
| Literal delimiter text | <code>" &#124; "</code>, <code>"&lt;/td&gt;"</code>, <code>"┌"</code> | Text emitted exactly as part of the output. |
| Column marker | `[L]`, `[C]`, `[R]`, `[V]` | Placeholder for a data column. |
| Fill marker | `f` or `F` | Expands to absorb remaining width. |
| Repeat group | <code>{ [L] &#124;}</code> | Repeats a sub-layout for a variable number of columns. |
| Escaped literal | `\f`, `\{`, `\]` | Emits the following character literally. |

The current grammar is:

```clojure
layout = delim? ((col | repeat) delim?)*
repeat  = <'{'> delim? (col delim?)* <'}'>
delim   = (escaped | fill | #'[^\\\\\\[\\]{}fF]+')+
escaped = <'\\\\'> #'.'
fill   = <'F'> (#'[\\d]+')?
col    = <'['> fill? align fill? <']'>
```

Column layouts and row layouts share this structure, but they interpret `align` differently.

Use a backslash when delimiter text needs a reserved character literally:

```clojure
(layout "a" {:layout {:cols ["\\f[L]\\F"]}})
;; => ["faF"]
```

## Column Layouts

Column layouts live at `[:layout :cols]`. The value is a vector whose first item is the layout string. Additional key/value pairs configure repeating groups.

```clojure
{:layout {:cols ["[L] [C] [R]"]}}
```

Supported column alignments:

| Marker | Meaning |
| --- | --- |
| `[L]` | Left-align the cell within the column width. |
| `[C]` | Center-align the cell within the column width. |
| `[R]` | Right-align the cell within the column width. |
| `[V]` | Verbatim output. Do not pad the cell to the computed column width. |

Examples:

```clojure
(layout "name price\napple 12\npear 4"
        {:layout {:cols ["[L]  [R]"]}})
;; => ["name   price"
;;     "apple     12"
;;     "pear       4"]
```

Use `f` when extra width should be distributed into the layout instead of ignored:

```clojure
(layout "left right"
        {:width 20
         :layout {:cols ["[L]f[R]"]}})
;; => ["left           right"]
```

Fill markers may appear in delimiters or inside column brackets:

```clojure
"[L]f[R]"      ;; all extra width between two columns
"f[R] [L]f"    ;; split extra width before and after the row
"[Lf] [Rf]"    ;; expand the column padding itself
```

When multiple fill markers are present, remaining width is distributed across them as evenly as possible. Any remainder is assigned from left to right according to the existing fill algorithm.

## Repeat Groups

Repeat groups make layouts adapt to the number of input columns. They are wrapped in `{...}`.

```clojure
{:layout {:cols ["|{ [L] |}" :repeat-for [pred/all-cols?]]}}
```

For three columns, the repeating section is expanded three times:

```text
| [L] | [L] | [L] |
```

Repeat groups are useful for table-like formats where the same cell pattern should be reused for every column:

```clojure
(layout "a b c\n1 2 3"
        {:layout {:cols ["|{ [C] |}" :repeat-for [pred/all-cols?]]}})
;; => ["| a | b | c |"
;;     "| 1 | 2 | 3 |"]
```

The `:repeat-for` value controls which columns a repeat group handles. The predicate receives `[idx last-idx]`. The older `:apply-for` key is still accepted on column layouts for compatibility, but `:repeat-for` is clearer because row layouts also use `:apply-for` for virtual row predicates.

Column predicates are supplied by `clj-string-layout.predicates` and are also re-exported by `clj-string-layout.layout` for compatibility:

| Predicate | Matches |
| --- | --- |
| `first-col?` | First column. |
| `second-col?` | Second column. |
| `last-col?` | Last column. |
| `not-first-col?` | Every column except the first. |
| `not-last-col?` | Every column except the last. |
| `interior-col?` | Columns that are neither first nor last. |
| `not-interior-col?` | First or last column. |
| `all-cols?` | Every column. |

You can also pass your own predicate:

```clojure
(defn even-col? [[idx _]] (even? idx))
```

## Row Layouts

Row layouts live at `[:layout :rows]`. They insert virtual rows before, between, or after data rows.

```clojure
{:layout {:cols ["│{ [L] │}" :repeat-for [pred/all-cols?]]
          :rows [["┌{─[─]─┬}─[─]─┐" :apply-for layouts/first-row?]
                 ["├{─[─]─┼}─[─]─┤" :apply-for layouts/interior-row?]
                 ["└{─[─]─┴}─[─]─┘" :apply-for layouts/last-row?]]}}
```

Row layout column markers use the character inside brackets as a drawing character, not a cell alignment. For example, `[─]` emits enough `─` characters to match the corresponding data column width. `[=f]` uses `=` and can include fill expansion.

Row predicates receive `[idx last-idx]`, where the indexes refer to virtual row positions. With three data rows, the virtual row positions are `0`, `1`, `2`, and `3`. `0` is before the first data row, `3` is after the last data row, and the interior positions are between data rows.

Row predicates are supplied by `clj-string-layout.predicates` and are also re-exported by `clj-string-layout.layout` for compatibility:

| Predicate | Matches |
| --- | --- |
| `first-row?` | Virtual row before the first data row. |
| `second-row?` | Virtual row after the first data row. Useful for Markdown header separators. |
| `last-row?` | Virtual row after the last data row. |
| `not-first-row?` | Every virtual row except the first. |
| `not-last-row?` | Every virtual row except the last. |
| `interior-row?` | Virtual rows between data rows. |
| `not-interior-row?` | First or last virtual row. |
| `all-rows?` | Every virtual row. |

Example Markdown table:

```clojure
(layout (str "name qty\n"
             "apple 12\n"
             "pear 4")
        layouts/layout-markdown-left)
;; => ["| name  | qty |"
;;     "|:----- |:--- |"
;;     "| apple | 12  |"
;;     "| pear  | 4   |"]
```

## Built-In Layouts

Built-in layouts are available in `clj-string-layout.layout`.

Box-drawing layouts:

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-ascii-box-left` | Left | No |
| `layout-ascii-box-center` | Center | No |
| `layout-ascii-box-right` | Right | No |
| `layout-ascii-box-fill-left` | Left | Yes |
| `layout-ascii-box-fill-center` | Center | Yes |
| `layout-ascii-box-fill-right` | Right | Yes |

Norton Commander-style layouts:

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-norton-commander-left` | Left | No |
| `layout-norton-commander-center` | Center | No |
| `layout-norton-commander-right` | Right | No |
| `layout-norton-commander-fill-left` | Left | Yes |
| `layout-norton-commander-fill-center` | Center | Yes |
| `layout-norton-commander-fill-right` | Right | Yes |

Markdown layouts:

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-markdown-left` | Left | No |
| `layout-markdown-center` | Center | No |
| `layout-markdown-right` | Right | No |
| `layout-markdown-fill-left` | Left | Yes |
| `layout-markdown-fill-center` | Center | Yes |
| `layout-markdown-fill-right` | Right | Yes |

HTML layouts:

| Var | Behavior |
| --- | --- |
| `layout-html-table` | Emits `<table>`, one `<tr>` per input row, and verbatim `<td>` contents. |
| `layout-html-table-readable` | Same shape, but left-aligns cell contents for more readable source output. |

HTML example:

```clojure
(layout "Alice why\na raven" layouts/layout-html-table)
;; => ["<table>"
;;     "  <tr><td>Alice</td><td>why</td></tr>"
;;     "  <tr><td>a</td><td>raven</td></tr>"
;;     "</table>"]
```

HTML and Markdown presets emit cell contents verbatim by default. Escape input
cells before rendering when the data is not already safe for the target format:

```clojure
(layout (escape/map-cells escape/html [["<Alice>" "tea & cake"]])
        layouts/layout-html-table)
;; => ["<table>"
;;     "  <tr><td>&lt;Alice&gt;</td><td>tea &amp; cake</td></tr>"
;;     "</table>"]

(layout (escape/map-cells escape/markdown-cell [["name" "a|b"]])
        layouts/layout-markdown-left)
;; => ["| name | a\\|b |"
;;     "|:---- |:----- |"]
```

## Raw Output

Set `:raw? true` if you need the pieces before they are joined:

```clojure
(layout "a b" {:raw? true
               :layout {:cols ["| [L] | [R] |"]}})
;; => [["| " "a" " | " "b" " |"]]
```

This is useful when a later step needs to decorate specific cells without re-parsing the final string.

## Large Data Sets

Automatic column widths require all rows to be inspected before output can be
rendered. For very large data sets with known schema widths, use `layout-seq`
and provide `:col-widths` to render rows lazily:

```clojure
(def rows (map vector ["a" "bb" "ccc"]))

(take 2 (layout-seq rows {:col-widths [3]
                          :layout {:cols ["[L]"]}}))
;; => ("a  " "bb ")
```

If the layout has virtual rows, pass `:row-count` so predicates such as
`last-row?` work without counting the input first:

```clojure
(layout-seq rows {:col-widths [3]
                  :row-count 3
                  :layout {:cols ["[L]"]
                           :rows [["[-]" :apply-for layouts/all-rows?]]}})
```

## Convenience And Diagnostics

Use `layout-str` when you want a single newline-delimited string:

```clojure
(layout-str "a b\naa bb" {:layout {:cols ["[L] [R]"]}})
;; => "a   b\naa bb"
```

Use `parse-layout` or `explain-layout` while developing custom layout strings:

```clojure
(parse-layout "[L]f[R]")
;; => [{:type :column, :align :l, ...} {:type :fill} ...]

(explain-layout "[x]")
;; => {:valid? false, :message "...", :data {:type :layout-parse-error, ...}}
```

## Development

Run the test suite:

```sh
clojure -M:test
```

Run the linter:

```sh
clojure -M:lint
```

Build the jar:

```sh
clojure -T:build jar
```

Install locally:

```sh
clojure -T:build install
```

Deploy to Clojars after setting Clojars credentials for `deps-deploy`:

```sh
clojure -T:deploy deploy
```

## Design Notes

The library intentionally keeps the public API small. Most users need only `clj-string-layout.core/layout`, reusable predicates in `clj-string-layout.predicates`, and preset layouts in `clj-string-layout.layout`.

The `f` and `F` characters are reserved as fill markers in layout delimiter positions. Use escaped literals such as `\f` or `\F` when delimiter text needs those characters literally.

## License

Copyright © 2017-2026 Matias Bjarland

Distributed under the Eclipse Public License 1.0.
