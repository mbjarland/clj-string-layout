(ns string-layout.core
  (:require [string-layout.layout :refer :all]
            [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]
            [com.rpl.specter :refer [select pred= pred ALL FIRST transform
                                     multi-transform multi-path]]
            [taoensso.tufte :refer [p profiled profile]]))


(def default-layout-config
  {
   :align-char      \space
   :fill-char       \space
   :word-split-char \space
   :row-split-char  \newline
   :width           80
   :raw?            false})

(defn make-col-layout-parser []
  (insta/parser
    "layout = delim? ((align | repeat) delim)*
     repeat = <'{'> delim? (align delim?)* <'}'>
     delim    = (fill | padding)+
     fill     = 'F'
     padding  = #'[^\\[\\]{}fF]*'
     align    = <'['> ('L' | 'C' | 'R' | 'V') <']'>"
    :string-ci true))
(def make-col-layout-parser-m (memoize make-col-layout-parser))

(defn make-row-layout-parser []
  (insta/parser
    "layout = delim? ((align | repeat) delim)*
     repeat = <'{'> delim? (align delim?)* <'}'>
     fill     = ('F')
     padding  = #'[^\\[\\]{}fF]*'
     align    = <'['> #'[^]]' <']'>"
    :string-ci true))
(def make-row-layout-parser-m (memoize make-row-layout-parser))

(defn transform-parsed [p row-layout?]
  "transforms the parse tree returned from instaparse
  to a vector of maps better suited for working with layouts"
  (insta/transform
    {:layout  vector
     :fill    (fn [_] :F)
     :padding identity
     :repeat  (fn [& r] {:repeat (into [] r)})
     :delim   (fn [& r] {:delim (into [] r)})
     :align   (fn [a]
                {:align
                 (if row-layout?
                   (first a)
                   (-> a .toUpperCase keyword))})}
    p))

(defn throw-parse-error [parsed s]
  (let [msg (with-out-str (fail/pprint-failure parsed))]
    (throw (ex-info (str "error parsing layout string '" s "':\n" msg)
                    {:failure (insta/get-failure parsed)}))))

(defn parse-layout-string [row-layout? layout-string]
  (let [parser (if row-layout? (make-row-layout-parser)
                               (make-col-layout-parser))
        parsed (parser layout-string)]
    (if (insta/failure? parsed)
      (throw-parse-error parsed layout-string)
      (transform-parsed parsed row-layout?))))

