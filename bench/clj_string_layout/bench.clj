(ns clj-string-layout.bench
  "Repeatable benchmarks split between parse-time and render-time so each
  change can be evaluated against the path it actually touches."
  (:require [clj-string-layout.core :as core]
            [clj-string-layout.impl.parser :as parser]
            [clj-string-layout.table :as table]))

;; --- benchmark fixtures ---------------------------------------------------

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

(def parse-fixtures
  ;; Representative layout strings sampled across the DSL surface so the
  ;; benchmark exercises text runs, fills, escapes, and repeat groups.
  [["plain three columns"   "[L]  [R]  [R]"                             false]
   ["box border"             "│ [L] │ [R] │ [R] │"                       false]
   ["markdown row"           "| [L] | [R] | [R] |"                       false]
   ["repeat all-cols"        "│{ [L] │} [L] │"                           false]
   ["box row rule"           "├{─[─]─┼}─[─]─┤"                           true]
   ["escapes + fills"        "f\\[[L]\\]f[Rf]f"                          false]
   ["dense repeats"          "{\\[[L]\\]}{|[L]}{|\\[[L]\\]}"             false]])

;; --- timing helpers ------------------------------------------------------

(defn- elapsed-ns [f]
  (let [start (System/nanoTime)]
    (f)
    (- (System/nanoTime) start)))

(defn- median [values]
  (nth (vec (sort values)) (quot (count values) 2)))

(defn- millis [nanos] (/ nanos 1000000.0))
(defn- micros [nanos] (/ nanos 1000.0))

(defn- bench
  "Run f after warming up and report min and median timings."
  [label unit f]
  (dotimes [_ 3] (f))
  (let [runs (doall (repeatedly 9 #(elapsed-ns f)))
        scale (case unit :ms millis :us micros)
        label-unit (case unit :ms "ms" :us "µs")]
    (printf "  %-42s min %8.2f %s  median %8.2f %s%n"
            label
            (scale (apply min runs)) label-unit
            (scale (median runs)) label-unit)))

;; --- benchmark groups ----------------------------------------------------

(defn- parse-once [s row?]
  (parser/parse-layout-string row? s))

(defn- bench-parse []
  (println)
  (println "PARSE (one parse per measured run, microseconds):")
  (doseq [[label s row?] parse-fixtures]
    (bench (str "parse " label) :us
           #(parse-once s row?))))

(defn- bench-parse-x1000 []
  (println)
  (println "PARSE × 1000 (1000 parses per measured run, milliseconds):")
  (doseq [[label s row?] parse-fixtures]
    (bench (str "parse " label) :ms
           #(dotimes [_ 1000] (parse-once s row?)))))

(defn- bench-render []
  (println)
  (println "RENDER (1000 rows per measured run, milliseconds):")
  (bench "table/table markdown" :ms
         #(table/table table-spec))
  (bench "core/layout plain" :ms
         #(core/layout rows plain-layout))
  (bench "core/layout-seq plain explicit widths" :ms
         #(doall (core/layout-seq rows plain-layout))))

(defn -main [& _]
  (println "clj-string-layout benchmark")
  (println "warmups: 3, measured runs: 9")
  (bench-parse)
  (bench-parse-x1000)
  (bench-render))
