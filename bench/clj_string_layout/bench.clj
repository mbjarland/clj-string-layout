(ns clj-string-layout.bench
  "Small repeatable benchmarks for common layout paths."
  (:require [clj-string-layout.core :as core]
            [clj-string-layout.table :as table]))

(def rows
  (mapv (fn [idx]
          [(str "item-" idx)
           (str idx)
           (format "$%.2f" (double (/ idx 10)))])
        (range 1000)))

(def table-spec
  {:format :markdown
   :headers ["Name" "Qty" "Price"]
   :rows rows})

(def plain-layout
  {:col-widths [9 4 7]
   :layout {:cols ["[L]  [R]  [R]"]}})

(defn- elapsed-ns [f]
  (let [start (System/nanoTime)]
    (f)
    (- (System/nanoTime) start)))

(defn- millis [nanos]
  (/ nanos 1000000.0))

(defn- median [values]
  (nth (vec (sort values)) (quot (count values) 2)))

(defn- bench [label f]
  (dotimes [_ 3]
    (f))
  (let [runs (doall (repeatedly 7 #(elapsed-ns f)))]
    (printf "%-42s min %7.2f ms  median %7.2f ms%n"
            label
            (millis (apply min runs))
            (millis (median runs)))))

(defn -main [& _]
  (println "clj-string-layout benchmark")
  (println "Rows: 1000, warmups: 3, measured runs: 7")
  (bench "table/table markdown" #(table/table table-spec))
  (bench "core/layout plain" #(core/layout rows plain-layout))
  (bench "core/layout-seq plain explicit widths" #(doall (core/layout-seq rows plain-layout))))
