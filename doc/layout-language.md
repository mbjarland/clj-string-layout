# Layout Language Reference

The layout language is a compact DSL for describing how data cells, delimiter
text, repeated column patterns, and virtual rows are assembled.

```clojure
(require '[clj-string-layout.core :refer [layout parse-layout explain-layout]]
         '[clj-string-layout.predicates :as pred])
```

## Mental Model

A layout has two layers:

| Layer | Config Path | Purpose |
| --- | --- | --- |
| Column layout | `[:layout :cols]` | Renders each input data row. |
| Row layouts | `[:layout :rows]` | Insert virtual rows before, between, or after data rows. |

Column layouts decide where cell values go. Row layouts draw table borders,
header separators, and other rows that are derived from column widths rather
than direct input values.

## Syntax Summary

| Form | Example | Meaning |
| --- | --- | --- |
| Literal text | `" | "` | Emitted exactly. |
| Column marker | `[L]`, `[C]`, `[R]`, `[V]` | Placeholder for a data cell in column layouts. |
| Row marker | `[-]`, `[=]`, `[*]` | Repeats the marker character to match a column width in row layouts. |
| Fill marker | `f` or `F` | Absorbs extra width. |
| Repeat group | `{ [L] |}` | Repeats a sub-layout for selected columns. |
| Escaped literal | `\f`, `\{`, `\]` | Emits a reserved character literally. |

The grammar is:

```clojure
layout = delim? ((col | repeat) delim?)*
repeat  = <'{'> delim? (col delim?)* <'}'>
delim   = (escaped | fill | #'[^\\\[\]{}fF]+')+
escaped = <'\\'> #'.'
fill   = <'F'> (#'[\d]+')?
col    = <'['> fill? align fill? <']'>
```

## Column Markers

Column markers are placeholders for data cells.

| Marker | Behavior |
| --- | --- |
| `[L]` | Left-align within the computed column width. |
| `[C]` | Center-align within the computed column width. |
| `[R]` | Right-align within the computed column width. |
| `[V]` | Verbatim output; no padding is added. |

```clojure
(layout [["name" "qty" "price"]
         ["apple" "12" "$1.50"]]
        {:layout {:cols ["[L]  [R]  [R]"]}})
;; => ["name   qty  price"
;;     "apple   12  $1.50"]
```

Use `[V]` for delimiter-separated formats where computed widths should not add
padding:

```clojure
(layout [["a" "b" "c"]]
        {:layout {:cols ["{[V]}{,[V]}"
                         :repeat-for [pred/first-col? pred/not-first-col?]]}})
;; => ["a,b,c"]
```

## Literal Text

Anything outside brackets and braces is delimiter text, unless it is a reserved
fill marker or repeat delimiter.

```clojure
(layout [["a" "b"]]
        {:layout {:cols ["| [L] | [R] |"]}})
;; => ["| a | b |"]
```

Use backslash escapes when literal text needs reserved characters:

```clojure
(layout [["x"]]
        {:layout {:cols ["\\{[L]\\}"]}})
;; => ["{x}"]

(layout [["x"]]
        {:layout {:cols ["\\f[L]\\F"]}})
;; => ["fxF"]
```

## Fill Markers

Fill markers consume remaining width when `:width` is wider than the data and
literal text.

```clojure
(layout [["left" "right"]]
        {:width 20
         :fill-char \.
         :layout {:cols ["[L]f[R]"]}})
;; => ["left...........right"]
```

Multiple fill markers split extra width as evenly as possible:

```clojure
(layout [["x" "y"]]
        {:width 9
         :fill-char \-
         :layout {:cols ["f[L]f[R]f"]}})
;; => ["--x--y---"]
```

Fill markers can also appear inside column brackets. This expands the padding
inside the column rather than creating a separate delimiter region.

```clojure
(layout [["a" "b"]]
        {:width 10
         :fill-char \.
         :layout {:cols ["[Lf] [Rf]"]}})
```

