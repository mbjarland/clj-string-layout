# High-Level Table Examples Gallery

This gallery shows the high-level `clj-string-layout.table` API. It uses the
same data for each named table format so the output shapes are easy to compare.

Use this API when you want common named table formats without writing layout DSL
strings. For the lower-level `clj-string-layout.core/layout` API and custom DSL
layouts, see the [recipe book](recipes.md) and
[layout language reference](layout-language.md).

```clojure
(require '[clj-string-layout.predicates :as pred]
         '[clj-string-layout.table :as table])

(def sample
  {:headers ["Name" "Qty" "Price"]
   :rows [["apple" 12 "$1.50"]
          ["pear" 4 "$2.00"]]})
```

The backing layout snippets below show the equivalent lower-level
`clj-string-layout.core/layout` config for the sample data. Most examples use
three left-aligned columns unless the section says otherwise. The high-level
table API still handles headers, map rows, escaping, and column specs before
rendering.

## Plain

```clojure
(table/table (assoc sample :format :plain))
```

```text
Name   Qty  Price
apple  12   $1.50
pear   4    $2.00
```

Backing layout:

```clojure
{:layout {:cols ["[L]  [L]  [L]"]}}
```

## Markdown

```clojure
(table/table (assoc sample :format :markdown))
```

```markdown
| Name  | Qty | Price |
|:----- |:--- |:----- |
| apple | 12  | $1.50 |
| pear  | 4   | $2.00 |
```

Backing layout:

```clojure
{:layout {:cols ["| [L] | [L] | [L] |"]
          :rows [["|:[-] |:[-] |:[-] |" :apply-for pred/second-row?]]}}
```

## Markdown Alignment Formats

Use `:markdown-left`, `:markdown-center`, or `:markdown-right` when every column
should use the same Markdown alignment. `:markdown` is the same as
`:markdown-left`.

```clojure
(table/table (assoc sample :format :markdown-center))
```

```markdown
|  Name | Qty | Price |
|:-----:|:---:|:-----:|
| apple |  12 | $1.50 |
|  pear |  4  | $2.00 |
```

```clojure
(table/table (assoc sample :format :markdown-right))
```

```markdown
|  Name | Qty | Price |
| -----:| ---:| -----:|
| apple |  12 | $1.50 |
|  pear |   4 | $2.00 |
```

Backing layouts:

```clojure
{:layout {:cols ["| [C] | [C] | [C] |"]
          :rows [["|:[-]:|:[-]:|:[-]:|" :apply-for pred/second-row?]]}}

{:layout {:cols ["| [R] | [R] | [R] |"]
          :rows [["| [-]:| [-]:| [-]:|" :apply-for pred/second-row?]]}}
```

## ASCII Grid

```clojure
(table/table (assoc sample :format :ascii-grid))
```

```text
+-------+-----+-------+
| Name  | Qty | Price |
+-------+-----+-------+
| apple | 12  | $1.50 |
+-------+-----+-------+
| pear  | 4   | $2.00 |
+-------+-----+-------+
```

Backing layout:

```clojure
{:layout {:cols ["| [L] | [L] | [L] |"]
          :rows [["+-[-]-+-[-]-+-[-]-+" :apply-for pred/all-rows?]]}}
```

## Box

```clojure
(table/table (assoc sample :format :box))
```

```text
┌───────┬─────┬───────┐
│ Name  │ Qty │ Price │
├───────┼─────┼───────┤
│ apple │ 12  │ $1.50 │
├───────┼─────┼───────┤
│ pear  │ 4   │ $2.00 │
└───────┴─────┴───────┘
```

Backing layout:

```clojure
{:layout {:cols ["│ [L] │ [L] │ [L] │"]
          :rows [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-for pred/first-row?]
                 ["├─[─]─┼─[─]─┼─[─]─┤" :apply-for pred/interior-row?]
                 ["└─[─]─┴─[─]─┴─[─]─┘" :apply-for pred/last-row?]]}}
```

Aliases: `:unicode-box` and `:ascii-box`.

## Double Box

```clojure
(table/table (assoc sample :format :double-box))
```

```text
╔═══════╦═════╦═══════╗
║ Name  ║ Qty ║ Price ║
╠═══════╬═════╬═══════╣
║ apple ║ 12  ║ $1.50 ║
╠═══════╬═════╬═══════╣
║ pear  ║ 4   ║ $2.00 ║
╚═══════╩═════╩═══════╝
```

