(ns clj-string-layout.table
  "High-level table API built on top of the layout DSL.

  Use this namespace when you want named output formats, headers, map rows, and
  column specs without writing layout strings directly. Drop down to
  clj-string-layout.core/layout when you need full DSL control."
  (:require [clj-string-layout.core :as core]
            [clj-string-layout.escape :as escape]
            [clj-string-layout.predicates :as pred]
            [clj-string-layout.presets :as presets]
            [clojure.string :as str]))

(def ^:private format-defaults
  {:plain {:escape identity :layout :generated :default-align :left}
   :markdown {:escape escape/markdown-cell :layout :generated :default-align :left}
   :markdown-left {:escape escape/markdown-cell :layout :generated :default-align :left}
   :markdown-center {:escape escape/markdown-cell :layout :generated :default-align :center}
   :markdown-right {:escape escape/markdown-cell :layout :generated :default-align :right}
   :box {:escape identity :layout :generated :default-align :left}
   :double-box {:escape identity :layout :generated :default-align :left}
   :unicode-box {:escape identity :layout :generated :default-align :left}
   :unicode-double-box {:escape identity :layout :generated :default-align :left}
   :ascii-box {:escape identity :layout :generated :default-align :left}
   :ascii-double-box {:escape identity :layout :generated :default-align :left}
   :ascii-grid {:escape identity :layout :generated :default-align :left}
   :csv {:escape escape/csv-cell :layout presets/layout-csv :default-align :verbatim}
   :tsv {:escape escape/tsv-cell :layout presets/layout-tsv :default-align :verbatim}
   :pipe {:escape identity :layout presets/layout-pipe-separated :default-align :verbatim}
   :psql {:escape identity :layout presets/layout-psql-left :default-align :left}
   :org {:escape escape/org-cell :layout presets/layout-org-left :default-align :left}
   :rst {:escape escape/rst-cell :layout presets/layout-rst-simple :default-align :left}
   :html {:escape escape/html :layout :html :default-align :verbatim}})

(defn formats
  "Returns the set of named formats accepted by table, table-str, and table-seq."
  []
  (set (keys format-defaults)))

(defn format-info
  "Returns registry information for a named table format.

  The returned map is descriptive and may contain implementation details such as
  :layout. It is useful for discovery, but callers should prefer table APIs over
  relying on this map's exact shape."
  [format]
  (get format-defaults format))

(defn- ensure-format [format]
  (or (format-info format)
      (throw (ex-info "Unknown table format"
                      {:type :unknown-table-format
                       :format format
                       :available-formats (formats)}))))

(defn- default-format [{:keys [format]}]
  (or format :plain))

(defn- align-token [align]
  (case align
    (:left :l "left" "l") "L"
    (:center :centre :c "center" "centre" "c") "C"
    (:right :r "right" "r") "R"
    (:verbatim :v "verbatim" "v") "V"
    "L"))

(defn- normalize-column [idx column]
  (cond
    (map? column) (assoc column :idx idx)
    (keyword? column) {:idx idx :key column :title (name column)}
    :else {:idx idx :key idx :title (str column)}))

(defn- infer-columns [{:keys [columns headers rows]}]
  (cond
    (seq columns) (mapv normalize-column (range) columns)
    (seq headers) (mapv (fn [idx header]
                          {:idx idx :key idx :title (str header)})
                        (range)
                        headers)
    (map? (first rows)) (mapv (fn [idx k]
                                {:idx idx :key k :title (name k)})
                              (range)
                              (keys (first rows)))
    :else (mapv (fn [idx]
                  {:idx idx :key idx :title (str idx)})
                (range (count (first rows))))))

(defn- column-title [headers column]
  (if headers
    (str (nth headers (:idx column) (:title column)))
    (str (:title column))))

(defn- row-value [row {:keys [idx key]}]
  (let [value (if (map? row)
                (get row key)
                (nth row (if (integer? key) key idx) nil))]
    (if (nil? value) "" value)))

(defn- apply-format [value {:keys [format]}]
  (if format
    (format value)
    value))

(defn- clip [value width]
  (subs value 0 (min width (count value))))

(defn- ellipsis [value width]
  (cond
    (<= (count value) width) value
    (<= width 0) ""
    (<= width 3) (subs "..." 0 width)
    :else (str (subs value 0 (- width 3)) "...")))

(defn- wrap [value width]
  (if (or (not width) (<= (count value) width))
    [value]
    (mapv (partial apply str) (partition-all width value))))

(defn- overflow-cell [value {:keys [width overflow] :or {overflow :none}}]
  (if (and width (> (count value) width))
    (case overflow
      :none value
      :clip (clip value width)
      :ellipsis (ellipsis value width)
      :wrap (wrap value width)
      :error (throw (ex-info "Table cell exceeds configured column width"
                             {:type :table-cell-overflow
                              :value value
                              :width width
                              :overflow overflow}))
      value)
    value))

(defn- scalar-cell [value]
  (if (vector? value) (first value) value))

