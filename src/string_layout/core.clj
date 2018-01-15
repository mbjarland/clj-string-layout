(ns string-layout.core
  (:require [string-layout.layout :refer :all]
            [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]
            [com.rpl.specter :refer [select pred= pred ALL FIRST NONE transform
                                     multi-transform multi-path setval must subselect
                                     select-one MAP-VALS LAST]]
            [taoensso.tufte :refer [p profiled profile]]))

(def default-layout-config
  {
   :align-char      \space
   :fill-char       \space
   :word-split-char \space
   :row-split-char  \newline
   :width           80
   :raw?            false})

(def grammar
  "layout = delim? ((col | repeat) delim)*
   repeat = <'{'> delim? (col delim?)* <'}'>
   delim    = (fill | #'[^\\[\\]{}fF]*')+
   fill     = <'F'> (#'[\\d]+')?
   col      = <'['> fill? align fill? <']'>")
;align    = ('L' | 'C' | 'R' | 'V') ")

(defn make-col-layout-parser []
  (insta/parser (str grammar \newline "align = ('L'|'C'|'R'|'V')")
                :string-ci true))
(def make-col-layout-parser-m (memoize make-col-layout-parser))

(defn make-row-layout-parser []
  (insta/parser (str grammar \newline "align = #'[^]]'")
                :string-ci true))
(def make-row-layout-parser-m (memoize make-row-layout-parser))



;; => [:layout [:del "| "] [:col [:fill "2"] [:align "C"] [:fill "2"]] [:del " |"]]
;; => {:layout [{:del ["| " {:f 2}]} {:col [{:f 2} \C {:f 2}]} {:del " |"}]
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
                   (-> a .toLowerCase keyword))})}
    parsed-layout))

(defn throw-parse-error [parsed s]
  (let [msg (with-out-str (fail/pprint-failure parsed))]
    (throw (ex-info (str "error parsing layout string '" s "':\n" msg)
                    {:failure (insta/get-failure parsed)}))))

(defn parse-layout-string [row-layout? layout-string]
  (let [parser        (if row-layout? (make-row-layout-parser-m)
                                      (make-col-layout-parser-m))
        parsed-layout (parser layout-string)]
    (if (insta/failure? parsed-layout)
      (throw-parse-error parsed-layout layout-string)
      (transform-parsed row-layout? parsed-layout))))



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
  :f values with (in this case ['***', '****'] or vice versa)
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
;(count (select [ALL :del ALL (pred= :f)] data))
; most performant way
;(transduce (map (constantly 1)) + (traverse [ALL :del ALL (pred= :f)] data))
;(transform [ALL :del ALL (pred= :f)]

; TODO: check out specter (must :a) instead of (pred :del) :del
(defn expand-fills
  "expands the :f formatting specifiers in the layout vector
  to the appropriate number of align-char characters"
  [width col-widths fill-chars layout]
  (let [del-f-path [ALL (must :del) ALL (pred= :f)]
        col-f-path [ALL (must :col) ALL (pred= :f)]
        del-fs     (select del-f-path layout)
        col-fs     (select col-f-path layout)
        ss         (select [ALL (must :del) ALL string?] layout)]
    (if (empty? del-fs)
      layout
      (let [fill-count  (+ (count del-fs) (count col-fs))
            tot-col-len (reduce + col-widths)
            tot-str-len (reduce + (map count ss))
            fill-width  (max 0 (- width (+ tot-col-len tot-str-len)))
            fills       (calculate-fills fill-width fill-count fill-chars)]
        (->> layout
             (setval [(subselect ALL MAP-VALS ALL (pred= :f))] fills)
             (transform [ALL (must :del)] join)
             (transform [ALL (must :col) ALL string?] count))))))


(comment
  [{:del [:f "║"]}
   {:del [" "]}
   {:col [:f {:align :c}]}
   {:del [" │"]}
   {:del [" "]}
   {:col [:f {:align :c}]}
   {:del [" │"]}
   {:del [" "]}
   {:col [:f {:align :c}]}
   {:del [" │"]}
   {:del [" "]}
   {:col [:f {:align :c}]}
   {:del [" │"]}
   {:del [" " :f]}
   {:col [:f {:align :c}]}
   {:del [" ║"]}]


  [{:del "║"}
   {:del " "}
   {:col [3 {:align :c} 3]}
   {:del " │"}
   {:del " "}
   {:col [4 {:align :c} 3]}
   {:del " │"}
   {:del " "}
   {:col [4 {:align :c} 3]}
   {:del " │"}
   {:del " "}
   {:col [4 {:align :c} 3]}
   {:del " │"}
   {:del "     "}
   {:col [3 {:align :c} 4]}
   {:del " ║"}]

  ;=>

  [{:del "║ "}
   {:col [3 {:align :c} 3]}
   {:del " │ "}
   {:col [4 {:align :c} 3]}
   {:del " │ "}
   {:col [4 {:align :c} 3]}
   {:del " │ "}
   {:col [4 {:align :c} 3]}
   {:del " │     "}
   {:col [3 {:align :c} 4]}
   {:del " ║"}]
  )

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
  (let [layout-config (merge default-layout-config layout-config)
        {:keys [align-char]} layout-config]
    (transform [:fill-char] (fnil identity align-char) layout-config)))

