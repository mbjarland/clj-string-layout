(ns clj-string-layout.layout
  (:require [clj-string-layout.predicates :as pred]
            [clj-string-layout.presets :as presets]))

;; This namespace keeps the historical API that exposed both predicates and
;; built-in layouts from one place. New code can require predicates and presets
;; directly when it wants clearer namespaces.

(def first-row?
  "Compatibility alias for clj-string-layout.predicates/first-row?."
  pred/first-row?)

(def not-first-row?
  "Compatibility alias for clj-string-layout.predicates/not-first-row?."
  pred/not-first-row?)

(def second-row?
  "Compatibility alias for clj-string-layout.predicates/second-row?."
  pred/second-row?)

(def last-row?
  "Compatibility alias for clj-string-layout.predicates/last-row?."
  pred/last-row?)

(def not-last-row?
  "Compatibility alias for clj-string-layout.predicates/not-last-row?."
  pred/not-last-row?)

(def interior-row?
  "Compatibility alias for clj-string-layout.predicates/interior-row?."
  pred/interior-row?)

(def not-interior-row?
  "Compatibility alias for clj-string-layout.predicates/not-interior-row?."
  pred/not-interior-row?)

(def all-rows?
  "Compatibility alias for clj-string-layout.predicates/all-rows?."
  pred/all-rows?)

(def first-col?
  "Compatibility alias for clj-string-layout.predicates/first-col?."
  pred/first-col?)

(def not-first-col?
  "Compatibility alias for clj-string-layout.predicates/not-first-col?."
  pred/not-first-col?)

(def second-col?
  "Compatibility alias for clj-string-layout.predicates/second-col?."
  pred/second-col?)

(def last-col?
  "Compatibility alias for clj-string-layout.predicates/last-col?."
  pred/last-col?)

(def not-last-col?
  "Compatibility alias for clj-string-layout.predicates/not-last-col?."
  pred/not-last-col?)

(def interior-col?
  "Compatibility alias for clj-string-layout.predicates/interior-col?."
  pred/interior-col?)

(def not-interior-col?
  "Compatibility alias for clj-string-layout.predicates/not-interior-col?."
  pred/not-interior-col?)

(def all-cols?
  "Compatibility alias for clj-string-layout.predicates/all-cols?."
  pred/all-cols?)

(def layout-ascii-box-left
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-left."
  presets/layout-ascii-box-left)

(def layout-ascii-box-center
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-center."
  presets/layout-ascii-box-center)

(def layout-ascii-box-right
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-right."
  presets/layout-ascii-box-right)

(def layout-ascii-box-fill-left
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-fill-left."
  presets/layout-ascii-box-fill-left)

(def layout-ascii-box-fill-center
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-fill-center."
  presets/layout-ascii-box-fill-center)

(def layout-ascii-box-fill-right
  "Compatibility alias for clj-string-layout.presets/layout-ascii-box-fill-right."
  presets/layout-ascii-box-fill-right)

(def layout-norton-commander-left
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-left."
  presets/layout-norton-commander-left)

(def layout-norton-commander-center
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-center."
  presets/layout-norton-commander-center)

(def layout-norton-commander-right
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-right."
  presets/layout-norton-commander-right)

(def layout-norton-commander-fill-left
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-fill-left."
  presets/layout-norton-commander-fill-left)

(def layout-norton-commander-fill-center
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-fill-center."
  presets/layout-norton-commander-fill-center)

(def layout-norton-commander-fill-right
  "Compatibility alias for clj-string-layout.presets/layout-norton-commander-fill-right."
  presets/layout-norton-commander-fill-right)

(def layout-markdown-left
  "Compatibility alias for clj-string-layout.presets/layout-markdown-left."
  presets/layout-markdown-left)

(def layout-markdown-center
  "Compatibility alias for clj-string-layout.presets/layout-markdown-center."
  presets/layout-markdown-center)

(def layout-markdown-right
  "Compatibility alias for clj-string-layout.presets/layout-markdown-right."
  presets/layout-markdown-right)

(def layout-markdown-fill-left
  "Compatibility alias for clj-string-layout.presets/layout-markdown-fill-left."
  presets/layout-markdown-fill-left)

(def layout-markdown-fill-center
  "Compatibility alias for clj-string-layout.presets/layout-markdown-fill-center."
  presets/layout-markdown-fill-center)

(def layout-markdown-fill-right
  "Compatibility alias for clj-string-layout.presets/layout-markdown-fill-right."
  presets/layout-markdown-fill-right)

(def layout-html-table
  "Compatibility alias for clj-string-layout.presets/layout-html-table."
  presets/layout-html-table)

(def layout-html-table-readable
  "Compatibility alias for clj-string-layout.presets/layout-html-table-readable."
  presets/layout-html-table-readable)
