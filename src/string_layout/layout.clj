(ns string-layout.layout)

; layout predicates
(defn -first? [[idx _]] (zero? idx))
(def -not-first? (complement -first?))

(defn -second? [[idx _]] (= idx 1))
(defn -last? [[idx last]] (= idx last))
(def -not-last? (complement -last?))
(defn -interior? [pair] (and (-not-first? pair)
                             (-not-last? pair)))
(def -not-interior? (complement -interior?))
(defn -all? [_] true)

; row layout predicates
(def first-row? -first?)
(def not-first-row? -not-first?)
(def second-row? -second?)
(def last-row? -last?)
(def not-last-row? -not-last?)
(def interior-row? -interior?)
(def not-interior-row? -not-interior?)
(def all-rows? -all?)

; col layout predicates
(def first-col? -first?)
(def not-first-col? -not-first?)
(def second-col? -second?)
(def last-col? -last?)
(def not-last-col? -not-last?)
(def interior-col? -interior?)
(def not-interior-col? -not-interior?)
(def all-cols? -all?)



(def ascii-box-layout-left
  {:layout {:cols ["│{ [L] │} [L] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def ascii-box-layout-center
  {:layout {:cols ["│{ [C] │} [C] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def ascii-box-layout-right
  {:layout {:cols ["│{ [R] │} [R] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})

(def norton-commander-layout-left
  {:layout {:cols ["║{ [L] │} [L] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def norton-commander-layout-center
  {:layout {:cols ["║{ [C] │} [C] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def norton-commander-layout-right
  {:layout {:cols ["║{ [R] │} [R] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})


(def markdown-layout-left
  {:layout {:cols ["|{ [L] |}" :apply-for [all-cols?]]
            :rows [["|{:[-] |}" :apply-for second-row?]]}})
(def markdown-layout-center
  {:layout {:cols ["|{ [C] |}" :apply-for [all-cols?]]
            :rows [["|{:[-]:|}" :apply-for second-row?]]}})
(def markdown-layout-right
  {:layout {:cols ["|{ [R] |}" :apply-for [all-cols?]]
            :rows [["|{ [-]:|}" :apply-for second-row?]]}})

(def html-table-layout
  {:layout {:cols ["  <tr>{<td>[V]</td>}</tr>" :apply-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table" :apply-for last-row?]]}})
