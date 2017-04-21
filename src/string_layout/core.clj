(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split]]))

(defn parse-align [^Character c]
  (case (Character/toUpperCase c)
    \L :L
    \R :R
    \C :C
    (throw IllegalArgumentException)))                      ;; TODO: figure out x

(defn parse-layout-string [layout-string]
  {:pre [(not-empty layout-string)]}
  (let [[_ aligns spaces]
        (reduce (fn [[in-brace aligns spaces] c]
                  (cond
                    (= c \[) [true aligns spaces]
                    (= c \]) [false aligns (conj spaces "")]
                    in-brace [in-brace (conj aligns (parse-align c)) spaces]
                    :else [in-brace aligns (conj (into [] (butlast spaces)) (str (last spaces) c))]))
                [false [] [""]]
                layout-string)]
    [aligns
     (map #(if (= (.toLowerCase %) "fill") "fill" %) spaces)]))

(defn transpose [vs]
  (into []
        (comp
          (map (fn [i]
                 (into [] (comp (filter #(contains? % i))
                                (map #(nth % i)))
                       vs)))
          (take-while seq))
        (range)))

(def layout-config
  {:align-char \space
   :width      10
   }
  )

; TODO: read up on mig layout, change precondition
(defn layout [rows layout-string layout-config]
  {:pre [(pos? (count rows))]}
  (let [{:keys [align-char width]} layout-config
        rows (if (instance? String rows) [] rows)
        [aligns spaces] (parse-layout-string layout-string)
        col-widths (map #(apply max (map count %)) (transpose rows))
        fill-width (max 0 (- width (+ (reduce + col-widths)
                                      (reduce + (map count (filter #(not= % "fill")))))))
        fail (fn [msg] (throw (IllegalArgumentException. ^String msg)))

        align (fn [w i]
                (case (nth aligns i)
                  :L (cl-format nil "~v,,,vA" align-char width w)
                  :R (cl-format nil "~v,,,v@A" align-char width w)
                  :C (cl-format nil "~v,,,v:@<~A~>" align-char width w)
                  ;:W (clojure.pprint/cl-format nil "~{~<~%~1," size ":;~A~> ~}")
                  (fail (str "Unsupported alignment operation '" (nth aligns i)
                             "' encountered, index: " i ", aligns: " aligns))))
        space (fn [i]
                (if (= (nth spaces i) "fill")
                  (apply str (repeat fill-width align-char))
                  (nth spaces i)))]
    (map #(reduce (fn [[i r] w] [(inc i) (str (align w i) (space (inc i)))])
                  [-1 ""]
                  %)
         rows)))

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
