(ns
  ^{:doc    "A library for laying out string data in table-like formats"
    :author "Matias Bjarland"}
  clj-string-layout.core
  (:require [clojure.pprint :as pprint]
            [clojure.string :refer [split join]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]))

(def
  ^{:doc "default layout config. Merged with argument one."}
  default-layout-config
  {:align-char      \space
   :fill-char       \space
   :word-split-char \space
   :row-split-char  \newline
   :width           80
   :raw?            false})

(def
  ^{:doc "base grammar for the layout string language"}
  grammar
  "layout = delim? ((col | repeat) delim?)*
   repeat = <'{'> delim? (col delim?)* <'}'>
   delim    = (fill | #'[^\\[\\]{}fF]+')+
   fill     = <'F'> (#'[\\d]+')?
   col      = <'['> fill? align fill? <']'>")

(def col-grammar (str grammar \newline "align = ('L'|'C'|'R'|'V')"))
(def row-grammar (str grammar \newline "align = #'[^]]'"))

(defn make-col-layout-parser []
  (insta/parser col-grammar :string-ci true))
(def make-col-layout-parser-m (memoize make-col-layout-parser))

(defn make-row-layout-parser []
  (insta/parser row-grammar :string-ci true))
(def make-row-layout-parser-m (memoize make-row-layout-parser))

(defn transform-parsed
  "transforms the parse tree returned from instaparse
  to a vector of maps better suited for working with layouts"
  [row-layout? parsed-layout]
  (insta/transform
    {:layout  vector
     :fill    (fn [] :f)
     :padding identity
     :repeat  (fn [& r] {:repeat (into [] r)})
     :delim   (fn [& r] {:del (into [] r)})
     :col     (fn [& r] {:col (into [] r)})
     :align   (fn [a]
                {:align
                  (if row-layout?
                    (first a)
                    (-> ^String a .toLowerCase keyword))})}
    parsed-layout))

(defn throw-parse-error [parsed s]
  (let [msg (with-out-str (fail/pprint-failure parsed))]
    (throw (ex-info (str "error parsing layout string '" s "':\n" msg)
                     {:failure (insta/get-failure parsed)}))))

(defn layout-values
  "Returns values from layout entries under k matching pred. If k is nil,
  all vector values in each layout entry are considered."
  ([layout k pred]
   (into []
         (comp (mapcat (fn [entry]
                         (if k
                           [(get entry k)]
                           (vals entry))))
               (filter vector?)
               cat
               (filter pred))
         layout)))

(defn replace-fill-tokens [layout fills]
  (first
    (reduce
      (fn [[out fills] entry]
        (let [[entry fills]
              (reduce-kv
                (fn [[entry fills] k v]
                  (if-not (vector? v)
                    [(assoc entry k v) fills]
                    (let [[v fills]
                          (reduce
                            (fn [[items fills] item]
                              (if (= :f item)
                                [(conj items (first fills)) (rest fills)]
                                [(conj items item) fills]))
                            [[] fills]
                            v)]
                      [(assoc entry k v) fills])))
                [{} fills]
                entry)]
          [(conj out entry) fills]))
      [[] fills]
      layout)))

(defn join-delims [layout]
  (mapv #(if (contains? % :del) (update % :del join) %) layout))

(defn count-col-fill-widths [layout]
  (mapv #(if (contains? % :col)
           (update % :col (fn [items]
                            (mapv (fn [item]
                                    (if (string? item) (count item) item))
                                  items)))
           %)
        layout))

(defn col-layouts [layout]
  (into [] (keep :col) layout))

(defn mappify
  "transform a vector layout ['layout string' :apply-for ...]
   to a map format {:layout parsed :apply-for. Works for both
   row and col layouts"
  [layout rest]
  (merge {:layout layout} (apply hash-map rest)))

(defn f-parse-layout-string
  "given a flag indicating whether to parse row or col
  layouts, return a function which takes a vector
  [layout-string rest] returns a hash-map
  {:layout parsed other-keys-based-on-rest}"
  [row-layout?]
  (let [parser (if row-layout? (make-row-layout-parser-m)
                               (make-col-layout-parser-m))]
    (fn [[layout-string & rest]]
      (let [parsed-layout (parser layout-string)]
        (if (insta/failure? parsed-layout)
          (throw-parse-error parsed-layout layout-string)
          (mappify (transform-parsed row-layout? parsed-layout)
                   rest))))))

; 10 6 -- 5 3 -> rest 2 -> 2/3
;   ["*"  "**" "**" "*"  "**" "**"]
;   ["*"  "*"  "*"  "*"  "*"  "*"]
;   [ 1    2    3    4    5    6]
;   [ 2/3  4/3  6/3  8/3  10/3 12/3]
; - [ 0    1    2    2    3    4] ;(int above)
;   [ 0    1    1    0    1    1] ;when changes
(defn calculate-fills
  "given a fill width (say 7), a fill count (say 2) and
  fill chars (say [* -]), returns strings suitable for
  replacing :f values with (in this case ['***', '----'] or vice versa)
  Note that there is an ambiguity here, we could as well have
  returned ['****', '---']"
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

(defn expand-fills-with-fs
  "expands the :f formatting specifiers in the layout vector
  to the appropriate number of align-char characters"
  [width col-widths fill-chars layout del-fs col-fs]
  (let [ss          (layout-values layout :del string?)
        fill-count  (+ (count del-fs) (count col-fs))
        tot-col-len (reduce + col-widths)
        tot-str-len (reduce + (map count ss))
        fill-width  (max 0 (- width (+ tot-col-len tot-str-len)))
        fills       (calculate-fills fill-width fill-count fill-chars)]
    (replace-fill-tokens layout fills)))

(defn f-expand-fills
  "expands the :f formatting specifiers in the layout vector
  to the appropriate number of align-char characters"
  [width col-widths fill-chars]
  (fn [layout]
    (let [del-fs (layout-values layout :del #{:f})
          col-fs (layout-values layout :col #{:f})]
      (->> (if (and (empty? del-fs) (empty? col-fs))
             layout
             (expand-fills-with-fs width col-widths fill-chars
                                    layout del-fs col-fs))
           join-delims
           count-col-fill-widths))))

(defn normalize-row-lens
  "Add empty elements to any rows which have fewer elements
  than col-count"
  [col-count rows]
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
  (let [v (nth (col-layouts layout) col)       ;[9 :l 8]
        w (+ (reduce + (keep #(when (int? %) %) v))
              (nth col-widths col))
        a (some #(when (keyword? %) %) v)]
    (letfn [(fmt [f] (pprint/cl-format nil f align-char w word))]
      (case a
        :l (fmt "~v,,,vA")
        :r (fmt "~v,,,v@A")
        :c (fmt "~v,,,v:@<~A~>")
        :v word                                             ;verbatim
        ;:W (fmt (str "~{~<~%~1," width ":;~A~> ~}"))
        (throw (IllegalArgumentException.
                 (str "Unsupported alignment operation '" a
                      "' encountered at align index: " col)))))))

(defn merge-default-layout [layout-config]
  (let [layout-config (merge default-layout-config layout-config)
        {:keys [align-char]} layout-config]
    (update layout-config :fill-char (fnil identity align-char))))

(defn row-fill-chars [row-layout fill-char fill-chars align-char]
  (let [fill-count (count (layout-values row-layout nil #{:f}))]
    (cond
      fill-chars fill-chars
      fill-char (repeat fill-count fill-char)
      align-char (repeat fill-count align-char))))

(defn f-expand-row-fills [layout-config col-widths]
  (let [{:keys [width fill-char align-char]} layout-config]
    (fn [row-spec]
      (let [{:keys [layout fill-char fill-chars] :or {fill-char fill-char}} row-spec
            fill-chars (row-fill-chars layout fill-char fill-chars align-char)
            tf         (f-expand-fills width col-widths fill-chars)]
        (update row-spec :layout tf)))))

(defn f-realize-rows [col-widths]
  (fn [row-spec]
    (update row-spec
            :layout
            (fn [row-layout]
              (first
                (reduce
                  (fn [[a ci] e]
                    (cond
                      (:del e) [(conj a (join (:del e))) ci]
                      (:col e) (let [g          (group-by int? (:col e))
                                     [widths aligns] [(get g true) (get g false)]
                                     width      (+ (reduce + widths) (nth col-widths ci))
                                     align-char (:align (first aligns))]
                                 [(conj a (join (repeat width align-char))) (inc ci)])
                      :else (throw (RuntimeException. (str "invalid layout element" e "encountered in" row-layout)))))
                  [[] 0]
                  row-layout))))))

(defn apply-row-layouts [layout-config rows]
  (if (get-in layout-config [:layout :rows])
    (let [row-specs (get-in layout-config [:layout :rows])
          cnt       (max 1 (count rows))]
      (reduce
        (fn [a idx]
          (let [matching (fn [{:keys [layout apply-for]}]
                           (when (apply-for [idx cnt]) layout))
                new-a    (into [] (concat a (keep matching row-specs)))]
            (if (= idx cnt) new-a
                            (conj new-a (nth rows idx)))))
        []
        (range (inc cnt))))
    rows))

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

(defn f-expand-repeats [col-count]
  (fn [layout]
    (if (not-any? :repeat layout)
      layout
      (let [[lhs repeats rhs] (partition-layout layout)     ;TODO: zippers for partition?
            count-cols   (fn [c] (count (keep :col c)))
            repeat-range (range (count-cols lhs)
                                (- col-count (count-cols rhs)))
            last-col     (dec col-count)
            f            (fn [a i]
                           (let [loc             [i last-col]
                                 matching-groups (keep (fn [g]
                                                         (when ((:apply-for g) loc)
                                                           (:repeat g))) repeats)
                                 addition        (reduce concat matching-groups)
                                 add-count       (count-cols addition)]
                             (if (zero? add-count)
                               (throw-no-matching-pred-exception i last-col)
                               (concat a addition))))]
        (into [] (concat (reduce f lhs repeat-range) rhs))))))

(defn get-col-preds [raw-layout-config]
  (let [cols (get-in raw-layout-config [:layout :cols])]
    (:apply-for (apply hash-map (rest cols)))))

; TODO: do nothing if there are no repeating groups
; TODO: validate same number of repeating groups as predicates
(defn f-spread-spec-apply-fors
  "takes a col layout spec {:layout [{:repeat x}...] :apply-for [a ...]}}
   and turns it into       {:layout [{:repeat x :apply-for a} ...] }}"
  [layout-config]
  (let [preds (get-col-preds layout-config)
        preds (if (sequential? preds) preds [preds])]
    (fn [spec]
      (first
        (reduce
          (fn [[spec preds] entry]
            (if (contains? entry :repeat)
              [(update spec :layout conj (assoc entry :apply-for (first preds)))
               (next preds)]
              [(update spec :layout conj entry) preds]))
          [(assoc spec :layout []) preds]
          (:layout spec))))))

;{:layout {:cols  ["║{ [C] │} [C] ║" :apply-for [all-cols? last-col?]]
(defn parse-layout-config
  "transform the layout config, replace the layout strings and the
  surrounding vectors with parsed and mappified data structures"
  [layout-config]
  (let [parse-and-spread-f
        (fn [row?]
          (fn [layout]
            (let [p (f-parse-layout-string row?)
                  s (f-spread-spec-apply-fors layout-config)]
              (->> layout p s))))]
    (->> layout-config
         (#(update-in % [:layout :cols] (parse-and-spread-f false)))
         (#(if (get-in % [:layout :rows])
             (update-in % [:layout :rows]
                        (fn [rows]
                          (mapv (parse-and-spread-f true) rows)))
             %))
         (#(update-in % [:layout :cols] dissoc :apply-for)))))

(defn merge-adjacent-dels [layout]
  (reduce
    (fn [a c]
      (let [c (if (and (:del c) (vector? (:del c)))
                (update c :del join)
                c)]
        (if (and (:del (last a)) (:del c))
          (conj (pop a) (update (last a) :del str (:del c)))
          (conj a c))))
    []
    layout))

(defn flatten-aligns [layout]
  (mapv #(if (contains? % :col)
           (update % :col (fn [items]
                            (mapv (fn [item]
                                    (if (and (map? item) (contains? item :align))
                                      (:align item)
                                      item))
                                  items)))
           %)
        layout))

;; things to validate in the layout config
;; * all repeating groups need to have at least one column def
;; * the col layout and all row layouts need to have:
;;   * the same number of repeating groups
;;   * the same number of columns
;;   tbd
(defn transform-layout-config [layout-config col-widths]
  (let [layout-config (parse-layout-config layout-config)
        {:keys [fill-char width]} layout-config
        repeats-f     (f-expand-repeats (count col-widths))
        c-fills-f     (f-expand-fills width col-widths (repeat fill-char))
        r-fills-f     (f-expand-row-fills layout-config col-widths)
        realize-f     (f-realize-rows col-widths)
        f             (fn [g] (fn [spec] (update spec :layout g)))
        col-f         (comp flatten-aligns merge-adjacent-dels c-fills-f repeats-f)
        row-f         (comp realize-f (f merge-adjacent-dels) r-fills-f (f repeats-f))]
    (cond-> (update-in layout-config [:layout :cols :layout] col-f)
      (get-in layout-config [:layout :rows])
      (update-in [:layout :rows] #(mapv row-f %)))))

(def transform-layout-config-m (memoize transform-layout-config))

(defn f-layout-cols [layout-config col-widths]
  (let [align-char (:align-char layout-config)
        col-layout (get-in layout-config [:layout :cols :layout])
        align      (partial align-word col-layout col-widths align-char)]
    (fn [row]
      (first
        (reduce
          (fn [[a ci] l]
            (cond
              (:col l) [(conj a (align (nth row ci) ci)) (inc ci)]
              (:del l) [(conj a (:del l)) ci]))
          [[] 0]
          col-layout)))))

; TODO: read up on mig layout, change precondition
; TODO: fix parsing failure handling
(defn layout
  "Lays out rows of text in columns. The first argument can either
  be a string with spaces between 'words' and newlines between each
  row _or_ a vector of vector of strings representing the rows
  and the words (columns) within the rows. The second argument is 
  a map representing a 'layout config', please see the docs for 
  details on layout configs. raw? (default false) will, if true
  in the layout config return rows as vectors of indented columns 
  and column spacings instead of returning rows as already joined 
  strings. The raw format can be useful if you need to do some post 
  processing like adding ansi colors to certain columns before 
  outputting to terminal etc."
  [rows layout-config]
  {:pre [(pos? (count rows))]}                              ;TODO: replace predicate with spec
  (let [layout-config (merge-default-layout layout-config)
        rows          (normalize-rows layout-config rows)
        col-widths    (calculate-col-widths rows)
        layout-config (transform-layout-config-m layout-config col-widths)
        c-layout-f    (f-layout-cols layout-config col-widths)
        result        (mapv c-layout-f rows)
        result        (apply-row-layouts layout-config result)]
    (if (:raw? layout-config) result (mapv join result))))
