(ns clj-string-layout.predicates
  "Reusable predicates for repeat groups and virtual row layouts.

  Predicate functions receive [idx last-idx], where idx is zero-based and
  last-idx is the last index in the current expansion. Column predicates are
  used with :repeat-for on [:layout :cols]. Row predicates are used with
  :apply-for on entries in [:layout :rows].")

(set! *warn-on-reflection* true)

(defn- first-index? [[idx _]]
  (zero? idx))

(defn- second-index? [[idx _]]
  (= idx 1))

(defn- last-index? [[idx last-idx]]
  (= idx last-idx))

(defn- interior-index? [loc]
  (and (not (first-index? loc))
       (not (last-index? loc))))

(defn- all-indexes? [_]
  true)

(def first-row?
  "Returns true for the virtual row before the first data row.

  Use with :apply-for in row layout specs. Receives [idx last-idx] and matches
  idx 0."
  first-index?)

(def not-first-row?
  "Returns true for every virtual row except the first.

  Use with :apply-for in row layout specs."
  (complement first-index?))

(def second-row?
  "Returns true for the virtual row after the first data row.

  This is commonly used for Markdown header separator rows."
  second-index?)

(def last-row?
  "Returns true for the virtual row after the last data row.

  Use with :apply-for for bottom borders and footers."
  last-index?)

(def not-last-row?
  "Returns true for every virtual row except the last."
  (complement last-index?))

(def interior-row?
  "Returns true for virtual rows between data rows.

  Does not match the first or last virtual row."
  interior-index?)

(def not-interior-row?
  "Returns true for first and last virtual rows.

  Useful when the top and bottom borders share a layout."
  (complement interior-index?))

(def all-rows?
  "Returns true for every virtual row."
  all-indexes?)

(def first-col?
  "Returns true for the first data column.

  Use with :repeat-for in column layout specs. Receives [idx last-idx] and
  matches idx 0."
  first-index?)

(def not-first-col?
  "Returns true for every data column except the first."
  (complement first-index?))

(def second-col?
  "Returns true for the second data column."
  second-index?)

(def last-col?
  "Returns true for the last data column."
  last-index?)

(def not-last-col?
  "Returns true for every data column except the last."
  (complement last-index?))

(def interior-col?
  "Returns true for data columns that are neither first nor last."
  interior-index?)

(def not-interior-col?
  "Returns true for first and last data columns."
  (complement interior-index?))

(def all-cols?
  "Returns true for every data column."
  all-indexes?)