(defn- expand-wrapped-row [cells]
  (let [cell-lines (mapv #(if (vector? %) % [%]) cells)
        line-count (apply max 1 (map count cell-lines))]
    (mapv (fn [line-idx]
            (mapv #(get % line-idx "") cell-lines))
          (range line-count))))

(defn- prepare-row [row columns escape-fn escape?]
  (let [cells (mapv (fn [column]
                       (let [value (-> (row-value row column)
                                       (apply-format column)
                                       str
                                       (overflow-cell column))]
                         (if (vector? value)
                           (mapv #(if escape? (escape-fn %) %) value)
                           (let [value (scalar-cell value)]
                             (if escape? (escape-fn value) value)))))
                     columns)]
    (expand-wrapped-row cells)))

(defn- table-rows [{:keys [headers rows escape?] :as spec} columns escape-fn]
  (let [escape? (not (false? escape?))
        header-row (when (or headers (seq (:columns spec)))
                     (mapv #(column-title headers %) columns))
        header-rows (when header-row
                      (expand-wrapped-row
                        (mapv (fn [value column]
                                (let [value (overflow-cell value column)]
                                  (if escape? (escape-fn value) value)))
                              header-row
                              columns)))
        data-rows (mapcat #(prepare-row % columns escape-fn escape?) rows)]
    (vec (concat header-rows data-rows))))

(defn- alignments [columns default-align]
  (mapv #(or (:align %) default-align) columns))

(defn- generated-cols [aligns separator]
  (str/join separator (map #(str "[" (align-token %) "]") aligns)))

(defn- markdown-rule-cell [align]
  (case align
    (:center :centre :c "center" "centre" "c") ":[-]:"
    (:right :r "right" "r") " [-]:"
    ":[-] "))

(defn- generated-rule [left sep right fill columns]
  (let [marker (str "[" fill "]")]
    (str left fill (str/join (str fill sep fill) (repeat (count columns) marker)) fill right)))

(defn- generated-layout [format columns default-align]
  (let [aligns (alignments columns default-align)]
    (case format
      :plain {:layout {:cols [(generated-cols aligns "  ")]}}
      (:markdown :markdown-left :markdown-center :markdown-right)
      {:layout {:cols [(str "| " (generated-cols aligns " | ") " |")]
                :rows [[(str "|" (str/join "|" (map markdown-rule-cell aligns)) "|")
                        :apply-for pred/second-row?]]}}

      (:box :unicode-box :ascii-box)
      {:layout {:cols [(str "│ " (generated-cols aligns " │ ") " │")]
                :rows [[(generated-rule "┌" "┬" "┐" "─" columns)
                        :apply-for pred/first-row?]
                       [(generated-rule "├" "┼" "┤" "─" columns)
                        :apply-for pred/interior-row?]
                       [(generated-rule "└" "┴" "┘" "─" columns)
                        :apply-for pred/last-row?]]}}

      (:double-box :unicode-double-box :ascii-double-box)
      {:layout {:cols [(str "║ " (generated-cols aligns " ║ ") " ║")]
                :rows [[(generated-rule "╔" "╦" "╗" "═" columns)
                        :apply-for pred/first-row?]
                       [(generated-rule "╠" "╬" "╣" "═" columns)
                        :apply-for pred/interior-row?]
                       [(generated-rule "╚" "╩" "╝" "═" columns)
                        :apply-for pred/last-row?]]}}

      :ascii-grid {:layout {:cols [(str "| " (generated-cols aligns " | ") " |")]
                            :rows [[(generated-rule "+" "+" "+" "-" columns)
                                    :apply-for pred/all-rows?]]}}
      nil)))

(defn- html-row [tag row]
  (str "  <tr>" (apply str (map #(str "<" tag ">" % "</" tag ">") row)) "</tr>"))

(defn- render-html [rows header?]
  (let [[header rows] (if header? [(first rows) (rest rows)] [nil rows])]
    (vec (concat ["<table>"]
                 (when header [(html-row "th" header)])
                 (map #(html-row "td" %) rows)
                 ["</table>"]))))

(defn- table-plan [{:keys [rows] :as spec}]
  (let [format (default-format spec)
        {:keys [escape layout default-align]} (ensure-format format)
        columns (infer-columns spec)
        rows (or rows [])
        rows (table-rows (assoc spec :rows rows) columns escape)
        header? (boolean (or (:headers spec) (seq (:columns spec))))
        layout-config (if (= :generated layout)
                        (generated-layout format columns default-align)
                        layout)]
    {:format format
     :rows rows
     :header? header?
     :layout-config layout-config}))

(defn table
  "Renders a high-level table spec to a vector of output lines.

  Required input is usually :rows, with optional :headers, :columns, and
  :format. Supported formats are returned by formats. Column specs may contain
  :key, :title, :align, :format, :width, and :overflow. Overflow policies are
  :none, :clip, :ellipsis, :wrap, and :error."
  [spec]
  (let [{:keys [format rows header? layout-config]} (table-plan spec)]
    (if (= :html format)
      (render-html rows header?)
      (core/layout rows (merge layout-config (select-keys spec [:width :display-width :raw?]))))))

(defn table-str
  "Renders a high-level table spec and joins the resulting lines with newlines."
  [spec]
  (str/join \newline (table spec)))

(defn table-seq
  "Renders a high-level table spec as a sequence of output lines.

  This currently applies the high-level table normalization before returning the
  sequence. Use clj-string-layout.core/layout-seq directly when you need fully
  streaming behavior with already-normalized rows and explicit column widths."
  [spec]
  (seq (table spec)))
