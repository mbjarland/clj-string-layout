(ns
  ^{:doc "Public entry points for rendering rows of strings with the clj-string-layout DSL.

  Most applications use layout for vector output, layout-str for newline joined
  output, or layout-seq for large inputs with explicit column widths. The
  parse-layout and explain-layout helpers are intended for developing and
  debugging custom layout strings."
    :author "Matias Bjarland"}
  clj-string-layout.core
  (:require [clj-string-layout.impl.config :as config]
            [clj-string-layout.impl.parser :as parser]
            [clj-string-layout.impl.render :as render]
            [clojure.string :as str]))

(def default-layout-config
  "Default configuration merged into every layout config.

  Important keys include :width, :align-char, :fill-char, :display-width,
  :col-widths, :row-count, :word-split-char, :row-split-char, and :raw?. The
  :layout key is intentionally not defaulted and must be supplied by callers.

  :align-char defaults to space. :fill-char defaults to whatever :align-char
  resolves to (so by default :fill-char is also space). Pass :fill-char
  explicitly when fill regions should use a different glyph than padded cells."
  config/default-layout-config)

(defn layout
  "Renders rows of strings into a vector of layout lines.

  rows may be a string, split with :row-split-char and :word-split-char, or a
  sequence of row sequences containing strings. Short rows are padded with empty
  cells to the widest input row unless explicit :col-widths are supplied.

  layout-config must include [:layout :cols], a vector whose first item is a
  layout string. Optional [:layout :rows] entries insert virtual rows such as
  table borders. See clj-string-layout.predicates for repeat and row predicate
  helpers.

  Returns a vector of strings by default. With :raw? true, returns a vector of
  row-piece vectors instead, which is useful for post-processing individual
  cells before joining."
  [rows layout-config]
  (let [layout-config (config/compile-layout-config layout-config)
        rows (config/normalize-rows layout-config rows)]
    (render/render-layout layout-config rows)))

(defn layout-seq
  "Renders rows of strings as a lazy sequence of layout lines.

  The rendering rules are the same as layout. For large data sets, pass
  :col-widths so rows can be rendered without first retaining the whole input to
  compute automatic widths. If virtual row layouts are present, pass :row-count
  so last-row? style predicates can be evaluated without counting the input.

  Without :col-widths, exact automatic widths still require realizing all rows
  before the first output row can be produced. With :raw? true, returns lazy
  row-piece vectors instead of joined strings."
  [rows layout-config]
  (let [layout-config (config/compile-layout-config layout-config)
        rows (config/normalize-row-seq layout-config rows)]
    (render/render-layout-seq layout-config rows)))

(defn layout-str
  "Renders rows and joins the resulting lines with newline characters.

  This is a convenience wrapper around layout for consumers that expect a
  single string rather than a vector of lines."
  [rows layout-config]
  (str/join \newline (layout rows layout-config)))

(defn layout-into!
  "Writes rendered layout lines to a java.io.Writer, one line per write.

  Each line is followed by a single newline. Honors layout-config the same
  way as layout-seq, so :col-widths and :row-count let large lazy inputs
  stream to the writer without realizing the whole output. Returns the
  writer."
  [^java.io.Writer writer rows layout-config]
  (doseq [line (layout-seq rows layout-config)]
    (let [s (if (string? line) ^String line (apply str line))]
      (.write writer ^String s)
      (.write writer "\n"))
    nil)
  writer)

(defn parse-layout
  "Parses a column layout string into the diagnostic layout representation.

  The returned data is useful for debugging custom layouts and error reports,
  but it is not a stable rendering API. Prefer layout, layout-str, or layout-seq
  for application code. Use parse-row-layout for row-layout strings, where the
  character inside brackets is the row drawing character rather than a column
  alignment marker.

  The 2-arity form (row-layout? layout-string) is kept for backwards
  compatibility; new code should call parse-layout or parse-row-layout
  explicitly."
  ([layout-string]
   (parser/parse-layout-string false layout-string))
  ([row-layout? layout-string]
   (parser/parse-layout-string row-layout? layout-string)))

(defn parse-row-layout
  "Parses a row layout string into the diagnostic layout representation.

  Row-layout markers like [-] or [=] use the bracket character as the row
  drawing glyph rather than a cell alignment. See parse-layout for the column
  variant and the same caveats about API stability."
  [layout-string]
  (parser/parse-layout-string true layout-string))

(defn- explain* [row-layout? layout-string]
  (try
    {:valid? true
     :layout (parser/parse-layout-string row-layout? layout-string)}
    (catch clojure.lang.ExceptionInfo e
      {:valid? false
       :message (ex-message e)
       :data (ex-data e)})))

(defn explain-layout
  "Returns parse diagnostics for a column layout string without throwing.

  Returns {:valid? true :layout ...} when parsing succeeds, otherwise
  {:valid? false :message ... :data ...}. The :data value is the ex-data map
  that parse-layout would have thrown. Use explain-row-layout for row layouts.

  The 2-arity form (row-layout? layout-string) is kept for backwards
  compatibility."
  ([layout-string]
   (explain* false layout-string))
  ([row-layout? layout-string]
   (explain* row-layout? layout-string)))

(defn explain-row-layout
  "Returns parse diagnostics for a row layout string without throwing.

  Behaves like explain-layout but parses with row-layout semantics."
  [layout-string]
  (explain* true layout-string))
