(ns string-layout.layout)

; layout predicates
(defn first-row? [[idx _]] (zero? idx))
(def not-first-row? (complement first-row?))

(defn second-row? [[idx _]] (= idx 1))
(defn last-row? [[idx last]] (= idx last))
(def not-last-row? (complement last-row?))

(defn interior-row? [pair] (and (not-first-row? pair)
                                      (not-last-row? pair)))
(def not-interior-row? (complement interior-row?))

(defn all-rows? [[_ _]] true)

; there is no difference between the row/col predicates,
; we duplicate them that way for clarity
(def first-col? first-row?)
(def not-first-col? not-first-row?)
(def second-col? second-row?)
(def last-col? last-row?)
(def not-last-col? not-last-row?)
(def interior-col? interior-row?)
(def not-interior-col? not-interior-row?)
(def all-cols? all-rows?)



(def ascii-box-layout-left
  {:layout {:cols  ["│{ [L] │} [L] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def ascii-box-layout-center
  {:layout {:cols  ["│{ [C] │} [C] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})
(def ascii-box-layout-right
  {:layout {:cols  ["│{ [R] │} [R] │" :apply-for [all-cols?]]
            :rows [["┌{─[─]─┬}─[─]─┐" :apply-for first-row?]
                   ["├{─[─]─┼}─[─]─┤" :apply-for interior-row?]
                   ["└{─[─]─┴}─[─]─┘" :apply-for last-row?]]}})

(def norton-commander-layout-left
  {:layout {:cols  ["║{ [L] │} [L] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def norton-commander-layout-center
  {:layout {:cols  ["║{ [C] │} [C] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})
(def norton-commander-layout-right
  {:layout {:cols  ["║{ [R] │} [R] ║" :apply-for [all-cols?]]
            :rows [["╔{═[═]═╤}═[═]═╗" :apply-for first-row?]
                   ["╟{─[─]─┼}─[─]─╢" :apply-for interior-row?]
                   ["╚{═[═]═╧}═[═]═╝" :apply-for last-row?]]}})


(def markdown-layout-left
  {:layout {:cols ["|{ [L] |}"  :apply-for [all-cols?]]
            :rows [["|{:[-] |}" :apply-for second-row?]]}})
(def markdown-layout-center
  {:layout {:cols ["|{ [C] |}"  :apply-for [all-cols?]]
            :rows [["|{:[-]:|}" :apply-for second-row?]]}})
(def markdown-layout-right
  {:layout {:cols ["|{ [R] |}"  :apply-for [all-cols?]]
            :rows [["|{ [-]:|}" :apply-for second-row?]]}})

(def html-table-layout
  {:layout {:cols ["  <tr>{<td>[V]</td>}</tr>" :apply-for [all-cols?]]
            :rows [["<table>" :apply-for first-row?]
                   ["</table" :apply-for last-row?]]}})
