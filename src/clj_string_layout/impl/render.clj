(ns clj-string-layout.impl.render
  (:require [clj-string-layout.impl.error :refer [layout-error]]
            [clojure.string :as str]))

(defn- text-entry? [entry]
  (= :text (:type entry)))

(defn- column-entry? [entry]
  (= :column (:type entry)))

(defn- repeat-entry? [entry]
  (= :repeat (:type entry)))

(defn- repeat-string [n ch]
  (if (pos? n)
    (.repeat (str ch) n)
    ""))

(defn- display-width [layout-config value]
  (let [width ((:display-width layout-config) value)]
    (when-not (and (integer? width) (not (neg? width)))
      (layout-error "Layout :display-width must return a non-negative integer"
                    {:type :invalid-layout-config
                     :path [:display-width]
                     :value value
                     :width width}))
    width))

(defn- count-columns [layout]
  (count (filter column-entry? layout)))

(defn- fill-slot-count [layout]
  (reduce (fn [total entry]
            (+ total
               (case (:type entry)
                 :fill 1
                 :column (:fills entry)
                 0)))
          0
          layout))

(defn- calculate-fills
  "Distributes fill-width across fill-count slots using fill-chars."
  [fill-width fill-count fill-chars]
  (if (zero? fill-count)
    []
    (let [[base-width remainder] ((juxt quot rem) fill-width fill-count)
          fill-width-at (fn [idx]
                          (+ base-width
                             (- (quot (* (inc idx) remainder) fill-count)
                                (quot (* idx remainder) fill-count))))]
      (mapv (fn [idx fill-char]
              (repeat-string (fill-width-at idx) fill-char))
            (range fill-count)
            fill-chars))))

(defn- layout-static-width [layout-config layout]
  (transduce (keep #(when (text-entry? %)
                     (display-width layout-config (:value %))))
             +
             layout))

(defn- take-fill-strings [fills n entry]
  (when (< (count fills) n)
    (layout-error "Not enough fill strings available while expanding layout"
                  {:type :invalid-layout-state
                   :entry entry
                   :needed n
                   :remaining (count fills)}))
  [(take n fills) (drop n fills)])

