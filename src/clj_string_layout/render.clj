(ns clj-string-layout.render
  (:require [clj-string-layout.error :refer [layout-error]]
            [clojure.string :as str]))

(defn- text-entry? [entry]
  (= :text (:type entry)))

(defn- column-entry? [entry]
  (= :column (:type entry)))

(defn- repeat-entry? [entry]
  (= :repeat (:type entry)))

(defn- repeat-string [n ch]
  (apply str (repeat n ch)))

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

(defn calculate-fills
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

(defn- layout-static-width [layout]
  (transduce (keep #(when (text-entry? %) (count (:value %)))) + layout))

(defn- take-fill-strings [fills n entry]
  (when (< (count fills) n)
    (layout-error "Not enough fill strings available while expanding layout"
                  {:type :invalid-layout-state
                   :entry entry
                   :needed n
                   :remaining (count fills)}))
  [(take n fills) (drop n fills)])

(defn- replace-fill-slots [layout fill-strings]
  (first
    (reduce
      (fn [[out fill-strings] entry]
        (case (:type entry)
          :fill
          (let [[[fill-string] fill-strings] (take-fill-strings fill-strings 1 entry)]
            [(conj out {:type :text :value fill-string}) fill-strings])

          :column
          (let [[column-fills fill-strings] (take-fill-strings fill-strings (:fills entry) entry)]
            [(conj out (assoc entry :fill-widths (mapv count column-fills)))
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

(defn expand-fills [width col-widths fill-chars layout]
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
                                      (layout-static-width layout))))]
          (-> layout
              (replace-fill-slots (calculate-fills fill-width fill-count fill-chars))
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

(defn expand-repeats [col-count layout]
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

(defn calculate-col-widths [rows]
  (apply mapv #(apply max (map count %&)) rows))

(defn- column-extra-width [column]
  (reduce + (:fill-widths column)))

(defn- pad-right [value width ch]
  (str value (repeat-string (max 0 (- width (count value))) ch)))

(defn- pad-left [value width ch]
  (str (repeat-string (max 0 (- width (count value))) ch) value))

(defn- pad-center [value width ch]
  (let [padding (max 0 (- width (count value)))
        left (quot (inc padding) 2)
        right (- padding left)]
    (str (repeat-string left ch) value (repeat-string right ch))))

(defn- align-word [column-width align-char word column idx]
  (let [width (+ column-width (column-extra-width column))]
    (case (:align column)
      :l (pad-right word width align-char)
      :r (pad-left word width align-char)
      :c (pad-center word width align-char)
      :v word
      (layout-error "Unsupported column alignment"
                    {:type :invalid-layout-config
                     :idx idx
                     :align (:align column)}))))

(defn- render-data-row [layout-config col-widths row]
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
                                           entry
                                           col-idx))
                     (inc col-idx)]
            (layout-error "Invalid data-row layout element"
                          {:type :invalid-layout-state
                           :entry entry})))
        [[] 0]
        col-layout))))

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
    (update row-spec :layout #(expand-fills width col-widths fill-chars %))))

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
                                 (expand-fills width col-widths (repeat fill-char)))
        prepare-row-spec #(-> %
                              (update :layout (partial expand-repeats col-count))
                              ((fn [row-spec]
                                 (expand-row-fills layout-config col-widths row-spec)))
                              ((fn [row-spec]
                                 (render-row-layout col-widths row-spec))))
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

(defn- apply-row-layouts [layout-config rows]
  (if-let [row-specs (get-in layout-config [:layout :rows])]
    (let [cnt (max 1 (count rows))]
      (reduce
        (fn [out idx]
          (let [matching-layouts (keep (fn [{:keys [layout apply-for]}]
                                         (when (apply-for [idx cnt]) layout))
                                       row-specs)
                out (into out matching-layouts)]
            (if (= idx cnt)
              out
              (conj out (nth rows idx)))))
        []
        (range (inc cnt))))
    rows))

(defn render-layout [layout-config rows]
  (let [col-widths (calculate-col-widths rows)
        layout-config (prepare-layout-config layout-config col-widths)
        data-rows (mapv #(render-data-row layout-config col-widths %) rows)
        rows (apply-row-layouts layout-config data-rows)]
    (if (:raw? layout-config)
      rows
      (mapv str/join rows))))
