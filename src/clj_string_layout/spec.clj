; (ns string-layout.spec
;   (:require [clojure.spec.alpha :as s]
;             [clojure.spec.test.alpha :as stest]
;             [clojure.spec.gen.alpha :as gen])
;   (:import [clojure.lang StringSeq]))


; (s/fdef expand-fills
;         :args (s/and (s/cat :spaces (s/cat :layout-element
;                                            (s/* (s/alt :f #(= % :F) :s string?)))
;                             :width nat-int?
;                             :col-widths (s/cat :col-width
;                                                (s/* nat-int?))
;                             :align-char char?)
;                      (fn [{:keys [spaces width col-widths]}]
;                        (> width (+ (reduce + col-widths)
;                                    (reduce + (map count (filter string? spaces)))))))
;         :ret (s/cat :layout-element (s/* string?))
;         :fn (s/and (fn [{:keys [spaces ret]}]
;                      (= (count ret) (count spaces)))
;                    (fn [{:keys [width col-widths ret]}]
;                      (= (+ (reduce + (map count ret))
;                            (reduce + col-widths))
;                         width))))

; (comment
;   (s/def ::rows-to-layout)
;   )
; (s/def ::layout-string (s/and string? not-empty))
; (s/def ::width pos-int?)
; (s/def ::align-char char?)
; (s/def ::split-char char?)
; (s/def ::raw boolean?)

; (s/def ::bob (s/cat :rows-to-layout (s/or :string-with-newlines (s/and string? not-empty)
;                                           :nested-vector-of-words (s/coll-of (s/coll-of string? :kind vector?) :kind vector?))
;                     :layout-string (s/and string? not-empty)
;                     :layout-config (s/keys :opt [::width
;                                                  ::align-char
;                                                  ::split-char
;                                                  ::raw?])))

; (s/fdef layout
;         :args (s/cat :rows-to-layout (s/or :string-with-newlines (s/and string? not-empty)
;                                            :nested-vector-of-words (s/coll-of (s/coll-of string? :kind vector?) :kind vector?))
;                      :layout-string (s/and string? not-empty)
;                      :layout-config (s/keys :opt [::width
;                                                   ::align-char
;                                                   ::split-char
;                                                   ::raw?]))
;         :ret (s/cat ::layout-element (s/coll-of string? :kind vector?)))


; (comment

;   (defn check [type data]
;     (if (s/valid? type data)
;       true
;       (throw (AssertionError. (s/explain type data)))))


;   ;(s/def ::layout-string
;   ;  (s/and string?
;   ;         not-empty
;   ;         (s/conformer seq)
;   ;         (s/+ (s/alt :col-align-expr ::col-align-expr
;   ;                    :col-padding-expr ::col-padding-expr))))

;   (defn char-seq->str-conformer [v]
;     (let [[head tail]
;           (reduce
;             (fn [[res padv] [k v]]
;               (if (= k :col-padding-non-bracket-char)
;                 [res (conj padv v)]
;                 (let [align   [:col-align (:col-align-char v)]
;                       padding [:col-padding (apply str padv)]]
;                   (if (empty? padv)
;                     [(conj res align) []]
;                     [(conj res padding align) []]))))
;             [[] []]
;             v)]
;       (if (empty? tail)
;         head
;         (conj head [:col-padding (apply str tail)]))))

;   (s/def ::layout-string
;     (s/and string?
;            not-empty
;            (s/conformer seq #(apply str %))
;            (s/+ (s/alt :col-align-bracket-expr
;                        (s/cat :left-bracket #{\[}
;                               :col-align-char #{\L \C \R \l \c \r}
;                               :right-bracket #{\]})
;                        :col-padding-non-bracket-char
;                        (complement #{\[ \]})))
;            (s/conformer char-seq->str-conformer)))

;   ;; parsed
;   (s/def ::parsed-spaces
;     (s/* (s/alt :fill-specifier #(= % :F) :pad-str string?)))

;   (s/def ::parsed-aligns
;     (s/* #{:L :C :R}))


;   (s/def ::stmt-fist-line
;     (s/cat

;       ))

;   (defn parse-statement-1st-line [first-line]
;     "Transform the first line of a csv statement into data map"
;     (let [openpar   (str/last-index-of first-line \()
;           closepar  (str/index-of first-line \) openpar)
;           lastspace (str/index-of first-line \space (+ 2 closepar))]
;       {:ticker (subs first-line (+ 1 openpar) closepar)
;        :name   (strip (str/trim (subs first-line 0 openpar)) "\"")
;        :type   (str->type (strip (str/trim (subs first-line lastspace)) "\""))}))

;   (defn parse-statement-2nd-line [second-line]
;     "Transform the second line of a csv statement into data map.
; Note that we filter out any TTM values."
;     (let [[fy-month & dates] (first (csv/parse-csv second-line))
;           mbeg (str/index-of fy-month " in ")
;           mend (str/index-of fy-month ".")]
;       {:fy-ending-month (month-name->int (subs fy-month (+ mbeg 4) mend))
;        :dates           (map str->last-day-of-the-month (filter #(not= "TTM" %) dates))}))

;   )
