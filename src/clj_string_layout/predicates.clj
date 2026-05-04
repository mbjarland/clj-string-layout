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

(def first-row? first-index?)
(def not-first-row? (complement first-index?))
(def second-row? second-index?)
(def last-row? last-index?)
(def not-last-row? (complement last-index?))
(def interior-row? interior-index?)
(def not-interior-row? (complement interior-index?))
(def all-rows? all-indexes?)

(def first-col? first-index?)
(def not-first-col? (complement first-index?))
(def second-col? second-index?)
(def last-col? last-index?)
(def not-last-col? (complement last-index?))
(def interior-col? interior-index?)
(def not-interior-col? (complement interior-index?))
(def all-cols? all-indexes?)
