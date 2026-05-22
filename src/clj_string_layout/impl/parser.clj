(ns ^:no-doc clj-string-layout.impl.parser
  "Hand-rolled recursive descent parser for the clj-string-layout DSL.

  This used to be an Instaparse grammar; the parser was rewritten to a plain
  scanner so the library has no third-party dependencies and can be required
  directly under Babashka (Instaparse uses deftype patterns SCI does not
  support). The parser emits the same internal layout IR as before:

    {:type :column :align <keyword-or-char> :fills <n> :fill-widths []}
    {:type :text :value <string>}
    {:type :fill}
    {:type :repeat :layout [<entries>...]}"
  (:require [clj-string-layout.impl.error :refer [layout-error parse-options!]]))

(defn- text-entry [^String value]
  {:type :text :value value})

(def ^:private fill-entry
  {:type :fill})

(defn- column-entry [align fills]
  {:type :column :align align :fills fills :fill-widths []})

(defn- repeat-entry [layout]
  {:type :repeat :layout layout})

(defn- fill-char? [^Character ch]
  (or (= ch \f) (= ch \F)))

(defn- col-align [^Character ch row-layout?]
  (if row-layout?
    ch
    (case (Character/toLowerCase ch)
      \l :l
      \c :c
      \r :r
      \v :v
      nil)))

(defn- parse-error! [layout-string idx message]
  (layout-error (str "Error parsing layout string '" layout-string
                     "' at position " idx ": " message)
                {:type :layout-parse-error
                 :layout-string layout-string
                 :idx idx
                 :message message}))

(defn- parse-column
  "s[idx] is '['. Returns [column-entry next-idx]."
  [^String s _n idx row-layout?]
  (let [close (.indexOf s "]" (unchecked-inc idx))]
    (when (neg? close)
      (parse-error! s idx "unclosed '['"))
    (let [content (.substring s (unchecked-inc idx) close)
          len (.length content)]
      (when (zero? len)
        (parse-error! s idx "empty column marker '[]'"))
      (let [c0 (.charAt content 0)
            fills-before (if (fill-char? c0) 1 0)
            pos1 (long fills-before)]
        (when (>= pos1 len)
          (parse-error! s idx "column marker missing alignment"))
        (let [align-char (.charAt content (int pos1))
              pos2 (unchecked-inc pos1)
              fills-after (if (and (< pos2 len)
                                   (fill-char? (.charAt content (int pos2))))
                            1 0)
              pos3 (unchecked-add pos2 fills-after)]
          (when (< pos3 len)
            (parse-error! s idx "trailing characters in column marker"))
          (let [align (col-align align-char row-layout?)]
            (when (nil? align)
              (parse-error! s idx (str "invalid column alignment '"
                                       align-char "'")))
            [(column-entry align (+ fills-before fills-after))
             (unchecked-inc close)]))))))

(defn- parse-delim
  "Parse delim text starting at idx. Returns [entries next-idx].

  Stops at the first '[', '{', '}' or end-of-string. Splits the run into one
  entry per atom (escape, fill, or contiguous literal text chunk) so escaped
  characters stay distinguishable from surrounding text — matching the
  Instaparse-era IR shape."
  [^String s ^long n ^long idx]
  (let [sb (StringBuilder.)]
    (loop [i idx
           entries []]
      (let [flush (fn [acc]
                    (if (pos? (.length sb))
                      (let [v (.toString sb)]
                        (.setLength sb 0)
                        (conj acc (text-entry v)))
                      acc))]
        (cond
          (>= i n) [(flush entries) i]
          :else
          (let [c (.charAt s (int i))]
            (cond
              (= c \\)
              (let [j (unchecked-inc i)]
                (when (>= j n)
                  (parse-error! s i "trailing '\\' in layout string"))
                (let [entries (flush entries)]
                  (recur (unchecked-add i 2)
                         (conj entries
                               (text-entry (str (.charAt s (int j))))))))

              (fill-char? c)
              (recur (unchecked-inc i) (conj (flush entries) fill-entry))

              (or (= c \[) (= c \{) (= c \}))
              [(flush entries) i]

              (= c \])
              (parse-error! s i "unmatched ']'")

              :else
              (do (.append sb c)
                  (recur (unchecked-inc i) entries)))))))))

(declare parse-layout-content)

(defn- parse-repeat
  "s[idx] is '{'. Returns [repeat-entry next-idx]."
  [^String s n idx row-layout?]
  (let [[content next-idx] (parse-layout-content s n (unchecked-inc idx) row-layout? true)]
    [(repeat-entry content) next-idx]))

(defn- parse-layout-content
  "Parse a sequence of (column | repeat | delim) entries.

  Used at top level (in-repeat? false) and inside a repeat group
  (in-repeat? true). Returns [entries next-idx]; for the in-repeat? case
  next-idx is one past the matching '}'."
  [^String s n idx row-layout? in-repeat?]
  (loop [i idx
         out []]
    (cond
      (>= i n)
      (if in-repeat?
        (parse-error! s idx "unclosed '{'")
        [out i])

      :else
      (let [c (.charAt s (int i))]
        (cond
          (= c \})
          (if in-repeat?
            [out (unchecked-inc i)]
            (parse-error! s i "unmatched '}'"))

          (= c \[)
          (let [[col next-i] (parse-column s n i row-layout?)]
            (recur next-i (conj out col)))

          (= c \{)
          (if in-repeat?
            (parse-error! s i "nested '{' inside repeat group")
            (let [[rep next-i] (parse-repeat s n i row-layout?)]
              (recur next-i (conj out rep))))

          :else
          (let [[delim-entries next-i] (parse-delim s n i)]
            (recur next-i (into out delim-entries))))))))

(defn parse-layout-string
  "Parses a column or row layout string into the internal layout IR."
  [row-layout? layout-string]
  (when-not (string? layout-string)
    (layout-error "Layout specification must start with a layout string"
                  {:type :invalid-layout-spec
                   :layout-string layout-string}))
  (let [^String s layout-string
        n (.length s)
        [entries _] (parse-layout-content s n 0 row-layout? false)]
    entries))

(defn parse-layout-spec
  "Parses a vector layout spec into {:layout <ir> ...options}."
  [row-layout? [layout-string & options]]
  (let [option-map (parse-options! options
                                   {:type :invalid-layout-spec
                                    :layout-string layout-string})]
    (merge {:layout (parse-layout-string row-layout? layout-string)}
           option-map)))