; 10 6 -- 5 3 -> rest 2 -> 2/3
;   ["*"  "**" "**" "*"  "**" "**"]
;   ["*"  "*"  "*"  "*"  "*"  "*"]
;   [ 1    2    3    4    5    6]
;   [ 2/3  4/3  6/3  8/3  10/3 12/3]
; - [ 0    1    2    2    3    4] ;(int above)
;   [ 0    1    1    0    1    1] ;when changes
(defn calculate-fills
  "given a fill width (say 7), a fill count (say 2) and an
  align char (say *), returns strings suitable for replacing
  :F values with (in this case ['***', '****'] or vice versa)
  Note that there is an ambiguity here, we could as well have
  returned ['****', '***']"
  [fill-width fill-count fill-chars]
  (let [[q sr] ((juxt quot rem) fill-width fill-count)
        r  (/ sr fill-count)
        s  (map int (iterate #(+ r %) r))                   ; 0 1 2 2 3 4
        ns (map #(join (repeat q %)) fill-chars)]
    (first (reduce
             (fn [[a l i] x]
               (let [n (nth ns i)]
                 [(conj a (if (= l x) n (str n (nth fill-chars i)))) x (inc i)]))
             [[] 0 0]
             (take fill-count s)))))

; best way (natahan marz)
;(count (select [ALL :delim ALL (pred= :F)] data))
; most performant way
;(transduce (map (constantly 1)) + (traverse [ALL :delim ALL (pred= :F)] data))
;(transform [ALL :delim ALL (pred= :F)]
(defn expand-fills
  "expands the :F formatting specifiers in the layout vector
  to the appropriate number of align-char characters"
  [width col-widths fill-chars layout]
  (prn "===>" layout)
  (let [f-path [ALL :delim ALL (pred= :F)]
        fs     (select f-path layout)
        ss     (select [ALL :delim ALL string?] layout)]
    (if (empty? fs)
      layout
      (let [fill-count  (count fs)
            sum-cols    (reduce + col-widths)
            sum-strings (reduce + (map count ss))
            fill-width  (max 0 (- width (+ sum-cols sum-strings)))
            fills       (calculate-fills fill-width fill-count fill-chars)
            fill-idx    (atom -1)]
        (transform f-path
                   (fn [_] (nth fills (swap! fill-idx inc)))
                   layout)))))


(defn normalize-row-lens [col-count rows]
  "Add empty elements to any rows which have fewer elements
  than col-count"
  (mapv #(into [] (take col-count (concat % (repeat ""))))
        rows))

(defn normalize-rows
  "add empty elements as padding to rows with too few elements. If the
  rows argument is a string, also split it using split-char as delimiter"
  [layout-config rows]
  (let [wp       (re-pattern (str \\ (:word-split-char layout-config)))
        rp       (re-pattern (str \\ (:row-split-char layout-config)))
        r        (if (instance? String rows) (mapv #(split % wp)
                                                   (split rows rp))
                                             rows)
        max-cols (apply max (map count r))]
    (normalize-row-lens max-cols r)))

(defn calculate-col-widths [rows]
  (apply mapv #(apply max (map count %&)) rows))

(defn align-word
  "align a word based on the given col-layout, col-widths and
  align-char. Word is a string word to layout and col is the column
  this word should be laid out for"
  [layout col-widths align-char word col]
  (let [aligns (keep :align layout)]
    (letfn [(fmt [f] (cl-format nil f align-char (nth col-widths col) word))]
      (case (nth aligns col)
        :L (fmt "~v,,,vA")
        :R (fmt "~v,,,v@A")
        :C (fmt "~v,,,v:@<~A~>")
        :V word                                             ;verbatim
        ;:W (fmt (str "~{~<~%~1," width ":;~A~> ~}"))
        :else (throw (IllegalArgumentException.
                       (str "Unsupported alignment operation '" (nth aligns col)
                            "' encountered at align index: " col " in " aligns)))))))

(defn merge-default-layout [layout-config]
  (let [{:keys [align-char]} layout-config
        layout (transform [:fill-char] (fnil identity align-char) layout-config)]
    (merge default-layout-config layout)))

(defn row-fill-chars [row-layout fill-char fill-chars align-char]
  (let [fill-count (count (select [ALL :delim ALL (pred= :F)] row-layout))]
    (cond
      fill-chars fill-chars
      fill-char (repeat fill-count fill-char)
      align-char (repeat fill-count align-char))))

(defn expand-row-spec-fills [layout-config col-widths row-spec]
  (let [{:keys [width fill-char align-char]} layout-config
        {:keys [layout fill-char fill-chars] :or {fill-char fill-char}} row-spec
        fill-chars (row-fill-chars layout fill-char fill-chars align-char)]
    (transform [:layout]
               #(expand-fills width col-widths fill-chars %)
               row-spec)))

(defn realize-row-layout [col-widths row-layout]
  (first
    (reduce
      (fn [[a i] e]
        (if (:delim e)
          [(conj a (join (:delim e))) i]
          [(conj a (join (repeat (nth col-widths i) (:align e)))) (inc i)]))
      [[] 0]
      row-layout)))

; data size 2 (..n)
; ↓               ↓               ↓       ↓
; ┌───────────────┬───────────────┬───────┐ ← 0
; │ Tables        │ Are           │ Cool  │
; ├───────────────┼───────────────┼───────┤ ← 1
; │ col 3 is      │ right-aligned │ $1600 │
; └───────────────┴───────────────┴───────┘ ← 2 (n)
(defn apply-row-layouts [layout-config rows]
  (let [row-specs (select [:layout :rows ALL] layout-config)
        cnt       (max 1 (count rows))]
    (reduce
      (fn [a idx]
        (let [matching (fn [{:keys [layout apply-for]}]
                         (when (apply-for [idx cnt]) layout))
              new-a    (into [] (concat a (keep matching row-specs)))]
          (if (= idx cnt) new-a
                          (conj new-a (nth rows idx)))))
      []
      (range (inc cnt)))))


(defn throw-invalid-grouping-exception []
  (throw (RuntimeException. "invalid grouping xxxx TODO: expound...")))

(defn partition-layout [layout]
  (let [parts (partition-by #(contains? % :repeat) layout)
        part  (fn [n] (into [] (nth parts n)))
        types (map (comp #(contains? % :repeat) first) parts)]
    (condp = types
      [true] [[] (part 0) []]
      [true false] [[] (part 0) (part 1)]
      [false true] [(part 0) (part 1) []]
      [false true false] parts
      (throw-invalid-grouping-exception))))

(defn throw-no-matching-pred-exception [idx last]
  (throw (RuntimeException. (str "no matching predicate found for index "
                                 idx " (last index = " last ")"))))

(defn expand-repeating-groups [col-count preds layout]
  (if (not-any? :repeat layout)
    layout
    (let [[lhs repeats rhs] (partition-layout layout)
          count-aligns (fn [c] (count (keep :align c)))
          repeat-range (range (count-aligns lhs)
                              (- col-count (count-aligns rhs)))
          last-col     (dec col-count)
          f            (fn [a i]
                         (let [matching-idxs (keep-indexed
                                               (fn [pi p] (when (p [i last-col]) pi))
                                               preds)
                               matching-reps (map #(:repeat (nth repeats %)) matching-idxs)
                               addition      (reduce concat matching-reps)
                               add-count     (count-aligns addition)]
                           (if (zero? add-count)
                             (throw-no-matching-pred-exception i last-col)
                             (concat a addition))))]
      (into [] (concat (reduce f lhs repeat-range) rhs)))))

(defn transform-layout-config [layout-config col-widths]
  (let [mappify       (fn [layout rest] (merge {:layout layout} (apply hash-map rest)))
        parse-fn      (fn [r? [ls & r]] (mappify (parse-layout-string r? ls) r))
        layout-config (->> layout-config
                           (transform [:layout (pred :cols) :cols] (partial parse-fn false))
                           (transform [:layout (pred :rows) :rows ALL] (partial parse-fn true)))
        _             (prn layout-config)
        fill-chars    (repeat (:fill-char layout-config))
        col-preds     (get-in layout-config [:layout :cols :apply-for])
        col-fill-fn   (partial expand-fills (:width layout-config) col-widths fill-chars)
        row-fill-fn   (partial expand-row-spec-fills layout-config col-widths)
        groups-fn     (partial expand-repeating-groups (count col-widths) col-preds)
        realize-fn    (partial realize-row-layout col-widths)]
    (->> layout-config
         (transform [:layout (pred :cols) :cols :layout] groups-fn)
         (transform [:layout (pred :rows) :rows ALL :layout] groups-fn)
         ((fn [lc]
            (prn "--> " lc)
            lc))
         (transform [:layout (pred :cols) :cols :layout] col-fill-fn)
         (transform [:layout (pred :rows) :rows ALL] row-fill-fn)
         (transform [:layout (pred :rows) :rows ALL :layout] realize-fn))))

(def transform-layout-config-m (memoize transform-layout-config))

(defn make-col-layout-fn [layout-config col-widths]
  (let [align-char (:align-char layout-config)
        col-layout (get-in layout-config [:layout :cols :layout])
        align      (partial align-word col-layout col-widths align-char)]
    (fn [row]
      (first
        (reduce
          (fn [[a ci] l]
            (cond
              (:align l) [(conj a (align (nth row ci) ci)) (inc ci)]
              (:delim l) [(conj a (join (:delim l))) ci]))
          [[] 0]
          col-layout)))))

;(fn [row] (str (join (interleave (map join delims)
;                                 (map-indexed #(align %2 %1) row)))
;               (join (last delims))))))

; TODO: read up on mig layout, change precondition
; TODO: fix parsing failure handling
(defn layout
  "Lays out rows of text in columns. The first argument can either
  be a string with spaces between 'words' and newlines between each
  row _or_ a vector of vector of strings representing the rows
  and the words (columns) within the rows. The second
  argument is a layout string on the form ' [L]f[R] [R] ' and
  the third argument is a layout config on the form
  {:width 80 :align-char \\space :raw? false} where width is the
  total width of the row (any 'f' specifiers between columns will
  fill the row out to this width), the align-char is the character to
  use when filling to width, and raw? (default false) will, if true,
  return rows as vectors of indented columns and column spacings instead of
  returning rows as already joined strings. The raw format can be useful
  if you need to do some post processing like adding ansi colors to
  certain columns before outputting to terminal etc."
  [rows layout-config]
  {:pre [(pos? (count rows))]}                              ;TODO: replace predicate with spec
  (let [layout-config (merge-default-layout layout-config)
        rows          (normalize-rows layout-config rows)
        col-widths    (calculate-col-widths rows)
        layout-config (transform-layout-config layout-config col-widths)
        layout-cols   (make-col-layout-fn layout-config col-widths)
        result        (mapv layout-cols rows)
        result        (if (get-in layout-config [:layout :rows])
                        (apply-row-layouts layout-config result)
                        result)]
    (if (:raw? layout-config) result (mapv join result))))


(comment
  (layout
    (str "Alice, why is" \newline
         "a raven like" \newline
         "a writing desk?")
    ascii-box-layout-center)

  )