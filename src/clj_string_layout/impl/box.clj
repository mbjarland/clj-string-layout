(ns ^:no-doc clj-string-layout.impl.box
  "Shared box-drawing character sets and layout-string builders.

  Both clj-string-layout.presets (column-count-agnostic, repeat-for based) and
  clj-string-layout.table (per-column alignment, fully expanded) consume from
  here so adding or tweaking a box style is a one-place change."
  (:require [clojure.string :as str]))

(def box-chars
  "Single-line Unicode box-drawing character set."
  {:cols {:left "│" :sep "│" :right "│"}
   :top {:left "┌" :fill "─" :sep "┬" :right "┐"}
   :middle {:left "├" :fill "─" :sep "┼" :right "┤"}
   :bottom {:left "└" :fill "─" :sep "┴" :right "┘"}})

(def double-box-chars
  "Double-line Unicode box-drawing character set."
  {:cols {:left "║" :sep "║" :right "║"}
   :top {:left "╔" :fill "═" :sep "╦" :right "╗"}
   :middle {:left "╠" :fill "═" :sep "╬" :right "╣"}
   :bottom {:left "╚" :fill "═" :sep "╩" :right "╝"}})

(def norton-commander-chars
  "Norton Commander-style mixed double/single box characters."
  {:cols {:left "║" :sep "│" :right "║"}
   :top {:left "╔" :fill "═" :sep "╤" :right "╗"}
   :middle {:left "╟" :fill "─" :sep "┼" :right "╢"}
   :bottom {:left "╚" :fill "═" :sep "╧" :right "╝"}})

(def ascii-grid-chars
  "Plain ASCII +---+ grid characters."
  {:cols {:left "|" :sep "|" :right "|"}
   :top {:left "+" :fill "-" :sep "+" :right "+"}
   :middle {:left "+" :fill "-" :sep "+" :right "+"}
   :bottom {:left "+" :fill "-" :sep "+" :right "+"}})

(defn aligned-cols
  "Build a fully-expanded box-drawing column layout from per-column align tokens.

  align-tokens is a sequence of single-character strings such as \"L\" or \"R\".
  Returns a layout string like \"│ [L] │ [R] │\"."
  [{:keys [left sep right]} align-tokens]
  (let [tokens (map #(str "[" % "]") align-tokens)]
    (str left " " (str/join (str " " sep " ") tokens) " " right)))

(defn aligned-rule
  "Build a fully-expanded box-drawing rule for n columns.

  Returns a layout string like \"┌─[─]─┬─[─]─┐\"."
  [{:keys [left fill sep right]} n]
  (let [marker (str "[" fill "]")]
    (str left fill (str/join (str fill sep fill) (repeat n marker)) fill right)))
