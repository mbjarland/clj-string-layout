(ns clj-string-layout.table
  "High-level table API built on top of the layout DSL.

  Use this namespace when you want named output formats, headers, map rows, and
  column specs without writing layout strings directly. Drop down to
  clj-string-layout.core/layout when you need full DSL control."
  (:require [clj-string-layout.core :as core]
            [clj-string-layout.escape :as escape]
            [clj-string-layout.impl.box :as box]
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

(def ^:private align-tokens
  {:left "L" :l "L" "left" "L" "l" "L"
   :center "C" :centre "C" :c "C" "center" "C" "centre" "C" "c" "C"
   :right "R" :r "R" "right" "R" "r" "R"
   :verbatim "V" :v "V" "verbatim" "V" "v" "V"})

(defn- align-token [align]
  (or (get align-tokens align)
      (throw (ex-info "Unknown table column alignment"
                      {:type :invalid-table-column
                       :align align
                       :allowed (sort-by str (keys align-tokens))}))))

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

(defn- decorate [cell-fn section row-idx column value]
  (if cell-fn
    (cell-fn {:section section
              :row row-idx
              :col (:idx column)
              :column column
              :value value})
    value))

(defn- prepare-row [row row-idx section columns escape-fn escape? cell-fn]
  (let [cells (mapv (fn [column]
                       (let [value (-> (row-value row column)
                                       (apply-format column)
                                       str
                                       (overflow-cell column))]
                         (if (vector? value)
                           (mapv #(decorate cell-fn section row-idx column
                                            (if escape? (escape-fn %) %))
                                 value)
                           (let [value (scalar-cell value)
                                 value (if escape? (escape-fn value) value)]
                             (decorate cell-fn section row-idx column value)))))
                     columns)]
    (expand-wrapped-row cells)))

(defn- table-rows [{:keys [headers rows footers escape? cell-fn] :as spec} columns escape-fn]
  (let [escape? (not (false? escape?))
        header-row (when (or headers (seq (:columns spec)))
                     (mapv #(column-title headers %) columns))
        header-rows (when header-row
                      (expand-wrapped-row
                        (mapv (fn [value column]
                                (let [value (overflow-cell value column)
                                      value (if escape? (escape-fn value) value)]
                                  (decorate cell-fn :header 0 column value)))
                              header-row
                              columns)))
        data-rows (mapcat (fn [row-idx row]
                            (prepare-row row row-idx :data columns escape-fn escape? cell-fn))
                          (range)
                          rows)
        footer-rows (mapcat (fn [row-idx row]
                              (prepare-row row row-idx :footer columns escape-fn escape? cell-fn))
                            (range)
                            footers)]
    {:rows (vec (concat header-rows data-rows footer-rows))
     :header-count (count header-rows)
     :footer-count (count footer-rows)}))

(defn- alignments [columns default-align]
  (mapv #(or (:align %) default-align) columns))

(defn- align-tokens-for [aligns]
  (mapv align-token aligns))

(defn- plain-cols [tokens]
  (str/join "  " (map #(str "[" % "]") tokens)))

(defn- markdown-rule-cell [align]
  (case (align-token align)
    "C" ":[-]:"
    "R" " [-]:"
    ":[-] "))

(defn- markdown-cols [tokens]
  (str "| " (str/join " | " (map #(str "[" % "]") tokens)) " |"))

(defn- markdown-rule [aligns]
  (str "|" (str/join "|" (map markdown-rule-cell aligns)) "|"))

(defn- bordered-format-layout [chars tokens n single-rule?]
  (let [cols (box/aligned-cols (:cols chars) tokens)
        rule (fn [edge] (box/aligned-rule (get chars edge) n))]
    {:layout {:cols [cols]
              :rows (if single-rule?
                      [[(rule :top) :apply-for pred/all-rows?]]
                      [[(rule :top) :apply-for pred/first-row?]
                       [(rule :middle) :apply-for pred/interior-row?]
                       [(rule :bottom) :apply-for pred/last-row?]])}}))

(def ^:private box-format-chars
  {:box box/box-chars
   :unicode-box box/box-chars
   :ascii-box box/box-chars
   :double-box box/double-box-chars
   :unicode-double-box box/double-box-chars
   :ascii-double-box box/double-box-chars
   :ascii-grid box/ascii-grid-chars})

(defn- generated-layout [format columns default-align header?]
  (let [aligns (alignments columns default-align)
        tokens (align-tokens-for aligns)
        n (count columns)]
    (case format
      :plain {:layout {:cols [(plain-cols tokens)]}}
      (:markdown :markdown-left :markdown-center :markdown-right)
      (cond-> {:layout {:cols [(markdown-cols tokens)]}}
        header?
        (assoc-in [:layout :rows]
                  [[(markdown-rule aligns) :apply-for pred/second-row?]]))

      (:box :unicode-box :ascii-box
       :double-box :unicode-double-box :ascii-double-box)
      (bordered-format-layout (get box-format-chars format) tokens n false)

      :ascii-grid
      (bordered-format-layout (get box-format-chars format) tokens n true)

      nil)))

(defn- html-row [tag row]
  (str "  <tr>" (apply str (map #(str "<" tag ">" % "</" tag ">") row)) "</tr>"))

(defn- html-caption [title escape? escape-fn]
  (str "  <caption>" (if escape? (escape-fn title) title) "</caption>"))

(defn- render-html [rows header-count footer-count raw? title escape? escape-fn]
  (let [[headers rest-rows] (split-at header-count rows)
        body-count (- (count rest-rows) footer-count)
        [body footers] (split-at body-count rest-rows)
        lines (vec (concat ["<table>"]
                           (when title [(html-caption title escape? escape-fn)])
                           (map #(html-row "th" %) headers)
                           (map #(html-row "td" %) body)
                           (map #(html-row "td" %) footers)
                           ["</table>"]))]
    (if raw? (mapv vector lines) lines)))

(defn- center-line [text width]
  (let [pad (max 0 (- width (count text)))
        left (quot pad 2)
        right (- pad left)]
    (str (apply str (repeat left \space)) text (apply str (repeat right \space)))))

(defn- prepend-title [lines title raw?]
  (if-not title
    lines
    (let [width (apply max 0 (map (if raw?
                                    #(reduce + (map count %))
                                    count)
                                  lines))
          banner (center-line title width)]
      (vec (cons (if raw? [banner] banner) lines)))))

(defn- table-plan [{:keys [rows] :as spec}]
  (let [format (default-format spec)
        {:keys [escape layout default-align]} (ensure-format format)
        columns (infer-columns spec)
        rows (or rows [])
        {:keys [rows header-count footer-count]} (table-rows (assoc spec :rows rows) columns escape)
        header? (boolean (or (:headers spec) (seq (:columns spec))))
        layout-config (if (= :generated layout)
                        (generated-layout format columns default-align header?)
                        layout)]
    {:format format
     :rows rows
     :header? header?
     :header-count header-count
     :footer-count footer-count
     :layout-config layout-config
     :escape escape}))

(defn table
  "Renders a high-level table spec to a vector of output lines.

  Required input is usually :rows, with optional :headers, :footers,
  :columns, :title, :cell-fn, and :format. Supported formats are returned
  by formats. Column specs may contain :key, :title, :align, :format,
  :width, and :overflow. Overflow policies are :none, :clip, :ellipsis,
  :wrap, and :error. :footers accepts the same row shapes as :rows.

  :cell-fn is an optional decoration callback that receives a context map
  with :section (one of :header, :data, :footer), :row (zero-based row
  index within the section), :col (column index), :column (the column
  spec), and :value (the post-format/escape cell value). It must return a
  string. Use it together with :display-width when adding ANSI styling so
  the layout engine pads with the original visible width.

  When :title is supplied it renders as a centered banner above the table for
  text formats and as a <caption> element for :html. The title is escaped with
  the same per-format escaper unless :escape? false is set on the spec.

  The :width and :display-width spec keys are forwarded to the layout engine
  for every format that emits visually padded text. They are intentionally
  ignored for :html output, where the result is structural markup rather than
  padded text. :raw? is honored for every format, including :html."
  [spec]
  (let [{:keys [format rows header-count footer-count layout-config escape]} (table-plan spec)
        raw? (boolean (:raw? spec))
        escape? (not (false? (:escape? spec)))
        title (:title spec)]
    (if (= :html format)
      (render-html rows header-count footer-count raw? title escape? escape)
      (-> (core/layout rows (merge layout-config
                                   (select-keys spec [:width :display-width :raw?])))
          (prepend-title title raw?)))))

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
