# clj-string-layout
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0) 
[![Current Version](https://img.shields.io/clojars/v/mbjarland/clj-string-layout.svg)](https://clojars.org/mbjarland/clj-string-layout)

A clojure library designed to format string data into table-like formats using a flexible layout language. 

## Installation

The latest release version of clj-string-layout is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/mbjarland/clj-string-layout/latest-version.svg)](https://clojars.org/mbjarland/clj-string-layout)

## Usage 
In your leiningen project.clj file: 

```
[mbjarland/clj-string-layout "1.0.0-SNAPSHOT"]
```

in your source file: TODO: CHECK THIS!

```
  (require '[string-layout.core :refer [layout]])
```

## Dependencies 

```
[com.rpl/specter "1.0.5"]
   [riddley "0.1.12"]
 [instaparse "1.4.8"]
 [org.clojure/clojure "1.9.0"]
   [org.clojure/core.specs.alpha "0.1.24"]
   [org.clojure/spec.alpha "0.1.143"]
```
 

## Examples
First, define some sample string data: 

```clojure
(def data (str "Alice, why is\n" 
               "a raven like\n"
               "a writing desk?")
```

and now call string-layout to format this data using some sample layout configurations. Explanations for the layout configurations can be found further down in this document. 

#### Example 1 - left justified, fixed column count layout: 

```clojure 
(layout
  data 
  {:layout {:cols ["[L] [L] [L]"]}})

=> ["Alice, why     is   " 
    "a      raven   like " 
    "a      writing desk?"]
```

#### Example 2 - centered dynamic column-count ascii box layout:

```clojure
(layout 
  data 
  {:layout {:cols  ["│{ [C] │} [C] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})

=>
["┌────────┬─────────┬───────┐"
 "│ Alice, │   why   │   is  │"
 "├────────┼─────────┼───────┤"
 "│    a   │  raven  │  like │"
 "├────────┼─────────┼───────┤"
 "│    a   │ writing │ desk? │"
 "└────────┴─────────┴───────┘"]
```

#### Example 3 - norton commander style dynamic column-count layout
Data right justified in an ascii box layout where we fill 
the layout to a specific width and allocate an equal amount 
of space to all columns: 

```clojure
(layout 
  data
  {:width 50
   :layout {:cols  ["║{ [Rf] │} [Rf] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                   ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                   ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})

=>
["╔═══════════════╤════════════════╤═══════════════╗"
 "║        Alice, │            why │            is ║"
 "╟───────────────┼────────────────┼───────────────╢"
 "║             a │          raven │          like ║"
 "╟───────────────┼────────────────┼───────────────╢"
 "║             a │        writing │         desk? ║"
 "╚═══════════════╧════════════════╧═══════════════╝"]
```

#### Example 4 - markdown table layout
Data centered, markdown table headers centered, 
header data inserted, and column widths filled with equal 
distribution to the default 80 character width: 

```clojure 
(layout 
  (str "header_1 header_2 header_3" \newline data)
  {:layout {:cols  ["|{ [Cf] |}" :apply-for [all-cols?]]
            :rows [["|{:[-f]:|}" :apply-for second-row?]]}})

=>
["|         header_1        |         header_2        |         header_3         |"
 "|:-----------------------:|:-----------------------:|:------------------------:|"
 "|          Alice,         |           why           |            is            |"
 "|            a            |          raven          |           like           |"
 "|            a            |         writing         |           desk?          |"]
```

#### Example 5 - html table layout


```clojure
(layout 
  data
  {:layout {:cols  ["  <tr>{<td>[V]</td>}</tr>" :apply-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table" :apply-for last-row?]]}})

=>
["<table>"
 "  <tr><td>Alice,</td><td>why</td><td>is</td></tr>"
 "  <tr><td>a</td><td>raven</td><td>like</td></tr>"
 "  <tr><td>a</td><td>writing</td><td>desk?</td></tr>"
 "</table"]
```

## Layout Configurations
A layout configuration is a map containing a set of configuration options and layout strings for laying out columns and rows. 

A somewhat contrived but complete example layout config: 

```clojure
(def full-layout-config
  {:align-char      \*
   :fill-char       \space
   :word-split-char \space
   :row-split-char  \newline
   :width           80
   :raw?            false
   :layout {:cols  ["+[L]+[L]+[L]+"]
            :rows [["-[~]-[~]-[~]-" :apply-for all-rows?]]}})
```

using this to lay out our data from above gives us: 

```clojure 
(layout data full-layout-config)

=>
; +[L   ]+[L    ]+[L  ]+
;  ↓      ↓       ↓     
["-~~~~~~-~~~~~~~-~~~~~-"  ; ← 0 row layout
 "+Alice,+why****+is***+"
 "-~~~~~~-~~~~~~~-~~~~~-"  ; ← 1 row layout
 "+a*****+raven**+like*+"
 "-~~~~~~-~~~~~~~-~~~~~-"  ; ← 2 row layout
 "+a*****+writing+desk?+"
 "-~~~~~~-~~~~~~~-~~~~~-"] ; ← 3 row layout
```

(with comments added for clarity)

The layout language will be explained in detail below, but first let's go through the 
Options explained: 

* `align-char` - the widest word in a column defines the column width. All other words in that column will need to be aligned to the widest width. `align-char` is the character used to pad words to the correct width.
* `fill-char` - the layout engine is capable of "fill to width" functionality where the data is filled to a specific width (default 80 characters). This functionality is enabled by using the `f` fill specifier in the `:cols` and `:rows` layout strings. If any fills are detected, then `fill-char` is the default character used for the "fill to width" functionality. Note that for simplicity, no fill chars were used in the above example. 
* `word-split-char` - if in-data is specified as a string (see section on in-data), this character is used to split the string into "words".
* `row-split-char` - if in-data is specified as a string (see section on in-data), this character is used to split the string into rows.



```

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; └───────────────┴───────────────┴───────┘ ← 1
```

## String Data In
String data can either be provided as a string where 
the word and line delimters are configurable (default to \space and \newline): 

```clojure
(def data (str "Alice, why is\n" 
               "a raven like\n"
               "a writing desk?")
```

_or_ for more fine grained control, as a vector of vectors of strings. The 
above could thus equally well have been provided as: 

```clojure 
(def data [["Alice," "why" "is"]
           ["a" "raven" "like"]
           ["a" "writing" "desk?"]])
```

## License

Copyright © 2017 Matias Bjarland

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


--------------



TODO: describe
o high level aim - general layout, not specific to 
  any format
o parsing and normalizing of data
o layout-config syntax (width, word-split-char, etc) 
o col layout syntax
o row layout syntax
o repeating groups
o fills 
o predicates
o performance
o clojars release version 
o prepackaged layouts 

