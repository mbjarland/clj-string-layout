(ns clj-string-layout.property-test
  (:require [clj-string-layout.core :refer [layout layout-str]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defn- cell-gen []
  (gen/fmap (partial apply str)
            (gen/vector gen/char-alphanumeric 0 8)))

(def rectangular-rows-gen
  (gen/bind (gen/tuple (gen/choose 1 8) (gen/choose 1 6))
            (fn [[row-count col-count]]
              (gen/fmap (fn [rows]
                          {:row-count row-count
                           :col-count col-count
                           :rows rows})
                        (gen/vector (gen/vector (cell-gen) col-count)
                                    row-count)))))

(defn- left-layout [col-count]
  (str/join "|" (repeat col-count "[L]")))

(defn- right-layout [col-count]
  (str/join "|" (repeat col-count "[R]")))

(defn- column-widths [rows]
  (apply mapv #(apply max (map count %&)) rows))

(defn- rendered-cell [pieces col-idx]
  (nth pieces (* 2 col-idx)))

(defn- check [property]
  (let [result (tc/quick-check 200 property)]
    (is (:pass? result) (pr-str result))))

(deftest left-aligned-raw-layout-preserves-column-invariants
  (check
    (prop/for-all [{:keys [row-count col-count rows]} rectangular-rows-gen]
      (let [widths (column-widths rows)
            rendered (layout rows {:raw? true
                                   :layout {:cols [(left-layout col-count)]}})]
        (and (= row-count (count rendered))
             (every? true?
                     (for [[row pieces] (map vector rows rendered)]
                       (and (= (dec (* 2 col-count)) (count pieces))
                            (every? true?
                                    (for [col-idx (range col-count)
                                          :let [cell (rendered-cell pieces col-idx)
                                                value (nth row col-idx)]]
                                      (and (= (nth widths col-idx) (count cell))
                                           (str/starts-with? cell value))))))))))))

(deftest right-aligned-raw-layout-preserves-column-invariants
  (check
    (prop/for-all [{:keys [row-count col-count rows]} rectangular-rows-gen]
      (let [widths (column-widths rows)
            rendered (layout rows {:raw? true
                                   :layout {:cols [(right-layout col-count)]}})]
        (and (= row-count (count rendered))
             (every? true?
                     (for [[row pieces] (map vector rows rendered)]
                       (every? true?
                               (for [col-idx (range col-count)
                                     :let [cell (rendered-cell pieces col-idx)
                                           value (nth row col-idx)]]
                                 (and (= (nth widths col-idx) (count cell))
                                      (str/ends-with? cell value)))))))))))

(deftest layout-str-joins-layout-lines
  (check
    (prop/for-all [{:keys [col-count rows]} rectangular-rows-gen]
      (let [config {:layout {:cols [(left-layout col-count)]}}]
        (= (str/join \newline (layout rows config))
           (layout-str rows config))))))
