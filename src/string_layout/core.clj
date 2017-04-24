(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]
            [clojure.spec   :as s]))

(defn parse-align [^Character c]
  (case (Character/toUpperCase c)
    \L :L
    \R :R
    \C :C
    (throw (IllegalArgumentException. "invalid align char: " c)))) ;; TODO: figure out x


(s/def ::layout-string (s/coll-of
                         (s/alt :f keyword? :s string?)
                         :kind vector?))


(defn expand-fills [spaces width col-widths align-char]
  {:pre []}
  (when (pos? (count (filter keyword? spaces)))
    (let [fill-count (count (filter keyword? spaces))
          fill-width (max 0 (- width (+ (reduce + col-widths)
                                        (reduce + (keep #(if (string? %) (count %)) spaces)))))
          int-per (int (/ fill-width fill-count))
          doubles (- fill-width (* fill-count int-per))
          fills (mapv
                  #(join (repeat % align-char))
                  (mapv +
                        (repeat fill-count int-per)
                        (concat (repeat doubles 1)
                                (repeat (- fill-count doubles) 0))))
          spaces (first
                  (reduce
                    (fn [[res i f] x]
                      (if (= (nth spaces i) :F)
                        [(conj res (nth fills f)) (inc i) (inc f)]
                        [(conj res x) (inc i) f]))
                    [[] 0 0]
                    spaces))]))
  spaces)

(defn parse-layout-string [layout-string]
  {:pre [(not-empty layout-string)]}
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
  (map (fn [row]
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
(defn layout [rows layout-string layout-config]
  {:pre [(pos? (count rows))]}
  (let [{:keys [align-char width]} layout-config
        [aligns spaces] (parse-layout-string layout-string)
        rows (if (instance? String rows) [] (normalize-rows (count aligns) rows))
        col-widths (map #(apply max (map count %)) (transpose rows))
        fill-width (max 0 (- width (+ (reduce + col-widths)
                                      (reduce + (keep #(if (string? %) (count %)) spaces)))))
        align (partial align-word aligns col-widths align-char)
        spaces (expand-fills spaces width col-widths align-char)
        space (partial expand-fills spaces fill-width align-char)
        indent-row (fn [row]
                     (second
                       (reduce (fn [[i r] w]
                                 [(inc i) (str r (align w i) (space (inc i)))])
                               [0 (first spaces)]
                               row)))]
    (map indent-row rows)))

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
