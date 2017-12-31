(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]))

(defn count-by [pred coll]
  (count (keep pred coll)))

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

(defn parse-layout-string [s]
  (let [parser (make-col-layout-parser)
        parsed (parser s)]
    (if (insta/failure? parsed)
      (throw-parse-error parsed s)
      (transform-parsed parsed false))))

; 10 6 -- 5 3 -> rest 2 -> 2/3
;   ["*"  "**" "**" "*"  "**" "**"]
;   ["*"  "*"  "*"  "*"  "*"  "*"]
;   [ 1    2    3    4    5    6]
;   [ 2/3  4/3  6/3  8/3  10/3 12/3]
; - [ 0    1    2    2    3    4] ;(int above)
;   [ 0    1    1    0    1    1] ;when changes
(defn calculate-fills2
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

(defn expand-fills
  "expands the :F formatting specifiers in the layout vector
  to the appropriate number of align-char characters"
  [layout width col-widths fill-chars]
  (let [spaces (keep :delim layout)
        f?     (partial = :F)
        fs     (flatten spaces)]
    (if (not-any? f? fs)
      spaces
      (let [fill-count (count (filter f? fs))
            sum-cols   (reduce + col-widths)
            sum-spaces (reduce + (keep #(when (string? %) (count %)) fs))
            fill-width (max 0 (- width (+ sum-cols sum-spaces)))
            fills      (calculate-fills fill-width fill-count fill-chars)
            fill-idx   (atom -1)]
        (mapv (fn [v]
                (mapv #(if (f? %) (nth fills (swap! fill-idx inc)) %) v))
              spaces)))))

(defn normalize-row-lens [col-count rows]
  "Add empty elements to any rows which have fewer elements
  than col-count"
  (mapv #(into [] (take col-count (concat % (repeat ""))))
        rows))

(defn normalize-rows [rows layout split-char]
  (let [align-count (count-by :align layout)
        p           (re-pattern (str \\ split-char))
        r           (if (instance? String rows) (mapv #(split % p) (split rows #"\n"))
                                                rows)]
    (normalize-row-lens align-count r)))

(defn calculate-col-widths [rows]
  (apply mapv #(apply max (map count %&)) rows))

(defn align-word [layout col-widths align-char word col]
  (let [aligns (keep :align layout)]
    (letfn [(fmt [f] (cl-format nil f align-char (nth col-widths col) word))]
      (case (nth aligns col)
        :L (fmt "~v,,,vA")
        :R (fmt "~v,,,v@A")
        :C (fmt "~v,,,v:@<~A~>")
        ;:W (fmt (str "~{~<~%~1,"  ":;~A~> ~}"))
        :else (throw (IllegalArgumentException.
                       (str "Unsupported alignment operation '" (nth aligns col)
                            "' encountered at align index: " col " in " aligns)))))))

(def default-layout-config
  {
   :align-char \space
   :split-char \space
   :width      80
   :raw?       false
   })


(comment
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
  [rows layout-string layout-config]
  {:pre [(pos? (count rows))]}                              ;TODO: replace predicate with spec
  (let [config     (merge default-layout-config layout-config)
        {:keys [align-char split-char width raw?]} config
        [layout] (parse-layout-string layout-string)
        rows       (normalize-rows rows layout split-char)
        col-widths (calculate-col-widths rows)
        align      (partial align-word layout col-widths align-char)
        delims     (expand-fills layout width col-widths align-char)
        layout-row (fn [row] (str (join (interleave (map join delims)
                                                    (map-indexed #(align %2 %1) row)))
                                  (join (last delims))))
        result     (mapv layout-row rows)]
    (if raw? result (mapv join result))))


