# High-Level Table API

The `clj-string-layout.table` namespace is the easiest entry point for common
tables. It lets you choose a named format, pass headers and rows, and describe
columns without writing layout DSL strings.

```clojure
(require '[clj-string-layout.table :as table])
```

## Basic Usage

```clojure
(table/table {:format :markdown
              :headers ["Name" "Qty"]
              :rows [["apple" 12]
                     ["pear" 4]]})
;; => ["| Name  | Qty |"
;;     "|:----- |:--- |"
;;     "| apple | 12  |"
;;     "| pear  | 4   |"]
```

Use `table-str` when the consumer wants one string:

```clojure
(table/table-str {:format :plain
                  :headers ["Name" "Qty"]
                  :rows [["apple" 12]]})
;; => "Name   Qty\napple  12 "
```

## Named Formats

Available formats are discoverable at runtime:

```clojure
(table/formats)
;; => #{:plain :markdown :ascii-grid :csv :tsv :pipe :psql :org :rst :html}
```

| Format | Output |
| --- | --- |
| `:plain` | Whitespace-separated aligned columns. |
| `:markdown` | Markdown pipe table. |
| `:ascii-grid` | ASCII `+---+` table. |
| `:csv` | Comma-separated values, CSV-escaped by default. |
| `:tsv` | Tab-separated values. |
| `:pipe` | Compact pipe-separated values. |
| `:psql` | PostgreSQL psql-style terminal table. |
| `:org` | Org mode table. |
| `:rst` | reStructuredText simple table. |
| `:html` | HTML table with optional header row. |

## Map Rows And Column Specs

Column specs let you render maps directly and control titles, alignment,
formatting, widths, and overflow.

```clojure
(table/table {:format :markdown
              :columns [{:key :name :title "Name"}
                        {:key :qty :title "Qty" :align :right}]
              :rows [{:name "apple" :qty 12}
                     {:name "pear" :qty 4}]})
;; => ["| Name  | Qty |"
;;     "|:----- | ---:|"
;;     "| apple |  12 |"
;;     "| pear  |   4 |"]
```

Column keys for vector rows can be numeric indexes:

```clojure
{:columns [{:key 0 :title "Name"}
           {:key 1 :title "Qty" :align :right}]}
```

## Cell Formatting

Use `:format` on a column to transform values before rendering:

```clojure
(table/table {:format :plain
              :columns [{:key :name :title "Name"}
                        {:key :price :title "Price"
                         :align :right
                         :format #(format "$%.2f" (double %))}]
              :rows [{:name "apple" :price 1.5}]})
;; => ["Name   Price"
;;     "apple  $1.50"]
```

## Overflow Policies

Use `:width` with `:overflow` to constrain cell text before layout.

| Policy | Behavior |
| --- | --- |
| `:none` | Default. Leave long values unchanged. |
| `:clip` | Cut values at `:width`. |
| `:ellipsis` | Cut values and append `...` where possible. |
| `:wrap` | Split values into multiple physical table rows. |
| `:error` | Throw ex-info when a value exceeds `:width`. |

```clojure
(table/table {:format :plain
              :columns [{:key 0 :title "Text" :width 4
                         :overflow :ellipsis}]
              :rows [["abcdef"]]})
;; => ["Text"
;;     "a..."]
```

Wrapping creates additional rows:

```clojure
(table/table {:format :plain
              :columns [{:key 0 :title "Txt" :width 3
                         :overflow :wrap}]
              :rows [["abcdef"]]})
;; => ["Txt"
;;     "abc"
;;     "def"]
```

## Escaping

The table API escapes by default for formats where raw values commonly break the
syntax:

| Format | Escaper |
| --- | --- |
| `:markdown` | `escape/markdown-cell` |
| `:csv` | `escape/csv-cell` |
| `:html` | `escape/html` |

Set `:escape? false` if you already escaped values yourself.

```clojure
(table/table {:format :html
              :headers ["<Name>"]
              :rows [["a&b"]]})
;; => ["<table>"
;;     "  <tr><th>&lt;Name&gt;</th></tr>"
;;     "  <tr><td>a&amp;b</td></tr>"
;;     "</table>"]
```

## When To Use The DSL Directly

Use the high-level table API when you need common output formats quickly. Use
`clj-string-layout.core/layout` directly when you need custom borders, multiple
repeat groups, custom row predicates, or a format not represented by the table
registry.
