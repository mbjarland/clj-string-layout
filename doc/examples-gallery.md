# High-Level Table Examples Gallery

This gallery shows the high-level `clj-string-layout.table` API. It uses the
same data for each named table format so the output shapes are easy to compare.

Use this API when you want common named table formats without writing layout DSL
strings. For the lower-level `clj-string-layout.core/layout` API and custom DSL
layouts, see the [recipe book](recipes.md) and
[layout language reference](layout-language.md).

```clojure
(require '[clj-string-layout.table :as table])

(def sample
  {:headers ["Name" "Qty" "Price"]
   :rows [["apple" 12 "$1.50"]
          ["pear" 4 "$2.00"]]})
```

## Plain

```clojure
(table/table (assoc sample :format :plain))
```

```text
Name   Qty  Price
apple  12   $1.50
pear   4    $2.00
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

## ASCII Box

```clojure
(table/table (assoc sample :format :ascii-box))
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

## ASCII Double Box

```clojure
(table/table (assoc sample :format :ascii-double-box))
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

## CSV

```clojure
(table/table (assoc sample :format :csv))
```

```csv
Name,Qty,Price
apple,12,$1.50
pear,4,$2.00
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

## Pipe

```clojure
(table/table (assoc sample :format :pipe))
```

```text
Name|Qty|Price
apple|12|$1.50
pear|4|$2.00
```

## psql

```clojure
(table/table (assoc sample :format :psql))
```

```text
 Name   | Qty | Price
------+-----+------
 apple  | 12  | $1.50
 pear   | 4   | $2.00
```

## Org Mode

```clojure
(table/table (assoc sample :format :org))
```

```text
| Name  | Qty | Price |
|-----+---+-----|
| apple | 12  | $1.50 |
| pear  | 4   | $2.00 |
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

## Alignment

Column specs can align values independently of the selected format.

```clojure
(table/table {:format :markdown
              :columns [{:key :name :title "Name"}
                        {:key :qty :title "Qty" :align :right}
                        {:key :price :title "Price" :align :right}]
              :rows [{:name "apple" :qty 12 :price "$1.50"}
                     {:name "pear" :qty 4 :price "$2.00"}]})
```

```markdown
| Name  | Qty | Price |
|:----- | ---:| -----:|
| apple |  12 | $1.50 |
| pear  |   4 | $2.00 |
```
