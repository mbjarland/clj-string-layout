(ns string-layout.core
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :refer [split join]]))

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
                    :else [in-brace
                           aligns
                           (conj (into [] (butlast spaces)) (str (last spaces) c))]))
                [false [] [""]]
                layout-string)]
    [aligns
     (mapv #(if (= (.toLowerCase %) "fill") :F %) spaces)]))

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


(defn expand-fills [spaces fill-width align-char i]
  (prn spaces i (= (nth spaces i) :F) "fill-width" fill-width "align char" align-char)
  (if (= (nth spaces i) :F)
    (apply str (repeat fill-width align-char))
    (nth spaces i)))

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

(defn expand [spaces width col-widths align-char]
  (let [fill-width (max 0 (- width (+ (reduce + col-widths)
                                      (reduce + (keep #(if (string? %) (count %)) spaces)))))
        len (dec (count spaces))
        fill-count (count (filter keyword? spaces))
        width-per (/ fill-width fill-count)
        int-per (int width-per)
        diff (- width-per int-per)
        fill (join (repeat int-per " "))]
    (first
      (reduce
        (fn [[res e i] x]
          (cond
            (not= (nth spaces i) :F) [(conj res (nth spaces i)) e (inc i)]
            (= i len) (if (zero? e)  [(conj res fill) 0 (inc i)]
                                     [(conj res (str fill " ")) 0 (inc i)]) [(conj res (str fill " ")) 0 (inc i)]
            (= (int e) 1)            [(conj res (str fill " ")) 0 (inc i)]
            :else                    [(conj res fill) (+ e diff) (inc i)]))
        [[] 0 0]
        spaces))))

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
