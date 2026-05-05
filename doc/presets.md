# Preset Layout Catalog

Built-in layouts live in `clj-string-layout.presets` and are also re-exported
from `clj-string-layout.layout` for backwards compatibility. Presets are plain
layout config maps, so they can be passed directly to `layout`, `layout-str`,
or `layout-seq`, or `assoc`'d with options such as `:width`, `:display-width`,
`:col-widths`, or `:raw?`.

For the higher-level named-format API, see [the table API guide](table-api.md)
and [the examples gallery](examples-gallery.md).

```clojure
(require '[clj-string-layout.core :refer [layout]]
         '[clj-string-layout.presets :as presets])
```

## Plain Column Layouts

| Var | Alignment | Behavior |
| --- | --- | --- |
| `layout-plain-left` | Left | Two spaces between columns, no borders. |
| `layout-plain-center` | Center | Two spaces between columns, no borders. |
| `layout-plain-right` | Right | Two spaces between columns, no borders. |

## Separated-Value Layouts

| Var | Separator | Behavior |
| --- | --- | --- |
| `layout-tsv` | Tab | Verbatim tab-separated cells. Escape with `escape/tsv-cell`. |
| `layout-csv` | Comma | Verbatim comma-separated cells. Escape with `escape/csv-cell`. |
| `layout-pipe-separated` | Pipe | Compact pipe-separated cells without Markdown header rows. |

## Box-Drawing Layouts

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-ascii-box-left` | Left | No |
| `layout-ascii-box-center` | Center | No |
| `layout-ascii-box-right` | Right | No |
| `layout-ascii-box-fill-left` | Left | Yes |
| `layout-ascii-box-fill-center` | Center | Yes |
| `layout-ascii-box-fill-right` | Right | Yes |

## Norton Commander-Style Layouts

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-norton-commander-left` | Left | No |
| `layout-norton-commander-center` | Center | No |
| `layout-norton-commander-right` | Right | No |
| `layout-norton-commander-fill-left` | Left | Yes |
| `layout-norton-commander-fill-center` | Center | Yes |
| `layout-norton-commander-fill-right` | Right | Yes |

## ASCII Grid Layouts

ASCII grid layouts use only `+`, `-`, and `|`.

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-ascii-grid-left` | Left | No |
| `layout-ascii-grid-center` | Center | No |
| `layout-ascii-grid-right` | Right | No |
| `layout-ascii-grid-fill-left` | Left | Yes |
| `layout-ascii-grid-fill-center` | Center | Yes |
| `layout-ascii-grid-fill-right` | Right | Yes |

## Terminal And Documentation Layouts

| Var | Format | Behavior |
| --- | --- | --- |
| `layout-psql-left` | PostgreSQL psql | Left-aligned cells with a separator after the first row. |
| `layout-psql-right` | PostgreSQL psql | Right-aligned cells with a separator after the first row. |
| `layout-rst-simple` | reStructuredText | Simple table with top, header, and bottom rules. |
| `layout-org-left` | Org mode | Left-aligned table with a separator after the first row. |
| `layout-org-right` | Org mode | Right-aligned table with a separator after the first row. |

## Markdown Layouts

| Var | Alignment | Fill-aware |
| --- | --- | --- |
| `layout-markdown-left` | Left | No |
| `layout-markdown-center` | Center | No |
| `layout-markdown-right` | Right | No |
| `layout-markdown-fill-left` | Left | Yes |
| `layout-markdown-fill-center` | Center | Yes |
| `layout-markdown-fill-right` | Right | Yes |

## HTML Layouts

| Var | Behavior |
| --- | --- |
| `layout-html-table` | Emits `<table>`, one `<tr>` per input row, and verbatim `<td>` contents. |
| `layout-html-table-readable` | Same shape, but left-aligns cell contents for more readable source output. |

```clojure
(layout "Alice why\na raven" presets/layout-html-table)
;; => ["<table>"
;;     "  <tr><td>Alice</td><td>why</td></tr>"
;;     "  <tr><td>a</td><td>raven</td></tr>"
;;     "</table>"]
```

## Escaping Untrusted Cells

Markup and separated-value presets emit cell contents verbatim. Escape input
cells before rendering when the data is not already safe for the target format:

```clojure
(require '[clj-string-layout.escape :as escape])

(layout (escape/map-cells escape/html [["<Alice>" "tea & cake"]])
        presets/layout-html-table)
;; => ["<table>"
;;     "  <tr><td>&lt;Alice&gt;</td><td>tea &amp; cake</td></tr>"
;;     "</table>"]

(layout (escape/map-cells escape/markdown-cell [["name" "a|b"]])
        presets/layout-markdown-left)
;; => ["| name | a\\|b |"
;;     "|:---- |:----- |"]
```

Additional helpers include `escape/tsv-cell`, `escape/org-cell`,
`escape/rst-cell`, and `escape/log-safe`.