(defn row-fill-chars [row-layout fill-char fill-chars align-char]
  (let [fill-count (count (select [ALL :del ALL (pred= :f)] row-layout))]
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

(comment
  [{:del "╔═"}
   {:col [{:align \═}]}
   {:del "═╤═"}
   {:col [{:align \═}]}
   {:del "═╤═"}
   {:col [{:align \═}]}
   {:del "═╤═"}
   {:col [{:align \═}]}
   {:del "═╤═"}
   {:col [{:align \═}]}
   {:del "═╗"}]
  )



(defn f-realize-row-layout [col-widths]
  (fn [row-layout]
    (let [align-chars (select [ALL (must :col) FIRST (must :align)] row-layout)
          cols        (map #(join (repeat %1 %2)) col-widths align-chars)]
      (->> row-layout
           (setval [(subselect ALL (must :col))] cols)
           (select [ALL MAP-VALS])))))

(comment
  (defn f-realize-row-layout [col-widths]
    (fn [row-layout]
      (first
        (reduce
          (fn [[a i] e]
            (if (:del e)
              [(conj a (join (:del e))) i]
              [(conj a (join (repeat (nth col-widths i) (:align e)))) (inc i)]))
          [[] 0]
          row-layout))))
  )

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

;
; "║{ [C] │} [C] ║"
; ==>
;  [{:del ["║"]}
;  {:repeat [{:del [" "]}
;            {:col [{:align :c}]}
;            {:del [" │"]}]}
;  {:del [" "]}
;  {:col [{:align :c}]}
;  {:del [" ║"]}]
(defn f-expand-repeating-groups [col-count]
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


(defn mappify
  "transform a vector layout ['layout string' :apply-for ...]
   to a map format {:layout parsed :apply-for. Works for both
   row and col layouts"
  [layout rest]
  (merge {:layout layout} (apply hash-map rest)))


(comment
  (setval [:layout
           (subselect
             ALL
             #(contains? % :repeat)
             :apply-for)
           ]
          af)
  )

; TODO: do nothing if there are no repeating groups
; TODO: validate same number of repeating groups as predicates
(defn spread-layout-apply-fors
  "takes a col layout spec {:layout [{:repeat x}...] :apply-for [a ...]}}
   and turns it into       {:layout [{:repeat x :apply-for a} ...] }}"
  [preds]
  (fn [layout-spec]
    (setval [:layout (subselect ALL (pred :repeat) :apply-for)] preds layout-spec)))

(defn get-col-preds [raw-layout-config]
  (:apply-for (apply hash-map (rest (get-in raw-layout-config [:layout :cols])))))

;{:layout {:cols  ["║{ [C] │} [C] ║" :apply-for [all-cols? last-col?]]
(defn parse-layout-config
  "transform the layout config, replace the layout strings and the
  surrounding vectors with parsed and mappified data structures"
  [layout-config]
  (let [parse-fn  (fn [r? [ls & r]] (mappify (parse-layout-string r? ls) r))
        col-preds (get-col-preds layout-config)
        spread-f  (spread-layout-apply-fors col-preds)]
    (->> layout-config
         (transform [:layout (must :cols)] (partial parse-fn false))
         (transform [:layout (must :cols)] spread-f)
         (transform [:layout (must :rows) ALL] (partial parse-fn true))
         (transform [:layout (must :rows) ALL] spread-f)
         (setval [:layout (must :cols) :apply-for] NONE))))

(defn merge-adjacent-dels [layout]
  (reduce
    (fn [a c]
      (let [f (fn [m] (first (keys m)))]
        (if (= :del (f (last a)) (f c))
          (transform [LAST MAP-VALS] #(join (concat % (:del c))) a)
          (conj a c))))
    []
    layout))

;; things to validate in the layout config
;; * all repeating groups need to have at least one column def
;; * the col layout and all row layouts need to have:
;;   * the same number of repeating groups
;;   * the same number of columns
;;   tbd
(defn transform-layout-config [layout-config col-widths]
  (let [layout-config (parse-layout-config layout-config)
        fill-chars    (repeat (:fill-char layout-config))
        expand-f      (f-expand-repeating-groups (count col-widths))
        c-fill-f      (partial expand-fills (:width layout-config) col-widths fill-chars)
        r-fill-f      (partial expand-row-spec-fills layout-config col-widths)
        realize-f     (f-realize-row-layout col-widths)
        prn-f         (fn [layout]
                        (prn :transform-mid layout)
                        layout)]
    (->> layout-config
         (transform [:layout (must :cols) :layout] expand-f)
         (transform [:layout (must :rows) ALL :layout] expand-f)
         (transform [:layout (must :cols) :layout] c-fill-f)
         (transform [:layout (must :rows) ALL] r-fill-f)
         (transform [:layout (must :cols) :layout] merge-adjacent-dels)
         (transform [:layout (must :rows) ALL :layout] merge-adjacent-dels)
         (prn-f)
         (transform [:layout (must :rows) ALL :layout] realize-f))))

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
              (:del l) [(conj a (join (:del l))) ci]))
          [[] 0]
          col-layout)))))

(comment
  (defn fake-it [rows layout-config]
    (let [layout-config (merge-default-layout layout-config)
          rows          (normalize-rows layout-config rows)
          col-widths    (calculate-col-widths rows)
          layout-config (transform-layout-config layout-config col-widths)]
      layout-config))

  )


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
        ]
    layout-config))

;    layout-cols   (make-col-layout-fn layout-config col-widths)
;    result        (mapv layout-cols rows)
;    result        (if (get-in layout-config [:layout :rows])
;                    (apply-row-layouts layout-config result)
;                    result)]
;(if (:raw? layout-config) result (mapv join result))))


(comment
  ;TODO: move col widths to layout-config
  ;TODO: potentially add to col widths when filling col layout
  (layout
    (str "Alice, why is" \newline
         "a raven like" \newline
         "a writing desk?")
    ascii-box-layout-center)


  ;;----------------------------------------------
  (parse-layout-string false "║{ [C] │} [fCf]f║")
  ;;=>
  [{:del ["║"]}
   {:repeat [{:del [" "]} {:col [{:align :c}]} {:del [" │"]}]}
   {:del [" "]}
   {:col [:f {:align :c} :f]}
   {:del [:f "║"]}]

  )