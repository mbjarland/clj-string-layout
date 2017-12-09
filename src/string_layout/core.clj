(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]
            [string-layout.spec])
  (:import [clojure.lang StringSeq]))

(defn parse-align [^Character c]
  (case (Character/toUpperCase c)
    \L :L
    \R :R
    \C :C
    (throw (IllegalArgumentException. "invalid align char: " c)))) ;; TODO: figure out x


(defn valid-layout-string? [s]
  (re-matches #".*" s))                                     ;;TODO: regex for layout string

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


(declare parse-layout-string)
(s/fdef parse-layout-string
        :args :string-layout.spec/layout-string
        :ret (s/cat :aligns :string-layout.spec/parsed-aligns
                    :spaces :string-layout.spec/parsed-spaces))

(defn parse-layout-string
  "parses a col-layout string"
  [layout-string]
  {:pre [(string-layout.spec/check :string-layout.spec/layout-string layout-string)]}
  (let [[_ aligns spaces]
        (reduce (fn [[in-brace aligns spaces] c]
                  (cond
                    (= c \[) [true aligns spaces]
                    (= c \]) [false aligns (conj spaces "")]
                    in-brace [in-brace (conj aligns (parse-align c)) spaces]
                    :else [in-brace
                           aligns
                           (conj (into [] (butlast spaces)) (str (last spaces) c))]))
                [false [] [""]]
                layout-string)
        spaces (mapv #(if (= (.toLowerCase %) "f") :F %) spaces)]
    [aligns spaces]))

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

(def layout-config
  {:align-char \space
   :width      10
   :borders    border-markdown
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


; [1 1 1 1]
; [2 1 1 1]
; [2 2 1 1]
; [2 2 2 1]
; [2 2 2 2] - not needed
;
; in the below
; 7 is the fill-width, 1 is (int width-per),
;
; (some #(if (= (reduce + %) 7) %)
;       (into [] (for [a [1 2]
;                      b [1 2]
;                      c [1 2]
;                      d [1 2] :when (>= a b c d)]
;                  [a b c d])))
;
;  => [2 2 2 1
;
;
;
(defn rep [q i]
  (int (reduce + (repeat i q))))

; TODO: read up on mig layout, change precondition
; TODO: fix parsing failure handling
(defn layout
  "Lays out rows of text in columns. The first argument can either
  be a string with spaces between 'words' and newlines between each
  row _or_ a vector of vector of strings representing the rows
  and the words (columns) within the rows. The second
  argument is a layout string on the form ' [L]f[R] [R] ' and
  the third argument is a layout config on the form
  {:width 80 :align-char \\space :raw false} where width is the
  total width of the row (any 'f' specifiers between columns will
  fill the row out to this width), the align-char is the character to
  use when filling to width, and raw (default false) will, if true,
  return rows as vectors of indented columns and column spacings instead of
  returning rows as already joined strings. The raw format can be useful
  if you need to do some post processing like adding ansi colors to
  certain columns before outputting to terminal etc."
  [rows layout-string layout-config]
  {:pre [(pos? (count rows))]}
  (let [{:keys [align-char width] :or {raw false}} layout-config
        [aligns spaces] (parse-layout-string layout-string)
        rows       (if (instance? String rows) (mapv #(split % #" ") (split rows #"\n"))
                                               (normalize-rows (count aligns) rows))
        col-widths (map #(apply max (map count %)) (transpose rows))
          align      (partial align-word aligns col-widths align-char)
        spaces     (expand-fills spaces width col-widths align-char)
        ;_          (prn "spaces:" spaces)
        ;_          (prn "col-widths:" col-widths)
        ;_          (prn "fill-width:" fill-width)
        ;_          (prn "aligns:" aligns)
        ;_          (prn "spaces:" spaces)
        ;_          (prn "rows:" rows)
        ;
        indent-row (fn [row]
                     (second
                       (reduce (fn [[i r] w]
                                 [(inc i) (conj r (align w i) (nth spaces (inc i)))])
                               [0 [(first spaces)]]
                               row)))
        result (mapv indent-row rows)]
    (mapv join result)))

;  rows.collect { List<String> row ->
;    // can not use
;    // row.indexed().inject(space(0)) { rowResult, wi, String word
;    // as indexed() is a groovy 2.4.x feature
;    def wi = -1
;    row.inject(space(0)) { rowResult, String word ->
;      wi++
;      rowResult << align(word, wi) << space(wi+1)
;    } as String

;public List<String> layout(List<List<String>> rows) {
;  if (rows.size() == 0) return []
;
;  def (aligns, spaces) = layoutStringParsed
;
;  def colWidths = rows.transpose().collect { List<String> col ->
;    col.max { String word -> word.length() }.length()
;  }
;
;  def align = { String word, int wi ->
;    switch(aligns[wi]) {
;      case Align.LEFT:  return word.padRight(colWidths[wi], alignChar)
;      case Align.RIGHT: return word.padLeft(colWidths[wi], alignChar)
;      default: throw new RuntimeException("Unsupported alignment operation '${aligns[wi]}' encountered, index: $wi, aligns: $aligns!")
;    }
;  }
;
;
;  int fillWidth = width - (colWidths.sum() + spaces.collect { s -> s == 'fill' ? 0 : s.length() }.sum())
;  if (fillWidth < 0) fillWidth = 0
;
;  def space = { int wi ->
;    spaces[wi] == 'fill' ? (alignChar*fillWidth) : spaces[wi]
;  }
;
;  rows.collect { List<String> row ->
;    // can not use
;    // row.indexed().inject(space(0)) { rowResult, wi, String word
;    // as indexed() is a groovy 2.4.x feature
;    def wi = -1
;    row.inject(space(0)) { rowResult, String word ->
;      wi++
;      rowResult << align(word, wi) << space(wi+1)
;    } as String
;  }
;}
