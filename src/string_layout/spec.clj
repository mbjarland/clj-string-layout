(ns string-layout.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen])
  (:import [clojure.lang StringSeq]))

(defn check [type data]
  (if (s/valid? type data)
    true
    (throw (AssertionError. (s/explain type data)))))


;(s/def ::layout-string
;  (s/and string?
;         not-empty
;         (s/conformer seq)
;         (s/+ (s/alt :col-align-expr ::col-align-expr
;                    :col-padding-expr ::col-padding-expr))))

(defn char-seq->str-conformer [v]
  (let [[head tail]
        (reduce
          (fn [[res padv] [k v]]
            (if (= k :col-padding-non-bracket-char)
              [res (conj padv v)]
              (let [align [:col-align (:col-align-char v)]
                    padding [:col-padding (apply str padv)]]
                (if (empty? padv)
                  [(conj res align) []]
                  [(conj res padding align) []]))))
          [[] []]
          v)]
    (if (empty? tail)
      head
      (conj head [:col-padding (apply str tail)]))))

(s/def ::layout-string
  (s/and string?
         not-empty
         (s/conformer seq #(apply str %))
         (s/+ (s/alt :col-align-bracket-expr
                     (s/cat :left-bracket #{\[}
                            :col-align-char #{\L \C \R \l \c \r}
                            :right-bracket #{\]})
                     :col-padding-non-bracket-char
                     (complement #{\[ \]})))
         (s/conformer char-seq->str-conformer)))

;; parsed 
(s/def ::parsed-spaces
  (s/* (s/alt :fill-specifier #(= % :F) :pad-str string?)))

(s/def ::parsed-aligns
  (s/* #{:L :C :R}))


(s/def ::stmt-fist-line
  (s/cat
    
    ))

(comment
  (defn parse-statement-1st-line [first-line]
    "Transform the first line of a csv statement into data map"
    (let [openpar (str/last-index-of first-line \()
          closepar (str/index-of first-line \) openpar)
          lastspace (str/index-of first-line \space (+ 2 closepar))]
      {:ticker (subs first-line (+ 1 openpar) closepar)
       :name   (strip (str/trim (subs first-line 0 openpar)) "\"")
       :type   (str->type (strip (str/trim (subs first-line lastspace)) "\""))}))

  (defn parse-statement-2nd-line [second-line]
    "Transform the second line of a csv statement into data map.
Note that we filter out any TTM values."
    (let [[fy-month & dates] (first (csv/parse-csv second-line))
          mbeg (str/index-of fy-month " in ")
          mend (str/index-of fy-month ".")]
      {:fy-ending-month (month-name->int (subs fy-month (+ mbeg 4) mend))
       :dates           (map str->last-day-of-the-month (filter #(not= "TTM" %) dates))}))

  )
