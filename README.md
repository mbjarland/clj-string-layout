# clj-string-layout

[![CI Status](https://github.com/mbjarland/clj-string-layout/actions/workflows/ci.yml/badge.svg)](https://github.com/mbjarland/clj-string-layout/actions)
[![Release](https://github.com/mbjarland/clj-string-layout/actions/workflows/release.yml/badge.svg)](https://github.com/mbjarland/clj-string-layout/actions/workflows/release.yml)
[![Clojars](https://img.shields.io/clojars/v/io.github.mbjarland/clj-string-layout.svg)](https://clojars.org/io.github.mbjarland/clj-string-layout)
[![cljdoc](https://cljdoc.org/badge/io.github.mbjarland/clj-string-layout)](https://cljdoc.org/d/io.github.mbjarland/clj-string-layout/CURRENT)
[![License](https://img.shields.io/badge/license-EPL--1.0-blue.svg)](LICENSE)

`clj-string-layout` turns rows of strings into aligned text — Markdown
tables, box-drawing terminal output, CSV, HTML, psql, Org mode, and
anything you can build out of the underlying layout DSL.

## One spec, many shapes

Describe the data once:

```clojure
(require '[clj-string-layout.table :as table])

(def items
  [{:item "apple" :qty 12 :price 1.50}
   {:item "pear"  :qty  4 :price 2.00}
   {:item "kiwi"  :qty  8 :price 0.75}])

(def cols
  [:item
   {:from :qty   :as "Qty"   :align :right}
   {:from :price :as "Price" :align :right
    :formatter   #(format "$%.2f" %)}])
```

Render it any way you need.

#### Box drawing — terminals, docs, REPL output

```clojure
(table/table-str {:format :box :columns cols :rows items})
```

```text
┌───────┬─────┬───────┐
│ item  │ Qty │ Price │
├───────┼─────┼───────┤
│ apple │  12 │ $1.50 │
├───────┼─────┼───────┤
│ pear  │   4 │ $2.00 │
├───────┼─────┼───────┤
│ kiwi  │   8 │ $0.75 │
└───────┴─────┴───────┘
```

#### Markdown — READMEs, issue templates, generated docs

```clojure
(table/table-str {:format :markdown :columns cols :rows items})
```

```markdown
| item  | Qty | Price |
|:----- | ---:| -----:|
| apple |  12 | $1.50 |
| pear  |   4 | $2.00 |
| kiwi  |   8 | $0.75 |
```

#### HTML — emails, reports, server output

```clojure
(table/table-str {:format :html :columns cols :rows items})
```

```html
<table>
  <tr><th>item</th><th>Qty</th><th>Price</th></tr>
  <tr><td>apple</td><td>12</td><td>$1.50</td></tr>
  <tr><td>pear</td><td>4</td><td>$2.00</td></tr>
  <tr><td>kiwi</td><td>8</td><td>$0.75</td></tr>
</table>
```

#### CSV — handoff to spreadsheets, downstream tools

```clojure
(table/table-str {:format :csv :columns cols :rows items})
```

```text
item,Qty,Price
apple,12,$1.50
pear,4,$2.00
kiwi,8,$0.75
```

The same `cols` and `items` also render as `:double-box`, `:ascii-grid`,
`:psql`, `:org`, `:rst`, `:tsv`, `:pipe`, `:plain`, and the three
alignment-specific `:markdown-*` variants. The
[examples gallery](doc/examples-gallery.md) shows every named format
side by side with the same data.

## Install

```clojure
;; deps.edn
{:deps {io.github.mbjarland/clj-string-layout {:mvn/version "2.1.0"}}}
```

No third-party Clojure runtime dependencies — a Babashka script can
require it directly:

```clojure
#!/usr/bin/env bb
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {io.github.mbjarland/clj-string-layout
                        {:mvn/version "2.1.0"}}})

(require '[clj-string-layout.table :as table])
(println (table/table-str {:format :box
                           :headers ["Name" "Qty"]
                           :rows [["apple" 12] ["pear" 4]]}))
```

Tested on Java 11, 17, and 21.

## Two layers

The example above is the high-level table API. Underneath it sits a
small layout DSL — the engine every named format is built on, and
the thing you reach for when you want text to line up in ways the
table API doesn't quite cover (custom borders, ANSI dashboards,
log lines with aligned timestamps, key/value pairs, anything).

```text
┌─ HIGH ────────────────────────────────────────────────────────────────┐
│ clj-string-layout.table                                               │
│ (table/table {:format :box :columns [...] :rows [...]})               │
│ → Most callers start (and stop) here.                                 │
├───────────────────────────────────────────────────────────────────────┤
│ clj-string-layout.core/layout                                         │
│ (layout rows {:layout {:cols ["[L] [R]"]}})                           │
│ → For anything the table API doesn't reach.                           │
│   Pre-canned configs in clj-string-layout.presets.                    │
└─ LOW ─────────────────────────────────────────────────────────────────┘
```

Deeper docs: [table API](doc/table-api.md) ·
[examples gallery](doc/examples-gallery.md) ·
[layout language](doc/layout-language.md) ·
[recipes](doc/recipes.md) ·
[preset catalog](doc/presets.md) ·
[CLI](doc/cli.md) · [errors](doc/errors.md).

## Table API reference

Four moving parts.

#### Rows

A vector of maps:

```clojure
:rows [{:item "apple" :qty 12} {:item "pear" :qty 4}]
```

…or a vector of positional vectors:

```clojure
:rows [["apple" 12] ["pear" 4]]
```

#### Columns

Two shapes:

```clojure
:qty                                                  ;; defaults
{:from :qty :as "Qty" :align :right                   ;; explicit
 :formatter #(format "%s units" %)
 :width 10 :overflow :ellipsis}
```

Map keys:

| Key | Means |
| --- | --- |
| `:from` | Row map key. Omit for vector rows — the column's source is its position. |
| `:as` | Header label. Defaults to the keyword name of `:from`. |
| `:align` | `:left`, `:center`, `:right`, or `:verbatim`. |
| `:formatter` | One-arg function applied to the cell value before rendering. |
| `:width` | Maximum cell width. |
| `:overflow` | `:none`, `:clip`, `:ellipsis`, `:wrap`, `:error`. |

#### Format

A keyword. One of:

`:plain` · `:markdown` · `:markdown-left` · `:markdown-center` ·
`:markdown-right` · `:box` · `:double-box` · `:ascii-grid` · `:csv` ·
`:tsv` · `:pipe` · `:psql` · `:org` · `:rst` · `:html`.

#### Extras

| Key | Adds |
| --- | --- |
| `:title` | Centered caption above text formats; `<caption>` for HTML. |
| `:footers` | Trailing rows that share column treatment. |
| `:cell-fn` | Per-cell decoration callback — pair with `:display-width` for ANSI styling. |
| `:width` + `:fill?` | Expand the rendered table toward a target width. |
| `:raw?` | Return vectors of pieces instead of joined strings. |

See the [table API guide](doc/table-api.md) for the full surface.

## Layout DSL

When the named formats don't reach, drop down to the layout DSL. The
column layout is a string made of column markers (`[L]` `[C]` `[R]`
`[V]`), fill regions (`f`), and optional repeat groups (`{…}`):

```clojure
(require '[clj-string-layout.core :refer [layout]])

(layout [["left" "right"]]
        {:width     30
         :fill-char \.
         :layout    {:cols ["[L]f[R]"]}})
;; => ["left.....................right"]
```

The string above has two column markers (`[L]` and `[R]`) and one fill
region (`f`) between them — extra width is absorbed by the fill so the
right-hand value sits flush against the target width. The DSL also
handles virtual row layouts (top/middle/bottom rules), repeat groups
for variable column counts, raw-piece output for cell decoration, and
streaming via `layout-seq`. See the
[layout language reference](doc/layout-language.md) for the grammar
and the [recipe book](doc/recipes.md) for paste-able examples.

## Command line

```sh
clojure -M:cli -- --input csv --format markdown --headers data.csv
bb bb-format --input tsv --format box < data.tsv
```

`bb bb-format` runs natively under Babashka in ~50 ms instead of the
JVM path's ~700 ms. See the [CLI guide](doc/cli.md) for the full
option list and the programmatic `cli/render` entry point.

## Development

Local checks, code expectations, and the release process live in
[CONTRIBUTING.md](CONTRIBUTING.md). Documentation is rebuilt on every
release at
[cljdoc.org](https://cljdoc.org/d/io.github.mbjarland/clj-string-layout/CURRENT).

## License

Copyright © 2017-2026 Matias Bjarland · Eclipse Public License 1.0
