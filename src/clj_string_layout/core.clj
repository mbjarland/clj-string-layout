(ns
  ^{:doc "A library for laying out string data in table-like formats"
    :author "Matias Bjarland"}
  clj-string-layout.core
  (:require [clj-string-layout.impl.config :as config]
            [clj-string-layout.impl.render :as render]))

(def default-layout-config
  "Default layout config. Merged with the layout config passed to layout."
  config/default-layout-config)

(defn layout
  "Lays out rows of text in columns.

  Rows can be a string, split by :row-split-char and :word-split-char, or a
  sequence of row sequences containing strings. The layout config describes
  column layout and optional virtual row layouts. When :raw? is true, rows are
  returned as vectors of pieces instead of joined strings."
  [rows layout-config]
  (let [layout-config (config/compile-layout-config layout-config)
        rows (config/normalize-rows layout-config rows)]
    (render/render-layout layout-config rows)))
