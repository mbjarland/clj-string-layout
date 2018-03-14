# clj-string-layout
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0) 
[![Current Version](https://img.shields.io/clojars/v/mbjarland/clj-string-layout.svg)](https://clojars.org/mbjarland/clj-string-layout)

A clojure library designed to format string data into table-like formats using a flexible layout language. 

# Installation

The latest release version of clj-string-layout is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/mbjarland/clj-string-layout/latest-version.svg)](https://clojars.org/mbjarland/clj-string-layout)
 

## Example Usage
First let's define some sample string data: 

```clojure
(def data (str "Alice, why is\n" 
               "a raven like\n"
               "a writing desk?")
```

string data can either be provided as a string where 
the word and line delimters (default to \space and \newline) 
are configurable _or_ for more fine grained control, as a vector 
of vectors of strings. The above could thus equally well have been 
provided as: 

```clojure 
(def data [["Alice," "why" "is"]
           ["a" "raven" "like"]
           ["a" "writing" "desk?"]])
```

Now lets lay call clj-string-layout to lay this out using some 
sample layout configurations. Explanations for the layout configurations 
can be found further down in this document. 

#### Example 1: fixed column-count layout
With date left justified:

```clojure 
(layout
  data 
  {:layout {:cols ["[L] [L] [L]"]}})
=> ["Alice, why     is   " 
    "a      raven   like " 
    "a      writing desk?"]
```

#### Example 2: dynamic column-count ascii box layout
With data center aligned: 

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

#### Example 3: dynamic column-count norton commander style layout
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

#### Example 4: Markdown table layout
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

#### Exapmle 5: Html table layout
This library is capable of producing html tables:

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

