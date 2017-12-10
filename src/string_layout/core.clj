(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [clojure.spec.alpha :as s]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]))

(comment
  (def layout-config
    {:align-char \space
     :width      10
     :borders    border-markdown
     :raw? false
     })

  )

(defn make-layout-parser-internal []
  (insta/parser
    "layout-string = col-delim (col-align col-delim)+
     col-delim    = (col-fill | col-padding)
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
    (reduce (fn [[aligns spaces] [k v]]
              (cond
                (= k :col-delim) [aligns (conj spaces (if (nil? v) "" v))]
                (= k :col-align) [(conj aligns v) spaces]
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

(defn expand-fills
  "expands the 'f' formatting specifiers in the 'spaces' vector
  to the appropriate number of 'align-char' characters"
  [spaces width col-widths align-char]
  (let [fill-kws (filter #(= :F %) spaces)]
    (if (empty? fill-kws)
      spaces
      (let [fill-count (count fill-kws)
            fill-width (max 0 (- width (+ (reduce + col-widths)
                                          (reduce + (keep #(if (string? %) (count %)) spaces)))))
            int-per    (int (/ fill-width fill-count))
            doubles    (- fill-width (* fill-count int-per))
            fills      (mapv
                         #(join (repeat % align-char))
                         (mapv +
                               (repeat fill-count int-per)
                               (concat (repeat doubles 1)
                                       (repeat (- fill-count doubles) 0))))]
        (first
          (reduce
            (fn [[res i f] x]
              (if (= (nth spaces i) :F)
                [(conj res (nth fills f)) (inc i) (inc f)]
                [(conj res x) (inc i) f]))
            [[] 0 0]
            spaces))))))


;(declare parse-layout-string)
;(s/fdef parse-layout-string
;        :args :string-layout.spec/layout-string
;        :ret (s/cat :aligns :string-layout.spec/parsed-aligns
;                    :spaces :string-layout.spec/parsed-spaces))
;
;(defn parse-layout-string
;  "parses a col-layout string"
;  [layout-string]
;  {:pre [(string-layout.spec/check :string-layout.spec/layout-string layout-string)]}
;  (let [[_ aligns spaces]
;        (reduce (fn [[in-brace aligns spaces] c]
;                  (cond
;                    (= c \[) [true aligns spaces]
;                    (= c \]) [false aligns (conj spaces "")]
;                    in-brace [in-brace (conj aligns (parse-align c)) spaces]
;                    :else [in-brace
;                           aligns
;                           (conj (into [] (butlast spaces)) (str (last spaces) c))]))
;                [false [] [""]]
;                layout-string)
;        spaces (mapv #(if (= (.toLowerCase %) "f") :F %) spaces)]
;    [aligns spaces]))

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

(defn transpose [vs]
  (into []
        (comp
          (map (fn [i]
                 (into [] (comp (filter #(contains? % i))
                                (map #(nth % i)))
                       vs)))
          (take-while seq))
        (range)))

(def border-ascii-box
  {
   :outer  ["┌─┐"
            "│ │"
            "└─┘"]
   :inner  ["│─"]
   :header ["┌─┐"
            "│ │"
            "┝━┥"]
   })

(def border-markdown
  {
   :outer  [nil
            "| |"
            nil]
   :inner  ["|x"]
   :header [nil
            "│ │"
            "|-|"]
   })

(defn normalize-rows [col-count rows]
  "Add empty elements to any rows which have fewer elements
  col-count"
  (mapv (fn [row]
          (let [c (count row)
                d (- col-count c)]
            (cond
              (zero? d) row
              (pos? d) (into [] (concat row (repeat d "")))
              :else (subvec row 0 col-count))))
        rows))

(defn align-word [aligns col-widths align-char word i]
  (letfn [(fmt [f] (cl-format nil f align-char (nth col-widths i) word))]
    (case (nth aligns i)
      :L (fmt "~v,,,vA")
      :R (fmt "~v,,,v@A")
      :C (fmt "~v,,,v:@<~A~>")
      ;:W (fmt (str "~{~<~%~1,"  ":;~A~> ~}"))
      :else (throw (IllegalArgumentException.
                     (str "Unsupported alignment operation '" (nth aligns i)
                          "' encountered, index: " i ", aligns: " aligns))))))


(defn parse-rows [rows aligns]
  (let [r (if (instance? String rows) (mapv #(split % #" ") (split rows #"\n"))
                                      rows)]
  (normalize-rows (count aligns) r)))

(defn calculate-col-widths [rows]
  (map #(apply max (map count %)) (transpose rows)))

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
  (let [{:keys [align-char width raw?] :or {raw? false}} layout-config
        [aligns spaces] (parse-layout-string layout-string)
        rows       (parse-rows rows aligns)
        col-widths (calculate-col-widths rows)
        align      (partial align-word aligns col-widths align-char)
        spaces     (expand-fills spaces width col-widths align-char)
        indent-row (fn [row]
                     (second
                       (reduce (fn [[i r] w]
                                 [(inc i) (conj r (align w i) (nth spaces (inc i)))])
                               [0 [(first spaces)]]
                               row)))
        result     (mapv indent-row rows)]
    (if raw? result (mapv join result))))

