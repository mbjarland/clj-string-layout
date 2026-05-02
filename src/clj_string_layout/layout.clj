(ns clj-string-layout.layout)

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



(def layout-ascii-box-left
  {:layout {:cols   ["│{ [L] │} [L] │" :apply-for [all-cols?]]
            :rows  [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                    ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                    ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def layout-ascii-box-center
  {:layout {:cols  ["│{ [C] │} [C] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def layout-ascii-box-right
  {:layout {:cols  ["│{ [R] │} [R] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})

(def layout-ascii-box-fill-left
  {:layout {:cols   ["│{ [Lf] │} [Lf] │" :apply-for [all-cols?]]
            :rows  [["┌{─[─f]─┬}─[─f]─┐" :apply-for first-row?]
                    ["├{─[─f]─┼}─[─f]─┤" :apply-for interior-row?]
                    ["└{─[─f]─┴}─[─f]─┘" :apply-for last-row?]]}})
(def layout-ascii-box-fill-center
  {:layout {:cols  ["│{ [Cf] │} [Cf] │" :apply-for [all-cols?]]
            :rows [["┌{─[─f]─┬}─[─f]─┐" :apply-for first-row?]
                   ["├{─[─f]─┼}─[─f]─┤" :apply-for interior-row?]
                   ["└{─[─f]─┴}─[─f]─┘" :apply-for last-row?]]}})
(def layout-ascii-box-fill-right
  {:layout {:cols  ["│{ [Rf] │} [Rf] │" :apply-for [all-cols?]]
            :rows [["┌{─[─f]─┬}─[─f]─┐" :apply-for first-row?]
                   ["├{─[─f]─┼}─[─f]─┤" :apply-for interior-row?]
                   ["└{─[─f]─┴}─[─f]─┘" :apply-for last-row?]]}})


(def layout-norton-commander-left
  {:layout {:cols  ["║{ [L] │} [L] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def layout-norton-commander-center
  {:layout {:cols  ["║{ [C] │} [C] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def layout-norton-commander-right
  {:layout {:cols  ["║{ [R] │} [R] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})

(def layout-norton-commander-fill-left
  {:layout {:cols  ["║{ [Lf] │} [Lf] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                   ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                   ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})
(def layout-norton-commander-fill-center
  {:layout {:cols  ["║{ [Cf] │} [Cf] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                   ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                   ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})
(def layout-norton-commander-fill-right
  {:layout {:cols  ["║{ [Rf] │} [Rf] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                   ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                   ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})


(def layout-markdown-left
  {:layout {:cols  ["|{ [L] |}" :apply-for [all-cols?]]
            :rows [["|{:[-] |}" :apply-for second-row?]]}})
(def layout-markdown-center
  {:layout {:cols  ["|{ [C] |}" :apply-for [all-cols?]]
            :rows [["|{:[-]:|}" :apply-for second-row?]]}})
(def layout-markdown-right
  {:layout {:cols  ["|{ [R] |}" :apply-for [all-cols?]]
            :rows [["|{ [-]:|}" :apply-for second-row?]]}})

(def layout-markdown-fill-left
  {:layout {:cols  ["|{ [Lf] |}" :apply-for [all-cols?]]
            :rows [["|{:[-f] |}" :apply-for second-row?]]}})
(def layout-markdown-fill-center
  {:layout {:cols  ["|{ [Cf] |}" :apply-for [all-cols?]]
            :rows [["|{:[-f]:|}" :apply-for second-row?]]}})
(def layout-markdown-fill-right
  {:layout {:cols  ["|{ [Rf] |}" :apply-for [all-cols?]]
            :rows [["|{ [-f]:|}" :apply-for second-row?]]}})


(def layout-html-table
  {:layout {:cols  ["  <tr>{<td>[V]</td>}</tr>" :apply-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table>" :apply-for last-row?]]}})

(def layout-html-table-readable
  {:layout {:cols  ["  <tr>{<td>[L]</td>}</tr>" :apply-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table>" :apply-for last-row?]]}})
