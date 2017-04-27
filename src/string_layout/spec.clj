(ns string-layout.spec
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.spec.gen :as gen])
  (:import (clojure.lang StringSeq)))

(defn check [type data]
  (if (s/valid? type data)
    true
    (throw (AssertionError. (s/explain type data)))))


;(s/def ::layout-string
;  (s/and string?
;         not-empty
;         (s/conformer seq)
;         (s/+ (s/alt :col-align-expr ::col-align-expr
;                     :col-padding-expr ::col-padding-expr))))

(s/def ::layout-string
  (s/and string?
         not-empty
         (s/conformer seq #(apply str %))
         (s/+ (s/alt :col-align-bracket-expr
                     (s/cat :left-bracket #{\[}
                            :col-align-char #{\L \C \R \l \c \r}
                            :right-bracket #{\]})
                     :col-padding-non-bracket
                     (s/+ (complement #{\[ \]}))))
         (s/conformer
           (fn [v]
             (prn v)
             (first (reduce
                      (fn [[res padv ] x]
                        (if (= :col-padding-non-bracket (first x))
                          [res (conj padv (second x))]
                          [(conj res
                                 [:col-padding-non-bracket (apply str padv)]
                                 [:col-align-bracket-expr (get-in x [1 :col-align-char])]) []]))
                      [[] []]
                      v))))))
  
(defn char-seq->str-conformer [v]
  (let [[head tail]
        (reduce
          (fn [[res padv] x]
            (if (= :col-padding-non-bracket-char (first x))
              [res (conj padv (second x))]
              (let [align [:col-align (get-in x [1 :col-padding-non-bracket-char])]]
                (if (empty? padv)
                  [(conj res align) padv]
                  [(conj res [:col-padding (apply str padv)]
                         align) []]))))
          [[] []]
          v)]
    (prn "head:" head "tail:" tail)
    (if (empty? tail)
      head
      (conj head [:col-padding (apply str tail)]))))

(s/def ::layout-string3
  (s/and string?
         not-empty
         (s/conformer seq #(apply str %))
         (s/+ (s/alt :col-align-bracket-expr
                     (s/cat :left-bracket #{\[}
                            :col-align-char #{\L \C \R \l \c \r}
                            :right-bracket #{\]})
                     :col-padding-non-bracket-char
                     (complement #{\[ \]})))
         (s/conformer char-seq->str-conformer)
         ))


(s/def ::layout-string2
  (s/and string?
         not-empty
         (s/conformer seq #(apply str %))
         (s/+ (s/alt :col-align-bracket-expr
                     (s/cat :left-bracket #{\[}
                            :col-align-char #{\L \C \R \l \c \r}
                            :right-bracket #{\]})
                     :col-padding-non-bracket
                     (complement #{\[ \]})))))

;; parsed 
(s/def ::parsed-spaces
  (s/* (s/alt :fill-specifier #(= % :F) :pad-str string?)))

(s/def ::parsed-aligns
  (s/* #{:L :C :R}))


(s/def ::stmt-fist-line
  (s/cat :
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
