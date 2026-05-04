(ns clj-string-layout.predicates)

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
  "Matches the virtual row before the first data row."
  first-index?)

(def not-first-row?
  "Matches every virtual row except the first."
  (complement first-index?))

(def second-row?
  "Matches the virtual row after the first data row."
  second-index?)

(def last-row?
  "Matches the virtual row after the last data row."
  last-index?)

(def not-last-row?
  "Matches every virtual row except the last."
  (complement last-index?))

(def interior-row?
  "Matches virtual rows between data rows."
  interior-index?)

(def not-interior-row?
  "Matches first and last virtual rows."
  (complement interior-index?))

(def all-rows?
  "Matches every virtual row."
  all-indexes?)

(def first-col?
  "Matches the first data column."
  first-index?)

(def not-first-col?
  "Matches every data column except the first."
  (complement first-index?))

(def second-col?
  "Matches the second data column."
  second-index?)

(def last-col?
  "Matches the last data column."
  last-index?)

(def not-last-col?
  "Matches every data column except the last."
  (complement last-index?))

(def interior-col?
  "Matches data columns that are neither first nor last."
  interior-index?)

(def not-interior-col?
  "Matches first and last data columns."
  (complement interior-index?))

(def all-cols?
  "Matches every data column."
  all-indexes?)
