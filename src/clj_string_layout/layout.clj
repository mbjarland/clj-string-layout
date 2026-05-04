(ns clj-string-layout.layout
  "Compatibility namespace for the historical public API.

  Earlier versions exposed both predicates and built-in layout maps from this
  namespace. New code can require clj-string-layout.predicates and
  clj-string-layout.presets directly for clearer intent, while existing code can
  continue using these aliases."
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

(def layout-plain-left
  "Compatibility alias for clj-string-layout.presets/layout-plain-left."
  presets/layout-plain-left)

(def layout-plain-center
  "Compatibility alias for clj-string-layout.presets/layout-plain-center."
  presets/layout-plain-center)

(def layout-plain-right
  "Compatibility alias for clj-string-layout.presets/layout-plain-right."
  presets/layout-plain-right)

(def layout-tsv
  "Compatibility alias for clj-string-layout.presets/layout-tsv."
  presets/layout-tsv)

(def layout-csv
  "Compatibility alias for clj-string-layout.presets/layout-csv."
  presets/layout-csv)

(def layout-pipe-separated
  "Compatibility alias for clj-string-layout.presets/layout-pipe-separated."
  presets/layout-pipe-separated)

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

(def layout-ascii-grid-left
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-left."
  presets/layout-ascii-grid-left)

(def layout-ascii-grid-center
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-center."
  presets/layout-ascii-grid-center)

(def layout-ascii-grid-right
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-right."
  presets/layout-ascii-grid-right)

(def layout-ascii-grid-fill-left
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-fill-left."
  presets/layout-ascii-grid-fill-left)

(def layout-ascii-grid-fill-center
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-fill-center."
  presets/layout-ascii-grid-fill-center)

(def layout-ascii-grid-fill-right
  "Compatibility alias for clj-string-layout.presets/layout-ascii-grid-fill-right."
  presets/layout-ascii-grid-fill-right)

(def layout-psql-left
  "Compatibility alias for clj-string-layout.presets/layout-psql-left."
  presets/layout-psql-left)

(def layout-psql-right
  "Compatibility alias for clj-string-layout.presets/layout-psql-right."
  presets/layout-psql-right)

(def layout-rst-simple
  "Compatibility alias for clj-string-layout.presets/layout-rst-simple."
  presets/layout-rst-simple)

(def layout-org-left
  "Compatibility alias for clj-string-layout.presets/layout-org-left."
  presets/layout-org-left)

(def layout-org-right
  "Compatibility alias for clj-string-layout.presets/layout-org-right."
  presets/layout-org-right)

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
