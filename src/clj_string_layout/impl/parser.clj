(ns ^:no-doc clj-string-layout.impl.parser
  "Hand-rolled recursive descent parser for the clj-string-layout DSL.

  The grammar is small enough that a regex-driven scanner reads more clearly
  than Instaparse:

    layout  := (column | repeat | delim)*
    column  := '[' fill? align fill? ']'
    repeat  := '{' (column | delim)* '}'
    delim   := (escape | fill | text)+
    escape  := '\\' .
    fill    := 'f' | 'F'
    text    := [^\\\\\\[\\]{}fF]+

  Column layouts read align as :l, :c, :r, or :v (case-insensitive). Row
  layouts keep the raw bracket character so callers can use it as the rule
  drawing glyph. Every parsed entry is one of:

    {:type :column :align <kw-or-char> :fills <0-2> :fill-widths []}
    {:type :text   :value <string>}
    {:type :fill}
    {:type :repeat :layout [<entries>...]}"
  (:require [clj-string-layout.impl.error :refer [layout-error parse-options!]]
            [clojure.string :as str]))

;; --- IR ---------------------------------------------------------------

(def ^:private fill-entry {:type :fill})

(defn- text-entry [v]
  {:type :text :value v})

(defn- column-entry [align fills]
  {:type :column :align align :fills fills :fill-widths []})

(defn- repeat-entry [body]
  {:type :repeat :layout body})

;; --- failures ---------------------------------------------------------

(defn- fail! [s idx msg]
  (layout-error (format "Error parsing layout string '%s' at position %d: %s"
                        s idx msg)
                {:type :layout-parse-error
                 :layout-string s
                 :idx idx
                 :message msg}))

;; --- column markers ---------------------------------------------------

(def ^:private column-body-re
  ;; Inside [...]: optional fill, exactly one align char, optional fill.
  #"(?i)([fF])?(.)([fF])?")

(defn- column-align [^Character ch row?]
  (if row?
    ch
    (case (Character/toLowerCase ch)
      \l :l, \c :c, \r :r, \v :v
      nil)))

(defn- read-column
  "Read '[…]' starting at idx (the '['). Returns [column-entry idx-after-]]."
  [^String s idx row?]
  (let [close (or (str/index-of s "]" (inc idx))
                  (fail! s idx "unclosed '['"))
        body  (subs s (inc idx) close)
        [_ before align-str after] (or (re-matches column-body-re body)
                                       (fail! s idx (str "invalid column marker '[" body "]'")))
        align (or (column-align (.charAt ^String align-str 0) row?)
                  (fail! s idx (str "invalid column alignment '" align-str "'")))]
    [(column-entry align (cond-> 0 before inc after inc))
     (inc close)]))

;; --- delim text -------------------------------------------------------

(def ^:private delim-token-re
  ;; One match per delim atom: escape, fill marker, or contiguous text run.
  #"\\(.)|([fF])|([^\\\[\]\{\}fF]+)")

(defn- delim-end
  "Scan forward to the first index that ends a delim: any of '[', ']', '{',
  '}', or end-of-string. Backslash escapes consume the next character so
  '\\]' stays part of the delim."
  [^String s idx]
  (let [n (.length s)
        bracket? #{\[ \] \{ \}}]
    (loop [i idx]
      (cond
        (>= i n)                  i
        (= \\ (.charAt s i))      (recur (min n (+ i 2)))
        (bracket? (.charAt s i))  i
        :else                     (recur (inc i))))))

(defn- read-delim
  "Read a delim run starting at idx. Returns [entries next-idx]."
  [s idx]
  (let [end   (delim-end s idx)
        chunk (subs s idx end)
        atom->entry (fn [[_ escape fill text]]
                      (cond
                        escape (text-entry escape)
                        fill   fill-entry
                        :else  (text-entry text)))]
    [(mapv atom->entry (re-seq delim-token-re chunk)) end]))

;; --- top-level dispatch -----------------------------------------------

(declare read-content)

(defn- read-repeat
  "Read '{…}' starting at idx (the '{'). Returns [repeat-entry idx-after-}]."
  [s idx row?]
  (let [[body next-idx] (read-content s (inc idx) row? true)]
    [(repeat-entry body) next-idx]))

(defn- read-content
  "Read entries until end-of-string or, when in-repeat? is true, a matching
  '}'. Returns [entries next-idx]."
  [^String s idx row? in-repeat?]
  (let [n (.length s)]
    (loop [i idx, out []]
      (if (>= i n)
        (if in-repeat?
          (fail! s idx "unclosed '{'")
          [out i])
        (case (.charAt s i)
          \] (fail! s i "unmatched ']'")
          \} (if in-repeat?
               [out (inc i)]
               (fail! s i "unmatched '}'"))
          \[ (let [[col j] (read-column s i row?)]
               (recur j (conj out col)))
          \{ (if in-repeat?
               (fail! s i "nested '{' inside repeat group")
               (let [[rep j] (read-repeat s i row?)]
                 (recur j (conj out rep))))
          (let [[entries j] (read-delim s i)]
            (recur j (into out entries))))))))

;; --- public API -------------------------------------------------------

(defn parse-layout-string
  "Parses a column or row layout string into the internal layout IR."
  [row? layout-string]
  (when-not (string? layout-string)
    (layout-error "Layout specification must start with a layout string"
                  {:type :invalid-layout-spec
                   :layout-string layout-string}))
  (first (read-content layout-string 0 row? false)))

(defn parse-layout-spec
  "Parses a vector layout spec into {:layout <ir> ...options}."
  [row? [layout-string & options]]
  (let [opts (parse-options! options
                             {:type :invalid-layout-spec
                              :layout-string layout-string})]
    (merge {:layout (parse-layout-string row? layout-string)}
           opts)))
