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
;; => #{:plain :markdown :markdown-left :markdown-center :markdown-right
;;      :box :double-box :unicode-box :unicode-double-box
;;      :ascii-box :ascii-double-box :ascii-grid
;;      :csv :tsv :pipe :psql :org :rst :html}
```

| Format | Output |
| --- | --- |
| `:plain` | Whitespace-separated aligned columns. |
| `:markdown` | Markdown pipe table. |
| `:markdown-left` | Markdown pipe table with left-aligned columns. |
| `:markdown-center` | Markdown pipe table with center-aligned columns. |
| `:markdown-right` | Markdown pipe table with right-aligned columns. |
| `:box` | Unicode box-drawing table with `┌─┬─┐` borders. Aliases: `:unicode-box`, `:ascii-box`. |
| `:double-box` | Unicode box-drawing table with `╔═╦═╗` borders. Aliases: `:unicode-double-box`, `:ascii-double-box`. |
| `:ascii-grid` | ASCII `+---+` table. |
| `:csv` | Comma-separated values, CSV-escaped by default. |
| `:tsv` | Tab-separated values. |
| `:pipe` | Compact pipe-separated values. |
| `:psql` | PostgreSQL psql-style terminal table. |
| `:org` | Org mode table. |
| `:rst` | reStructuredText simple table. |
| `:html` | HTML table with optional header row. |

The Markdown formats only emit the `|:---|` rule row when a header is present
(via `:headers` or `:columns`). Headerless data renders as plain pipe rows.

## Columns

`:columns` is how you tell the table what to extract from each row, what to
label it, and how to align or format it. There are two shapes:

| Shape | Reads as |
| --- | --- |
| `:qty` | from `:qty`, as `"qty"`, defaults otherwise |
| `{:from :qty :as "Qty" :align :right :formatter f ...}` | full map: explicit everything |

The keyword shortcut is sugar for `{:from :qty :as "qty"}`. Use it
whenever the only thing you want is "include this column with sensible
defaults". For anything else, reach for the map form. Both shapes can
appear in the same `:columns` vector.

Map keys:

| Key | Meaning |
| --- | --- |
| `:from` | Row map key. Required for map rows; omit it for vector rows to use the column's position. |
| `:as` | Header label. Defaults to the keyword name of `:from` (or empty when `:from` is omitted). |
| `:align` | `:left`, `:center`, `:right`, or `:verbatim`. |
| `:formatter` | One-arg function applied to the cell value before rendering. |
| `:width` | Maximum cell width, paired with `:overflow`. |
| `:overflow` | `:none`, `:clip`, `:ellipsis`, `:wrap`, or `:error`. |

A mixed example:

```clojure
(table/table {:format :markdown
              :columns [:name
                        {:from :qty   :as "Qty"   :align :right}
                        {:from :price :as "Price" :align :right
                         :formatter #(format "$%.2f" %)}]
              :rows [{:name "apple" :qty 12 :price 1.5}
                     {:name "pear"  :qty 4  :price 2.0}]})
;; => ["| name  | Qty | Price |"
;;     "|:----- | ---:| -----:|"
;;     "| apple |  12 | $1.50 |"
;;     "| pear  |   4 | $2.00 |"]
```

For vector rows that just need labels, use `:headers`:

```clojure
(table/table {:format :markdown
              :headers ["Name" "Qty"]
              :rows [["apple" 12] ["pear" 4]]})
```

For vector rows that need per-column options, omit `:from` from the map
form — the column's source is its position in `:columns`:

```clojure
{:columns [{:as "Name"}
           {:as "Qty"   :align :right}
           {:as "Price" :align :right :formatter #(format "$%.2f" %)}]
 :rows [["apple" 12 1.5]]}
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
              :columns [{:as "Text" :width 4 :overflow :ellipsis}]
              :rows [["abcdef"]]})
;; => ["Text"
;;     "a..."]
```

Wrapping creates additional rows:

```clojure
(table/table {:format :plain
              :columns [{:as "Txt" :width 3 :overflow :wrap}]
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
| `:tsv` | `escape/tsv-cell` |
| `:org` | `escape/org-cell` |
| `:rst` | `escape/rst-cell` |
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

## Cell Decoration

Pass `:cell-fn` to wrap each cell value before the layout engine pads it.
The callback receives a context map and must return a string:

| Context key | Meaning |
| --- | --- |
| `:section` | One of `:header`, `:data`, `:footer`. |
| `:row` | Zero-based row index within the section. |
| `:col` | Column index. |
| `:column` | The column spec map. |
| `:value` | The post-format/escape value, ready to wrap. |

```clojure
(table/table {:format :markdown
              :headers ["Name" "Qty"]
              :rows [["apple" "12"]]
              :cell-fn (fn [{:keys [section value]}]
                         (if (= :header section)
                           (str "**" value "**")
                           value))})
;; => ["| **Name** | **Qty** |"
;;     "|:-------- |:------- |"
;;     "| apple    | 12      |"]
```

When the callback adds non-printing characters such as ANSI color codes,
also pass `:display-width clj-string-layout.width/ansi-width` so the layout
engine pads using the original visible width rather than the byte length of
the wrapped value.

## Footers

Pass `:footers` (a vector of rows in the same shape as `:rows`) to render
trailing rows below the data. Footers are useful for totals, summaries, or
any row that should be styled the same as data but always appear last.

```clojure
(table/table {:format :box
              :headers ["Item" "Qty"]
              :rows [["apple" 12] ["pear" 4]]
              :footers [["Total" 16]]})
;; ┌───────┬─────┐
;; │ Item  │ Qty │
;; ├───────┼─────┤
;; │ apple │ 12  │
;; ├───────┼─────┤
;; │ pear  │ 4   │
;; ├───────┼─────┤
;; │ Total │ 16  │
;; └───────┴─────┘
```

Box-drawing formats automatically separate footers from the data with the
same interior rule used between data rows. For `:html`, footers are emitted
as `<tr><td>` rows after the data; wrap them in `<tfoot>` post-render if you
need the structural element.

## Title / Caption

Pass `:title` to render a caption above the table. For text formats the title
is centered to the rendered width and emitted as a single line; for `:html`
it becomes a `<caption>` element inside the `<table>`.

```clojure
(table/table {:format :box
              :title "Inventory"
              :headers ["Name" "Qty"]
              :rows [["apple" "12"]]})
;; =>
;;    Inventory
;; ┌───────┬─────┐
;; │ Name  │ Qty │
;; ├───────┼─────┤
;; │ apple │ 12  │
;; └───────┴─────┘
```

The title is escaped with the same per-format escaper unless `:escape? false`
is set on the spec.

## Width, Display Width, And Raw Output

`:width` and `:display-width` are forwarded to the layout engine for every
format that emits visually padded text (plain, markdown, the box variants,
ascii-grid, psql, org, and rst). They are intentionally ignored for `:html`,
since HTML output is structural markup rather than padded text.

Pass `:fill? true` together with `:width` to make the generated formats
expand their column padding toward that width:

```clojure
(table/table {:format :box
              :fill? true
              :width 25
              :headers ["Name" "Qty"]
              :rows [["apple" "12"]]})
;; ┌────────────┬──────────┐
;; │ Name       │ Qty      │
;; ├────────────┼──────────┤
;; │ apple      │ 12       │
;; └────────────┴──────────┘
```

`:raw?` is honored for every format. For visually padded formats it returns
each row as a vector of pieces ready for cell decoration; for `:html` each
output line becomes a single-piece vector so callers can wrap or annotate
specific lines without re-parsing the rendered string.

## When To Use The DSL Directly

Use the high-level table API when you need common output formats quickly. Use
`clj-string-layout.core/layout` directly when you need custom borders, multiple
repeat groups, custom row predicates, or a format not represented by the table
registry.
