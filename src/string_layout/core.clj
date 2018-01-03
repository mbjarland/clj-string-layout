(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]
            [com.rpl.specter :refer [select pred= pred ALL FIRST transform
                                     multi-transform multi-path ]]))

(def default-layout
  {
   :align-char \space
   :fill-char  \space
   :split-char \space
   :width      80
   :raw?       false
   ;:col-layout "│ [L] │ [C] │ [R] │"
   ;:row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"] :apply-when first-row?
   ;             ["└─[─]─┴─[─]─┴─[─]─┘"] :apply-when last-row?]
   })


(defn first-row? [[idx _]] (zero? idx))
(def not-first-row? (complement first-row?))
(defn second-row? [[idx _]] (= idx 1))

(defn last-row? [[idx cnt]] (= idx cnt))
(defn interior-row? [[idx cnt]] (not (or (zero? idx) (= idx cnt))))

(defn all-rows? [[_ _]] true)

(defn make-col-layout-parser []
  (insta/parser
    "layout   = delim (align delim)+
     delim    = (fill | padding)*
     fill     = ('F' | 'f')
     padding  = #'[^\\[\\]fF]*'
     align    = <'['> ('L' | 'l' | 'C' | 'c' | 'R' | 'r') <']'>"))

(defn make-row-layout-parser []
  (insta/parser
    "layout   = delim (align delim)+
     delim    = (fill | padding)*
     fill     = ('F' | 'f')
     padding  = #'[^\\[\\]fF]*'
     align    = <'['> #'[^]]' <']'>"))

;;(def make-col-layout-parser (memoize make-col-layout-parser-internal))
;;(def make-row-layout-parser (memoize make-row-layout-parser-internal))

(defn transform-parsed [p row-layout?]
  "transforms the parse tree returned from instaparse
  to a vector of maps better suited for working with layouts"
  (insta/transform
    {:layout  vector
     :fill    (fn [_] :F)
     :padding identity
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
  [layout width col-widths fill-chars]
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
  [rows col-layout split-char]
  (let [align-count (count (keep :align col-layout))
        p           (re-pattern (str \\ split-char))
        r           (if (instance? String rows) (mapv #(split % p) (split rows #"\n"))
                                                rows)]
    (normalize-row-lens align-count r)))

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
        ;:W (fmt (str "~{~<~%~1," width ":;~A~> ~}"))
        :else (throw (IllegalArgumentException.
                       (str "Unsupported alignment operation '" (nth aligns col)
                            "' encountered at align index: " col " in " aligns)))))))


(defn merge-default-layout [layout]
  (let [{:keys [align-char]} layout
        layout (transform [:fill-char] (fnil identity align-char) layout)]
    (merge default-layout layout)))

(defn row-fill-chars [row-layout fill-char fill-chars align-char]
  (let [fill-count (count (select [ALL :delim ALL (pred= :F)] row-layout))]
    (cond
      fill-chars fill-chars
      fill-char (repeat fill-count fill-char)
      align-char (repeat fill-count align-char))))

(defn expand-row-spec-fills [layout-config col-widths row-spec]
  (let [{:keys [width fill-char align-char]} layout-config
        {:keys [row-layout fill-char fill-chars] :or {fill-char fill-char}} row-spec
        fill-chars (row-fill-chars row-layout fill-char fill-chars align-char)]
    (transform [:row-layout]
               #(expand-fills % width col-widths fill-chars)
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


;["┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first?]
;->
;{:row-layout "┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first?}
(defn row-spec->map [row-spec]
  (let [[row-layout & rest] row-spec]
    (merge {:row-layout row-layout} (apply hash-map rest))))

(defn realize-row-specs [layout-config col-widths]
  (let [parse   (partial parse-layout-string true)
        expand  (partial expand-row-spec-fills layout-config col-widths)
        realize (partial realize-row-layout col-widths)]
    (->> layout-config
         (transform [:layout :rows ALL] row-spec->map)
         (transform [:layout :rows ALL :row-layout] parse)
         (transform [:layout :rows ALL] expand)
         (transform [:layout :rows ALL :row-layout] realize))))

(comment
  (def my-layout-config
    {:width     20
     :fill-char \*
     :layout    {:cols "│ [L] │ [C] │ [R] │"
                 :rows [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first-row?]
                        ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when interior-row?]
                        ["└─[─]─┴─[─]─┴─[─]─┘" :apply-when last-row?]]}})
  (realize-row-specs my-layout-config [5 5 5])

  )

; data size 2 (..n)
; ↓               ↓               ↓       ↓
; ┌───────────────┬───────────────┬───────┐ ← 0
; │ Tables        │ Are           │ Cool  │
; ├───────────────┼───────────────┼───────┤ ← 1
; │ col 3 is      │ right-aligned │ $1600 │
; └───────────────┴───────────────┴───────┘ ← 2 (n)
(defn apply-row-layout [layout-config col-widths rows]
  (let [realized  (realize-row-specs layout-config col-widths)
        row-specs (select [:layout :rows ALL] realized)
        cnt       (max 1 (count rows))]
    (reduce
      (fn [a idx]
        (let [matching (fn [{:keys [row-layout apply-when] :as all}]
                         (when (apply-when [idx cnt]) row-layout))
              new-a    (into [] (concat a (keep matching row-specs)))]
          (if (= idx cnt) new-a
                          (conj new-a (nth rows idx)))))
      []
      (range (inc cnt)))))

(defn make-col-layout-fn [col-layout col-widths align-char]
  (let [delims (keep :delim col-layout)
        align  (partial align-word col-layout col-widths align-char)]
    (fn [row] (str (join (interleave (map join delims)
                                     (map-indexed #(align %2 %1) row)))
                   (join (last delims))))))

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
        {:keys [align-char fill-char split-char width raw?]} layout-config
        col-layout    (parse-layout-string false (get-in layout-config [:layout :cols]))
        rows          (normalize-rows rows col-layout split-char)
        col-widths    (calculate-col-widths rows)
        fill-chars    (repeatedly (fn [] fill-char))     ;TODO: WRONG!!
        col-layout    (expand-fills col-layout width col-widths fill-chars)
        layout-row    (make-col-layout-fn col-layout col-widths align-char)
        result        (mapv layout-row rows)
        result        (if (get-in layout-config [:layout :rows])
                        (apply-row-layout layout-config col-widths result)
                        result)]
    (if raw? result (mapv join result))))

(comment

  (println
    (join \newline
          (layout
            (str "alice, why is a raven" \newline
                 "like a writing desk?" \newline
                 "Have you guessed the riddle" \newline
                 "yet?")
            {:layout {:cols   "│ [L] │ [C] │ [R] │ [R] │ [C] │"
                      :rows [["┌─[─]─┬─[─]─┬─[─]─┬─[─]─┬─[─]─┐" :apply-when first-row?]
                             ["├─[─]─┼─[─]─┼─[─]─┼─[─]─┼─[─]─┤" :apply-when interior-row?]
                             ["└─[─]─┴─[─]─┴─[─]─┴─[─]─┴─[─]─┘" :apply-when last-row?]]}})))

  (println
    (join \newline
          (layout
            (str "hdr1 hdr2 hdr3 hdr4 hdr5" \newline
                 "alice, why is a raven" \newline
                 "like a writing desk?" \newline
                 "Have you guessed the riddle" \newline
                 "yet?")
            {:layout    {:cols   "| [L] | [C] | [R] | [R] | [C] |"
                         :rows [["|:[-] |:[-]:| [-]:| [-]:|:[-]:|" :apply-when second-row?]]}})))

  )

