(ns clj-string-layout.impl.config
  (:require [clj-string-layout.impl.error :refer [layout-error]]
            [clj-string-layout.impl.parser :as parser]
            [clojure.string :as str])
  (:import [java.util.regex Pattern]))

(def default-layout-config
  {:align-char \space
   :fill-char \space
   :word-split-char \space
   :row-split-char \newline
   :display-width count
   :width 80
   :raw? false})

(defn- merge-defaults [layout-config]
  (let [layout-config (merge default-layout-config layout-config)
        {:keys [align-char]} layout-config]
    (update layout-config :fill-char (fnil identity align-char))))

(defn- spec-vector? [value]
  (and (vector? value) (string? (first value))))

(defn- validate-cols! [cols]
  (when-not (spec-vector? cols)
    (layout-error "Layout config must contain [:layout :cols] as a vector"
                  {:type :invalid-layout-config
                   :path [:layout :cols]
                   :value cols})))

(defn- option-map [path options]
  (when (odd? (count options))
    (layout-error "Layout specification options must be key/value pairs"
                  {:type :invalid-layout-config
                   :path path
                   :options options}))
  (apply hash-map options))

(defn- validate-row-spec! [idx row-spec]
  (when-not (spec-vector? row-spec)
    (layout-error "Row layout specs must be vectors starting with a string"
                  {:type :invalid-layout-config
                   :path [:layout :rows idx]
                   :value row-spec}))
  (let [options (option-map [:layout :rows idx] (rest row-spec))]
    (when-not (ifn? (:apply-for options))
      (layout-error "Row layout specs must include an :apply-for predicate"
                    {:type :invalid-layout-config
                     :path [:layout :rows idx :apply-for]
                     :value (:apply-for options)}))))

(defn- validate-raw-config! [layout-config]
  (when-not (map? layout-config)
    (layout-error "Layout config must be a map"
                  {:type :invalid-layout-config
                   :value layout-config}))
  (validate-cols! (get-in layout-config [:layout :cols]))
  (when-not (ifn? (:display-width layout-config))
    (layout-error "Layout :display-width must be a function"
                  {:type :invalid-layout-config
                   :path [:display-width]
                   :value (:display-width layout-config)}))
  (doseq [[idx row-spec] (map-indexed vector (get-in layout-config [:layout :rows]))]
    (validate-row-spec! idx row-spec)))

(defn- repeat-entry? [entry]
  (= :repeat (:type entry)))

(defn- column-entry? [entry]
  (= :column (:type entry)))

(defn- repeat-count [layout]
  (count (filter repeat-entry? layout)))

(defn- repeat-column-count [repeat-entry]
  (count (filter column-entry? (:layout repeat-entry))))

(defn- layout-options [spec]
  (option-map [:layout :cols] (rest spec)))

(defn- repeat-predicates [layout-config]
  (let [options (layout-options (get-in layout-config [:layout :cols]))
        predicates (if (contains? options :repeat-for)
                     (:repeat-for options)
                     (:apply-for options))]
    (cond
      (nil? predicates) []
      (sequential? predicates) predicates
      :else [predicates])))

(defn- validate-predicates! [predicates path]
  (doseq [[idx predicate] (map-indexed vector predicates)]
    (when-not (ifn? predicate)
      (layout-error "Repeat predicates must be functions"
                    {:type :invalid-layout-config
                     :path (conj path idx)
                     :value predicate}))))

(defn- validate-repeat-shapes! [spec path]
  (doseq [[idx entry] (map-indexed vector (:layout spec))
          :when (repeat-entry? entry)]
    (when (zero? (repeat-column-count entry))
      (layout-error "Repeat groups must contain at least one column marker"
                    {:type :invalid-layout-config
                     :path (conj path :layout idx)
                     :entry entry}))))

(defn- attach-repeat-predicates [spec predicates path]
  (let [repeat-count (repeat-count (:layout spec))]
    (validate-repeat-shapes! spec path)
    (cond
      (zero? repeat-count) spec

      (empty? predicates)
      (layout-error "Repeat groups require :repeat-for predicates on :layout :cols"
                    {:type :invalid-layout-config
                     :path path
                     :repeat-count repeat-count})

      (not= repeat-count (count predicates))
      (layout-error "Repeat group count must match repeat predicate count"
                    {:type :invalid-layout-config
                     :path path
                     :repeat-count repeat-count
                     :predicate-count (count predicates)})

      :else
      (first
        (reduce
          (fn [[spec predicates] entry]
            (if (repeat-entry? entry)
              [(update spec :layout conj (assoc entry :apply-for (first predicates)))
               (next predicates)]
              [(update spec :layout conj entry) predicates]))
          [(assoc spec :layout []) predicates]
          (:layout spec))))))

(defn compile-layout-config [layout-config]
  (let [layout-config (merge-defaults layout-config)
        _ (validate-raw-config! layout-config)
        predicates (vec (repeat-predicates layout-config))
        _ (validate-predicates! predicates [:layout :cols :repeat-for])
        col-spec (-> (parser/parse-layout-spec false
                                               (get-in layout-config [:layout :cols]))
                     (attach-repeat-predicates predicates [:layout :cols])
                     (dissoc :apply-for :repeat-for))
        row-specs (mapv #(-> (parser/parse-layout-spec true %)
                             (attach-repeat-predicates predicates [:layout :rows]))
                        (get-in layout-config [:layout :rows]))]
    (cond-> (assoc-in layout-config [:layout :cols] col-spec)
      (seq row-specs) (assoc-in [:layout :rows] row-specs))))

(defn- split-pattern [split-char]
  (re-pattern (Pattern/quote (str split-char))))

(defn- normalize-row-lens [col-count rows]
  (mapv #(into [] (take col-count (concat % (repeat "")))) rows))

(defn normalize-rows [layout-config rows]
  (let [rows (if (string? rows)
               (mapv #(str/split % (split-pattern (:word-split-char layout-config)))
                     (str/split rows (split-pattern (:row-split-char layout-config))))
               rows)]
    (when-not (and (sequential? rows) (seq rows))
      (layout-error "Rows must contain at least one row"
                    {:type :invalid-rows
                     :value rows}))
    (doseq [[row-idx row] (map-indexed vector rows)]
      (when-not (sequential? row)
        (layout-error "Rows must be a string or a sequence of row sequences"
                      {:type :invalid-rows
                       :path [row-idx]
                       :value row}))
      (doseq [[col-idx value] (map-indexed vector row)]
        (when-not (string? value)
          (layout-error "Row values must be strings"
                        {:type :invalid-rows
                         :path [row-idx col-idx]
                         :value value}))))
    (let [max-cols (apply max (map count rows))]
      (when (zero? max-cols)
        (layout-error "Rows must contain at least one column"
                      {:type :invalid-rows
                       :value rows}))
      (normalize-row-lens max-cols rows))))
