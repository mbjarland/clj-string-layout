(ns
  ^{:doc "A library for laying out string data in table-like formats"
    :author "Matias Bjarland"}
  clj-string-layout.core
  (:require [clj-string-layout.impl.config :as config]
            [clj-string-layout.impl.parser :as parser]
            [clj-string-layout.impl.render :as render]
            [clojure.string :as str]))

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

(defn layout-str
  "Lays out rows and joins the resulting lines with newlines."
  [rows layout-config]
  (str/join \newline (layout rows layout-config)))

(defn parse-layout
  "Parses a layout string and returns the internal diagnostic representation.

  Column layout parsing is used by default. Pass true as the first argument to
  parse a row layout. The returned representation is intended for debugging
  custom layouts, not as a stable rendering API."
  ([layout-string]
   (parse-layout false layout-string))
  ([row-layout? layout-string]
   (parser/parse-layout-string row-layout? layout-string)))

(defn explain-layout
  "Returns parse diagnostics for a layout string without throwing.

  The result is {:valid? true :layout ...} when parsing succeeds, otherwise
  {:valid? false :message ... :data ...}."
  ([layout-string]
   (explain-layout false layout-string))
  ([row-layout? layout-string]
   (try
     {:valid? true
      :layout (parse-layout row-layout? layout-string)}
     (catch clojure.lang.ExceptionInfo e
       {:valid? false
        :message (ex-message e)
        :data (ex-data e)}))))