Backing layout:

```clojure
{:layout {:cols ["║ [L] ║ [L] ║ [L] ║"]
          :rows [["╔═[═]═╦═[═]═╦═[═]═╗" :apply-for pred/first-row?]
                 ["╠═[═]═╬═[═]═╬═[═]═╣" :apply-for pred/interior-row?]
                 ["╚═[═]═╩═[═]═╩═[═]═╝" :apply-for pred/last-row?]]}}
```

Aliases: `:unicode-double-box` and `:ascii-double-box`.

## CSV

```clojure
(table/table (assoc sample :format :csv))
```

```csv
Name,Qty,Price
apple,12,$1.50
pear,4,$2.00
```

Backing layout:

```clojure
{:layout {:cols ["{[V]}{,[V]}" :repeat-for [pred/first-col? pred/not-first-col?]]}}
```

## TSV

```clojure
(table/table (assoc sample :format :tsv))
```

```text
Name	Qty	Price
apple	12	$1.50
pear	4	$2.00
```

Backing layout:

```clojure
{:layout {:cols ["{[V]}{\t[V]}" :repeat-for [pred/first-col? pred/not-first-col?]]}}
```

## Pipe

```clojure
(table/table (assoc sample :format :pipe))
```

```text
Name|Qty|Price
apple|12|$1.50
pear|4|$2.00
```

Backing layout:

```clojure
{:layout {:cols ["{[V]}{|[V]}" :repeat-for [pred/first-col? pred/not-first-col?]]}}
```

## psql

```clojure
(table/table (assoc sample :format :psql))
```

```text
 Name   | Qty | Price
--------+-----+--------
 apple  | 12  | $1.50
 pear   | 4   | $2.00
```

Backing layout:

```clojure
{:layout {:cols [" [L] { | [L]}" :repeat-for [pred/not-first-col?]]
          :rows [["-[-]-{-+-[-]}" :apply-for pred/second-row?]]}}
```

## Org Mode

```clojure
(table/table (assoc sample :format :org))
```

```text
| Name  | Qty | Price |
|-------+-----+-------|
| apple | 12  | $1.50 |
| pear  | 4   | $2.00 |
```

Backing layout:

```clojure
{:layout {:cols ["{| [L] }{| [L] }|" :repeat-for [pred/first-col? pred/not-first-col?]]
          :rows [["{|-[-]-}{+-[-]-}|" :apply-for pred/second-row?]]}}
```

## reStructuredText

```clojure
(table/table (assoc sample :format :rst))
```

```text
=====  ===  =====
Name   Qty  Price
=====  ===  =====
apple  12   $1.50
pear   4    $2.00
=====  ===  =====
```

Backing layout:

```clojure
{:layout {:cols ["{[L]}{  [L]}" :repeat-for [pred/first-col? pred/not-first-col?]]
          :rows [["{[=]}{  [=]}" :apply-for pred/first-row?]
                 ["{[=]}{  [=]}" :apply-for pred/second-row?]
                 ["{[=]}{  [=]}" :apply-for pred/last-row?]]}}
```

## HTML

```clojure
(table/table (assoc sample :format :html))
```

```html
<table>
  <tr><th>Name</th><th>Qty</th><th>Price</th></tr>
  <tr><td>apple</td><td>12</td><td>$1.50</td></tr>
  <tr><td>pear</td><td>4</td><td>$2.00</td></tr>
</table>
```

Backing renderer:

```clojure
{:format :html
 :layout :html}
```

The high-level HTML format is rendered directly so it can emit `<th>` for the
header row and `<td>` for data rows. Lower-level HTML presets are available when
you want to render every row as data cells with the layout DSL.

## Alignment

Column specs can align values independently of the selected format.

```clojure
(table/table {:format :markdown
              :columns [{:from :name  :as "Name"}
                        {:from :qty   :as "Qty"   :align :right}
                        {:from :price :as "Price" :align :right}]
              :rows [{:name "apple" :qty 12 :price "$1.50"}
                     {:name "pear" :qty 4 :price "$2.00"}]})
```

```markdown
| Name  | Qty | Price |
|:----- | ---:| -----:|
| apple |  12 | $1.50 |
| pear  |   4 | $2.00 |
```

Backing layout for those column alignments:

```clojure
{:layout {:cols ["| [L] | [R] | [R] |"]
          :rows [["|:[-] | [-]:| [-]:|" :apply-for pred/second-row?]]}}
```