(defn- replace-fill-slots [layout layout-config fill-strings]
  (first
    (reduce
      (fn [[out fill-strings] entry]
        (case (:type entry)
          :fill
          (let [[[fill-string] fill-strings] (take-fill-strings fill-strings 1 entry)]
            [(conj out {:type :text :value fill-string}) fill-strings])

          :column
          (let [[column-fills fill-strings] (take-fill-strings fill-strings (:fills entry) entry)]
            [(conj out (assoc entry :fill-widths (mapv #(display-width layout-config %) column-fills)))
             fill-strings])

          [(conj out entry) fill-strings]))
      [[] fill-strings]
      layout)))

(defn merge-adjacent-text [layout]
  (reduce
    (fn [out entry]
      (if (and (text-entry? (peek out)) (text-entry? entry))
        (conj (pop out) (update (peek out) :value str (:value entry)))
        (conj out entry)))
    []
    layout))

(defn- expand-fills [layout-config width col-widths fill-chars layout]
  (let [fill-count (fill-slot-count layout)]
    (if (zero? fill-count)
      (merge-adjacent-text layout)
      (let [fill-chars (take fill-count fill-chars)]
        (when-not (= fill-count (count fill-chars))
          (layout-error "Fill character count must match fill marker count"
                        {:type :invalid-layout-config
                         :fill-count fill-count
                         :fill-chars fill-chars}))
         (let [fill-width (max 0 (- width
                                    (+ (reduce + col-widths)
                                       (layout-static-width layout-config layout))))]
           (-> layout
               (replace-fill-slots layout-config (calculate-fills fill-width fill-count fill-chars))
               merge-adjacent-text))))))

(defn- invalid-grouping! [layout]
  (layout-error "Repeat groups must form one contiguous repeat section"
                {:type :invalid-layout-config
                 :layout layout}))

(defn- partition-layout [layout]
  (let [parts (partition-by repeat-entry? layout)
        part (fn [n] (into [] (nth parts n)))
        types (map (comp repeat-entry? first) parts)]
    (case (vec types)
      [true] [[] (part 0) []]
      [true false] [[] (part 0) (part 1)]
      [false true] [(part 0) (part 1) []]
      [false true false] [(part 0) (part 1) (part 2)]
      (invalid-grouping! layout))))

(defn- no-matching-repeat! [idx last-idx]
  (layout-error "No repeat group matched column index"
                {:type :invalid-layout-config
                 :idx idx
                 :last-idx last-idx}))

(defn- expand-repeats [col-count layout]
  (if (not-any? repeat-entry? layout)
    layout
    (let [[lhs repeats rhs] (partition-layout layout)
          repeat-range (range (count-columns lhs)
                              (- col-count (count-columns rhs)))
          last-idx (dec col-count)
          expanded (into []
                         (concat
                           (reduce
                             (fn [out idx]
                               (let [loc [idx last-idx]
                                     matching-groups (filter #((:apply-for %) loc) repeats)
                                     additions (mapcat :layout matching-groups)]
                                 (if (zero? (count-columns additions))
                                   (no-matching-repeat! idx last-idx)
                                   (into out additions))))
                             lhs
                             repeat-range)
                           rhs))]
      (when-not (= col-count (count-columns expanded))
        (layout-error "Expanded layout column count does not match row data"
                      {:type :invalid-layout-config
                       :expected col-count
                       :actual (count-columns expanded)
                       :layout expanded}))
      expanded)))

(defn- calculate-row-widths [layout-config rows]
  (mapv (fn [row]
          (mapv #(display-width layout-config %) row))
        rows))

(defn- calculate-col-widths [row-widths]
  (apply mapv max row-widths))

(defn- configured-widths [layout-config rows]
  (if-some [col-widths (:col-widths layout-config)]
    {:col-widths (vec col-widths)}
    (let [row-widths (calculate-row-widths layout-config rows)]
      {:col-widths (calculate-col-widths row-widths)
       :row-widths row-widths})))

(defn- column-extra-width [column]
  (reduce + (:fill-widths column)))

(defn- pad-right [value value-width width ch]
  (str value (repeat-string (max 0 (- width value-width)) ch)))

(defn- pad-left [value value-width width ch]
  (str (repeat-string (max 0 (- width value-width)) ch) value))

(defn- pad-center [value value-width width ch]
  (let [padding (max 0 (- width value-width))
        left (quot (inc padding) 2)
        right (- padding left)]
    (str (repeat-string left ch) value (repeat-string right ch))))

(defn- align-word [column-width align-char word word-width column idx]
  (let [width (+ column-width (column-extra-width column))]
    (case (:align column)
      :l (pad-right word word-width width align-char)
      :r (pad-left word word-width width align-char)
      :c (pad-center word word-width width align-char)
      :v word
      (layout-error "Unsupported column alignment"
                    {:type :invalid-layout-config
                     :idx idx
                     :align (:align column)}))))

(defn- render-data-row [layout-config col-widths cell-widths row]
  (let [align-char (:align-char layout-config)
        col-layout (get-in layout-config [:layout :cols :layout])]
    (first
      (reduce
        (fn [[out col-idx] entry]
          (case (:type entry)
            :text [(conj out (:value entry)) col-idx]
            :column [(conj out (align-word (nth col-widths col-idx)
                                            align-char
                                            (nth row col-idx)
                                            (nth cell-widths col-idx)
                                            entry
                                            col-idx))
                     (inc col-idx)]
            (layout-error "Invalid data-row layout element"
                          {:type :invalid-layout-state
                           :entry entry})))
        [[] 0]
        col-layout))))

(defn- render-data-row* [layout-config col-widths row]
  (render-data-row layout-config
                   col-widths
                   (mapv #(display-width layout-config %) row)
                   row))

(defn- row-fill-chars [row-layout fill-char fill-chars align-char]
  (let [fill-count (fill-slot-count row-layout)]
    (cond
      fill-chars (do
                   (when-not (= fill-count (count fill-chars))
                     (layout-error "Row :fill-chars must match fill marker count"
                                   {:type :invalid-layout-config
                                    :fill-count fill-count
                                    :fill-chars fill-chars}))
                   fill-chars)
      fill-char (repeat fill-char)
      align-char (repeat align-char))))

(defn- expand-row-fills [layout-config col-widths row-spec]
  (let [{default-fill-char :fill-char align-char :align-char width :width} layout-config
        {:keys [layout fill-char fill-chars]
         :or {fill-char default-fill-char}} row-spec
        fill-chars (row-fill-chars layout fill-char fill-chars align-char)]
    (update row-spec :layout #(expand-fills layout-config width col-widths fill-chars %))))

(defn- render-row-layout [col-widths row-spec]
  (update row-spec
          :layout
          (fn [row-layout]
            (first
              (reduce
                (fn [[out col-idx] entry]
                  (case (:type entry)
                    :text [(conj out (:value entry)) col-idx]
                    :column (let [width (+ (nth col-widths col-idx)
                                           (column-extra-width entry))]
                              [(conj out (repeat-string width (:align entry)))
                               (inc col-idx)])
                    (layout-error "Invalid row layout element"
                                  {:type :invalid-layout-state
                                   :entry entry})))
                [[] 0]
                row-layout)))))

(defn- prepare-layout-config [layout-config col-widths]
  (let [col-count (count col-widths)
        width (:width layout-config)
        fill-char (:fill-char layout-config)
        prepare-col-layout #(->> %
                                 (expand-repeats col-count)
                                 (expand-fills layout-config width col-widths (repeat fill-char)))
        expand-row-spec (partial expand-row-fills layout-config col-widths)
        render-row-spec (partial render-row-layout col-widths)
        prepare-row-spec (fn [row-spec]
                           (-> row-spec
                               (update :layout #(expand-repeats col-count %))
                               expand-row-spec
                               render-row-spec))
        layout-config (update-in layout-config [:layout :cols :layout]
                                 prepare-col-layout)]
    (when-not (= col-count (count-columns (get-in layout-config [:layout :cols :layout])))
      (layout-error "Column layout count does not match row data"
                    {:type :invalid-layout-config
                     :expected col-count
                     :actual (count-columns (get-in layout-config [:layout :cols :layout]))}))
    (cond-> layout-config
      (get-in layout-config [:layout :rows])
      (update-in [:layout :rows] #(mapv prepare-row-spec %)))))

(defn- matching-row-layouts [row-specs idx cnt]
  (keep (fn [{:keys [layout apply-for]}]
          (when (apply-for [idx cnt]) layout))
        row-specs))

(defn- row-layout-count [layout-config rows]
  (max 1 (or (:row-count layout-config) (count rows))))

(defn- validate-row-count! [layout-config rows]
  (when-some [row-count (:row-count layout-config)]
    (when-not (= row-count (count rows))
      (layout-error "Layout :row-count must match the number of data rows"
                    {:type :invalid-rows
                     :row-count row-count
                     :actual (count rows)}))))

(defn- apply-row-layouts [layout-config rows]
  (if-let [row-specs (get-in layout-config [:layout :rows])]
    (let [_ (validate-row-count! layout-config rows)
          cnt (row-layout-count layout-config rows)]
      (reduce
        (fn [out idx]
          (let [matching-layouts (matching-row-layouts row-specs idx cnt)
                out (into out matching-layouts)]
            (if (= idx cnt)
              out
              (conj out (nth rows idx)))))
        []
        (range (inc cnt))))
    rows))

(defn- apply-row-layouts-seq [layout-config rows]
  (if-let [row-specs (get-in layout-config [:layout :rows])]
    (let [cnt (row-layout-count layout-config rows)]
      (letfn [(step [idx rows]
                (lazy-seq
                  (when (<= idx cnt)
                    (if (= idx cnt)
                      (do
                        (when (and (:row-count layout-config) (seq rows))
                          (layout-error "Rows continued after :row-count"
                                        {:type :invalid-rows
                                         :row-count (:row-count layout-config)
                                         :idx idx}))
                        (matching-row-layouts row-specs idx cnt))
                      (concat (matching-row-layouts row-specs idx cnt)
                              (if-let [rows (seq rows)]
                                (cons (first rows) (step (inc idx) (rest rows)))
                                (layout-error "Rows ended before :row-count"
                                              {:type :invalid-rows
                                               :row-count (:row-count layout-config)
                                               :idx idx})))))))]
        (step 0 rows)))
    rows))

(defn render-layout-seq [layout-config rows]
  (let [{:keys [col-widths row-widths]} (configured-widths layout-config rows)
        layout-config (prepare-layout-config layout-config col-widths)
        data-rows (if row-widths
                    (map #(render-data-row layout-config col-widths %1 %2)
                         row-widths
                         rows)
                    (map #(render-data-row* layout-config col-widths %) rows))
        rows (apply-row-layouts-seq layout-config data-rows)]
    (if (:raw? layout-config)
      rows
      (map str/join rows))))

(defn render-layout [layout-config rows]
  (let [{:keys [col-widths row-widths]} (configured-widths layout-config rows)
        layout-config (prepare-layout-config layout-config col-widths)
        data-rows (if row-widths
                    (mapv #(render-data-row layout-config col-widths %1 %2)
                          row-widths
                          rows)
                    (mapv #(render-data-row* layout-config col-widths %) rows))
        rows (apply-row-layouts layout-config data-rows)]
    (if (:raw? layout-config)
      rows
      (mapv str/join rows))))
