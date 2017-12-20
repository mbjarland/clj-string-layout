(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]))

(defn make-layout-parser-internal []
  (insta/parser
    "layout-string = col-delim (col-align col-delim)+
     col-delim    = (col-fill | col-padding)*
     col-fill     = ('F' | 'f')
     col-padding  = #'[^\\[\\]fF]*'
     col-align    = <'['> ('L' | 'l' | 'C' | 'c' | 'R' | 'r') <']'>"))

(def make-layout-parser (memoize make-layout-parser-internal))

(defn transform-parsed [p]
  "transforms the parse tree returned from instaparse
  to two vectors 'aligns' and 'spaces'"
  (let [t (insta/transform
            {:layout-string vector
             :col-fill      (fn [_] :F)
             :col-padding   identity
             :col-align     (fn [a]
                              [:col-align (-> a .toUpperCase keyword)])}
            p)]
    (reduce (fn [[aligns spaces] [k & v]]
              (cond
                (= k :col-delim) [aligns (conj spaces (if (nil? v) [] (into [] v)))]
                (= k :col-align) [(conj aligns (first v)) spaces]
                :else [aligns spaces]))
            [[] []]
            t)))

(defn throw-parse-error [parsed s]
  (let [msg (with-out-str (fail/pprint-failure parsed))]
    (throw (ex-info (str "error parsing layout string '" s "':\n" msg)
                    {:failure (insta/get-failure parsed)}))))


(defn parse-layout-string [s]
  (let [parser (make-layout-parser)
        parsed (parser s)]
    (if (insta/failure? parsed)
      (throw-parse-error parsed s)
      (transform-parsed parsed))))

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
  [fill-width fill-count align-char]
  (let [[q sr] ((juxt quot rem) fill-width fill-count)
        r (/ sr fill-count)
        s (map int (iterate #(+ r %) r))                    ; 0 1 2 2 3 4
        n (join (repeat q align-char))]
    (first (reduce
             (fn [[a l] x]
               [(conj a (if (= l x) n (str n align-char))) x])
             [[] 0]
             (take fill-count s)))))

(defn expand-fills
  "expands the :F formatting specifiers in the spaces vector
  to the appropriate number of align-char characters"
  [spaces width col-widths align-char]
  (let [f? (partial = :F)
        fs (flatten spaces)]
    (if (not-any? f? fs)
      spaces
      (let [fill-count (count (filter f? fs))
            sum-cols   (reduce + col-widths)
            sum-spaces (reduce + (keep #(when (string? %) (count %)) fs))
            fill-width (max 0 (- width (+ sum-cols sum-spaces)))
            fills      (calculate-fills fill-width fill-count align-char)
            fill-idx   (atom -1)]
        (mapv (fn [v]
                (mapv #(if (f? %) (nth fills (swap! fill-idx inc)) %) v))
              spaces)))))

(s/fdef expand-fills
        :args (s/and (s/cat :spaces (s/cat :layout-element
                                           (s/* (s/alt :f #(= % :F) :s string?)))
                            :width nat-int?
                            :col-widths (s/cat :col-width
                                               (s/* nat-int?))
                            :align-char char?)
                     (fn [{:keys [spaces width col-widths]}]
                       (> width (+ (reduce + col-widths)
                                   (reduce + (map count (filter string? spaces)))))))
        :ret (s/cat :layout-element (s/* string?))
        :fn (s/and (fn [{:keys [spaces ret]}]
                     (= (count ret) (count spaces)))
                   (fn [{:keys [width col-widths ret]}]
                     (= (+ (reduce + (map count ret))
                           (reduce + col-widths))
                        width))))

(comment
  ; row-layout
  ; 2 elements - assumes top and bottom, no middle delims
  ; 3 elements - assumes top middle bottom and that
  ;              middle is always applied
  ; more       - applies middles with first matching predicate


  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ 🡐 0
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; ├───────────────┼───────────────┼───────┤ 🡐 1
  ; │ col 2 is      │   centered    │   $12 │
  ; ├───────────────┼───────────────┼───────┤ 🡐 2
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"]
                ["├─[─]─┼─[─]─┼─[─]─┤"]
                ["└─[─]─┴─[─]─┴─[─]─┘"]]}


  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐
  ; │ Tables        │ Are           │ Cool  │
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"]
                ["└─[─]─┴─[─]─┴─[─]─┘"]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ 🡐 0
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘

  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when zero?]
                ["└─[─]─┴─[─]─┴─[─]─┘"]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐
  ; │ Tables        │     Are       │  Cool │
  ; ┝━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━┿━━━━━━━┥ 🡐 0
  ; │ col 3 is      │   centered    │ $1600 │
  ; ├───────────────┼───────────────┼───────┤ 🡐 1
  ; │ col 2 is      │   centered    │   $12 │
  ; ┝━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━┿━━━━━━━┥ 🡐 2
  ; │ col 2 is      │   centered    │   $12 │
  ; ├───────────────┼───────────────┼───────┤ 🡐 3
  ; │ zebra stripes │   are neat    │    $1 │
  ; └───────────────┴───────────────┴───────┘
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"]
                ["┝━[━]━┿━[━]━┿━[━]━┥" :apply-when even?]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when odd?]
                ["└─[─]─┴─[─]─┴─[─]─┘"]]}
  
  ; | Tables        | Are           | Cool  |
  ; | ------------- |:-------------:| -----:|
  ; | col 3 is      | right-aligned | $1600 |
  ; | col 2 is      | centered      |   $12 |
  ; | zebra stripes | are neat      |    $1 |

  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [nil
                ["| [-] |:[-]:| [-]:|" :apply-when zero?]
                nil]}
  )

(defn normalize-row-lens [col-count rows]
  "Add empty elements to any rows which have fewer elements
  than col-count"
  (mapv #(into [] (take col-count (concat % (repeat ""))))
        rows))

(defn normalize-rows [rows aligns split-char]
  (let [p (re-pattern (str \\ split-char))
        r (if (instance? String rows) (mapv #(split % p) (split rows #"\n"))
                                      rows)]
    (normalize-row-lens (count aligns) r)))

(defn calculate-col-widths [rows]
  (apply mapv #(apply max (map count %&)) rows))

(defn align-word [aligns col-widths align-char word col]
  (letfn [(fmt [f] (cl-format nil f align-char (nth col-widths col) word))]
    (case (nth aligns col)
      :L (fmt "~v,,,vA")
      :R (fmt "~v,,,v@A")
      :C (fmt "~v,,,v:@<~A~>")
      ;:W (fmt (str "~{~<~%~1,"  ":;~A~> ~}"))
      :else (throw (IllegalArgumentException.
                     (str "Unsupported alignment operation '" (nth aligns col)
                          "' encountered at align index: " col " in " aligns))))))

(def default-layout-config
  {
   :align-char \space
   :split-char \space
   :width      80
   :raw?       false
   })

(comment 
(s/def ::rows-to-layout)
)
(s/def ::layout-string (s/and string? not-empty))
(s/def ::width pos-int?)
(s/def ::align-char char?)
(s/def ::split-char char?)
(s/def ::raw boolean?)

(s/def ::bob (s/cat :rows-to-layout (s/or :string-with-newlines (s/and string? not-empty)
                                          :nested-vector-of-words (s/coll-of (s/coll-of string? :kind vector?) :kind vector?))
                    :layout-string (s/and string? not-empty)
                    :layout-config (s/keys :opt [::width
                                                 ::align-char
                                                 ::split-char
                                                 ::raw?])))

(s/fdef layout
        :args (s/cat :rows-to-layout (s/or :string-with-newlines (s/and string? not-empty)
                                           :nested-vector-of-words (s/coll-of (s/coll-of string? :kind vector?) :kind vector?))
                     :layout-string (s/and string? not-empty)
                     :layout-config (s/keys :opt [::width
                                                  ::align-char
                                                  ::split-char
                                                  ::raw?]))
        :ret (s/cat ::layout-element (s/coll-of string? :kind vector?)))

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
  [rows layout-string layout-config]
  {:pre [(pos? (count rows))]}
  (let [config     (merge default-layout-config layout-config)
        {:keys [align-char split-char width raw?]} config
        [aligns spaces] (parse-layout-string layout-string)
        rows       (normalize-rows rows aligns split-char)
        col-widths (calculate-col-widths rows)
        align      (partial align-word aligns col-widths align-char)
        spaces     (expand-fills spaces width col-widths align-char)
        layout-row (fn [row] (str (join (interleave (map join spaces)
                                                    (map-indexed #(align %2 %1) row)))
                                  (join (last spaces))))
        result     (mapv layout-row rows)]
    (if raw? result (mapv join result))))

(stest/instrument `why layout)