## Repeat Groups

Repeat groups make one layout work for any number of columns. They are selected
by predicates passed with `:repeat-for`.

The simplest pattern repeats the same cell shape for every column:

```clojure
(layout [["a" "b" "c"]]
        {:layout {:cols ["|{ [C] |}"
                         :repeat-for [pred/all-cols?]]}})
;; => ["| a | b | c |"]
```

Use separate repeat groups for first, interior, and last columns:

```clojure
(layout [["a" "b" "c"]]
        {:layout {:cols ["{\\[[L]\\]}{|[L]}{|\\[[L]\\]}"
                         :repeat-for [pred/first-col?
                                      pred/interior-col?
                                      pred/last-col?]]}})
;; => ["[a]|b|[c]"]
```

Use first/not-first groups for delimiter-separated values without trailing
delimiters:

```clojure
(layout [["a" "b" "c"]]
        {:layout {:cols ["{[V]}{\t[V]}"
                         :repeat-for [pred/first-col? pred/not-first-col?]]}})
;; => ["a\tb\tc"]
```

## Row Layouts

Row layouts insert virtual rows. They use the same syntax, but bracket markers
mean drawing characters rather than data alignment.

```clojure
(layout [["a" "b"]]
        {:layout {:cols ["|{ [L] |}" :repeat-for [pred/all-cols?]]
                  :rows [["+{-[-]-+}"
                          :apply-for pred/all-rows?]]}})
;; => ["+---+---+"
;;     "| a | b |"
;;     "+---+---+"]
```

For row layouts, `[-]` emits enough `-` characters to match the corresponding
data column width. Literal text around the marker accounts for cell padding and
separators.

## Row Predicates

Row predicates receive `[idx last-idx]` for virtual row positions.

| Position | Meaning with two data rows |
| --- | --- |
| `0` | Before the first data row. |
| `1` | Between row 0 and row 1. |
| `2` | After the last data row. |

```clojure
(layout [["name" "qty"]
         ["apple" "12"]]
        {:layout {:cols ["| [L] | [R] |"]
                  :rows [["| [-] | [-] |" :apply-for pred/second-row?]]}})
;; => ["| name  | qty |"
;;     "| ----- | --- |"
;;     "| apple |  12 |"]
```

## Common Patterns

Plain aligned columns:

```clojure
{:layout {:cols ["{[L]}{  [L]}"
                 :repeat-for [pred/first-col? pred/not-first-col?]]}}
```

CSV shape without escaping:

```clojure
{:layout {:cols ["{[V]}{,[V]}"
                 :repeat-for [pred/first-col? pred/not-first-col?]]}}
```

Markdown pipe table shape:

```clojure
{:layout {:cols ["|{ [L] |}" :repeat-for [pred/all-cols?]]
          :rows [["|{:[-] |}" :apply-for pred/second-row?]]}}
```

ASCII grid shape:

```clojure
{:layout {:cols ["|{ [L] |}" :repeat-for [pred/all-cols?]]
          :rows [["+{-[-]-+}" :apply-for pred/all-rows?]]}}
```

## Diagnostics

Use `parse-layout` to inspect how a layout is interpreted:

```clojure
(parse-layout "{[V]}{,[V]}")
```

Use `explain-layout` to capture parse errors without throwing:

```clojure
(explain-layout "[x]")
;; => {:valid? false, :message "...", :data {:type :layout-parse-error, ...}}
```

## Troubleshooting

| Symptom | Likely Fix |
| --- | --- |
| Literal `f` becomes fill space | Escape it as `\f`. |
| Repeat group errors | Add matching `:repeat-for` predicates. |
| Extra delimiter at row end | Use first/not-first repeat groups instead of `all-cols?`. |
| Markdown or HTML breaks on data | Escape cells with `clj-string-layout.escape`. |
| Huge input is retained | Use `layout-seq` with explicit `:col-widths`. |
