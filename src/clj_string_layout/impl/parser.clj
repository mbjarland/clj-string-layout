(ns ^:no-doc clj-string-layout.impl.parser
  (:require [clj-string-layout.impl.error :refer [layout-error parse-options!]]
            [instaparse.core :as insta]
            [instaparse.failure :as fail]))

(def grammar
  "layout = delim? ((col | repeat) delim?)*
   repeat = <'{'> delim? (col delim?)* <'}'>
   delim  = (escaped | fill | #'[^\\\\\\[\\]{}fF]+')+
   escaped = <'\\\\'> #'.'
   fill   = <'F'> (#'[\\d]+')?
   col    = <'['> fill? align fill? <']'>")

(def col-grammar (str grammar \newline "align = ('L'|'C'|'R'|'V')"))
(def row-grammar (str grammar \newline "align = #'[^]]'"))

(defonce col-parser (delay (insta/parser col-grammar :string-ci true)))
(defonce row-parser (delay (insta/parser row-grammar :string-ci true)))

(defn- parser [row-layout?]
  (if row-layout? @row-parser @col-parser))

(defn- parse-error [parsed layout-string]
  (let [message (with-out-str (fail/pprint-failure parsed))]
    (layout-error (str "Error parsing layout string '" layout-string "':\n" message)
                  {:type :layout-parse-error
                   :layout-string layout-string
                   :failure (insta/get-failure parsed)})))

(defn- transform-parsed [row-layout? parsed-layout]
  (insta/transform
    {:layout vector
     :fill (fn [& _] {:type :fill})
     :escaped identity
     :repeat (fn [& entries] {:type :repeat :layout (vec entries)})
     :delim (fn [& parts] {:type :delimiter :parts (vec parts)})
     :col (fn [& parts] {:type :column* :parts (vec parts)})
     :align (fn [value]
              {:type :align
               :value (if row-layout?
                        (first value)
                        (keyword (.toLowerCase ^String value)))})}
    parsed-layout))

(defn- fill? [entry]
  (= :fill (:type entry)))

(defn- align? [entry]
  (= :align (:type entry)))

(defn- text-entry [value]
  {:type :text :value value})

(declare normalize-layout)

(defn- delimiter-entries [{:keys [parts]}]
  (mapcat (fn [part]
            (cond
              (string? part) [(text-entry part)]
              (fill? part) [part]
              :else (layout-error "Invalid delimiter element in layout parse tree"
                                  {:type :invalid-layout-ir
                                   :entry part})))
          parts))

(defn- column-entry [{:keys [parts]}]
  (let [aligns (filter align? parts)]
    (when-not (= 1 (count aligns))
      (layout-error "Column layout markers must contain exactly one alignment"
                    {:type :invalid-layout-ir
                     :parts parts}))
    {:type :column
     :align (:value (first aligns))
     :fills (count (filter fill? parts))
     :fill-widths []}))

(defn- normalize-entry [entry]
  (case (:type entry)
    :delimiter (delimiter-entries entry)
    :column* [(column-entry entry)]
    :repeat [(update entry :layout normalize-layout)]
    (layout-error "Invalid layout entry in parse tree"
                  {:type :invalid-layout-ir
                   :entry entry})))

(defn- normalize-layout [layout]
  (into [] (mapcat normalize-entry) layout))

(defn parse-layout-string
  "Parses a column or row layout string into the internal layout IR."
  [row-layout? layout-string]
  (when-not (string? layout-string)
    (layout-error "Layout specification must start with a layout string"
                  {:type :invalid-layout-spec
                   :layout-string layout-string}))
  (let [parsed-layout ((parser row-layout?) layout-string)]
    (if (insta/failure? parsed-layout)
      (parse-error parsed-layout layout-string)
      (normalize-layout (transform-parsed row-layout? parsed-layout)))))

(defn parse-layout-spec
  "Parses a vector layout spec into {:layout <ir> ...options}."
  [row-layout? [layout-string & options]]
  (let [option-map (parse-options! options
                                   {:type :invalid-layout-spec
                                    :layout-string layout-string})]
    (merge {:layout (parse-layout-string row-layout? layout-string)}
           option-map)))
