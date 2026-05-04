(ns clj-string-layout.core-test
  (:require [clj-string-layout.core :refer [explain-layout layout layout-str
                                            parse-layout]]
            [clj-string-layout.impl.parser :as parser]
            [clj-string-layout.impl.render :as render]
            [clj-string-layout.layout :as layouts]
            [clj-string-layout.predicates :as pred]
            [clj-string-layout.presets :as presets]
            [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]])
  (:import [java.util Random]))

(defn- column
  ([align]
   (column align 0))
  ([align fills]
   {:type :column :align align :fills fills :fill-widths []}))

(defn- text [value]
  {:type :text :value value})

(def fill {:type :fill})

(defn- sample-display-width [value]
  (reduce + (map #(if (= \界 %) 2 1) value)))

(defn- random-word [^Random rng]
  (apply str
         (repeatedly (.nextInt rng 8)
                     #(char (+ (int \a) (.nextInt rng 26))))))

(defn- random-rows [^Random rng row-count col-count]
  (mapv (fn [_]
          (mapv (fn [_] (random-word rng))
                (range col-count)))
        (range row-count)))

(defn- raw-cell [pieces col-idx]
  (nth pieces (* 2 col-idx)))

(deftest invalid-layout-strings
  (are [row-layout? layout-string]
       (thrown-with-msg? clojure.lang.ExceptionInfo
                         #"Error parsing layout string"
                         (parser/parse-layout-spec row-layout? [layout-string]))
    false "[]"
    false "[x]"
    false "[x"
    false "x]"
    false "[x][c][r]"
    false "[l][x][r]"
    false "[l][c][x]"
    false "{ [c] "
    false " [c] }"))

(deftest parse-layout-strings-to-ir
  (are [layout-string row? expected-layout]
       (= {:layout expected-layout} (parser/parse-layout-spec row? [layout-string]))
    "[L]" false [(column :l)]
    "[C]" false [(column :c)]
    "[R]" false [(column :r)]
    "[l]" false [(column :l)]
    "[c]" false [(column :c)]
    "[r]" false [(column :r)]
    "f[L]f" false [fill (column :l) fill]
    "[fLf]" false [(column :l 2)]
    "[L]" true [(column \L)]
    "[*]" true [(column \*)]
    "a[L]b" false [(text "a") (column :l) (text "b")]
    "[L][C][R]" false [(column :l) (column :c) (column :r)]
    "|[L]|[C]|" false [(text "|") (column :l) (text "|") (column :c) (text "|")]
    "-[l]-f-[r]-" false [(text "-") (column :l) (text "-") fill (text "-") (column :r) (text "-")]
    "|f[fl]f|f[rf]f|" false [(text "|") fill (column :l 1) fill (text "|") fill (column :r 1) fill (text "|")]))

(deftest escaped-layout-literals
  (is (= [(text "f") (column :l) (text "F")]
         (parse-layout "\\f[L]\\F")))
  (is (= [(text "{") (column :l) (text "}")]
         (parse-layout "\\{[L]\\}")))
  (is (= ["faF"]
         (layout "a" {:layout {:cols ["\\f[L]\\F"]}}))))

(deftest layout-diagnostics
  (is (= [(column :l)] (parse-layout "[L]")))
  (is (:valid? (explain-layout "[L]")))
  (is (false? (:valid? (explain-layout "[x]"))))
  (is (= :layout-parse-error (-> (explain-layout "[x]") :data :type))))

(deftest calculate-fills-test
  (are [fill-width fill-count fill-chars expected]
       (= expected (#'render/calculate-fills fill-width fill-count fill-chars))
    0 1 [\*] [""]
    1 1 [\*] ["*"]
    2 1 [\*] ["**"]
    2 2 [\* \+] ["*" "+"]
    3 2 [\* \+] ["*" "++"]
    4 2 [\* \+] ["**" "++"]
    3 3 [\* \+ \-] ["*" "+" "-"]
    4 3 [\* \+ \-] ["*" "+" "--"]
    5 3 [\* \+ \-] ["*" "++" "--"]
    6 3 [\* \+ \-] ["**" "++" "--"]
    7 3 [\* \+ \-] ["**" "++" "---"]
    8 3 [\* \+ \-] ["**" "+++" "---"]
    9 3 [\* \+ \-] ["***" "+++" "---"]
    10 3 [\* \+ \-] ["***" "+++" "----"]
    7 2 [\* \+] ["***" "++++"]
    20 6 [\1 \2 \3 \4 \5 \6] ["111" "222" "3333" "444" "555" "6666"]))

(deftest simple-layout-expressions
  (are [rows align-c split-c col-layout width expected]
       (= expected
          (layout rows {:word-split-char split-c
                        :align-char align-c
                        :width width
                        :layout {:cols col-layout}}))
    "a b" \space \space ["[L] [R]"] 20 ["a b"]
    "a b" \space \space ["[L] [R]"] 0 ["a b"]
    "a b" \space \space ["[R] [L]"] 20 ["a b"]
    "a b" \space \space ["[R] [L]"] 0 ["a b"]
    "a b" \space \space ["[R]f[L]"] 20 ["a                  b"]
    "a b" \space \space ["f[R] [L]f"] 20 ["        a b         "]
    "a b\naa bb" \space \space ["[L] [R]"] 20 ["a   b" "aa bb"]
    "a b\naa bb" \space \space ["[L] [R]"] 0 ["a   b" "aa bb"]
    "a b\naa bb" \space \space ["[L]  [R]"] 20 ["a    b" "aa  bb"]
    "a b\naa bb" \space \space ["[L]  [R]"] 0 ["a    b" "aa  bb"]
    "a b" \space \space ["[L]f[R]"] 20 ["a                  b"]
    "a b" \space \space ["[L]f[R]"] 0 ["ab"]
    "a b\naa bb" \space \space ["[L]f[R]"] 10 ["a        b" "aa      bb"]
    "a b\naa bb" \space \space ["[L]f[R]"] 0 ["a  b" "aabb"]
    "a b\naa bb" \space \space ["f[R] [R]"] 10 ["      a  b" "     aa bb"]
    "a b\naa bb" \space \space ["f[R] [R]"] 0 [" a  b" "aa bb"]
    "a b\naa bb" \space \space ["[R] [R]f"] 10 [" a  b     " "aa bb     "]
    "a b\naa bb" \space \space ["[R] [R]f"] 0 [" a  b" "aa bb"]
    "a b\naa bb" \space \space ["[l]-f-f-[r]"] 0 ["a --- b" "aa---bb"]
    "a*b\naa*bb" \* \* ["[l]*f*f*[r]"] 0 ["a*****b" "aa***bb"]))

(deftest custom-split-chars-are-treated-literally
  (is (= ["a   b" "aa bb"]
         (layout "a|b.aa|bb"
                 {:word-split-char \|
                  :row-split-char \.
                  :layout {:cols ["[L] [R]"]}}))))

(deftest raw-layout-output
  (is (= [["| " "a" " | " "b" " |"]]
         (layout "a b" {:raw? true
                        :layout {:cols ["| [L] | [R] |"]}}))))

(deftest string-layout-output
  (is (= "a   b\naa bb"
          (layout-str "a b\naa bb" {:layout {:cols ["[L] [R]"]}}))))

(deftest randomized-layout-invariants
  (let [rng (Random. 20260504)]
    (dotimes [case-idx 100]
      (let [col-count (inc (.nextInt rng 5))
            row-count (inc (.nextInt rng 10))
            rows (random-rows rng row-count col-count)
            col-widths (apply mapv #(apply max (map count %&)) rows)
            layout-string (str/join "|" (repeat col-count "[L]"))
            rendered (layout rows {:raw? true
                                   :layout {:cols [layout-string]}})]
        (is (= row-count (count rendered))
            (pr-str {:case case-idx :rows rows}))
        (doseq [[row-idx [row pieces]] (map-indexed vector (map vector rows rendered))]
          (is (= (dec (* 2 col-count)) (count pieces))
              (pr-str {:case case-idx :row row-idx :pieces pieces}))
          (doseq [col-idx (range col-count)]
            (let [value (nth row col-idx)
                  cell (raw-cell pieces col-idx)
                  width (nth col-widths col-idx)]
              (is (= width (count cell))
                  (pr-str {:case case-idx :row row-idx :col col-idx :cell cell}))
              (is (str/starts-with? cell value)
                  (pr-str {:case case-idx :row row-idx :col col-idx :cell cell}))))
          (doseq [piece (map #(nth pieces (inc (* 2 %)))
                             (range (dec col-count)))]
            (is (= "|" piece)
                (pr-str {:case case-idx :row row-idx :pieces pieces}))))))))

(deftest custom-display-width
  (is (= ["界 x "
          "ab yy"]
         (layout [["界" "x"] ["ab" "yy"]]
                 {:display-width sample-display-width
                  :layout {:cols ["[R] [L]"]}})))
  (is (= ["界   x"]
         (layout [["界" "x"]]
                 {:display-width sample-display-width
                  :width 6
                  :layout {:cols ["[L]f[R]"]}}))))

(deftest left-justified-column-layout
  (is (= [" Alice, why     is    "
          " a      raven   like  "
          " a      writing desk? "]
         (layout (str "Alice, why is" \newline
                      "a raven like" \newline
                      "a writing desk?")
                 {:width 40
                  :layout {:cols [" [L] [L] [L] "]}}))))

(deftest box-layout-left-center-and-right
  (let [data (str "Alice, why is" \newline
                  "a raven like" \newline
                  "a writing desk?")]
    (is (= ["╔════════════╤═════════════╤═══════════╗"
            "║ Alice,     │ why         │ is        ║"
            "╟────────────┼─────────────┼───────────╢"
            "║ a          │ raven       │ like      ║"
            "╟────────────┼─────────────┼───────────╢"
            "║ a          │ writing     │ desk?     ║"
            "╚════════════╧═════════════╧═══════════╝"]
           (layout data
                   {:width 40
                    :layout {:cols ["║{ [Lf] │} [Lf] ║" :apply-for [layouts/all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for layouts/first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for layouts/interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for layouts/last-row?]]}})))
    (is (= ["╔════════════╤═════════════╤═══════════╗"
            "║   Alice,   │     why     │     is    ║"
            "╟────────────┼─────────────┼───────────╢"
            "║      a     │    raven    │    like   ║"
            "╟────────────┼─────────────┼───────────╢"
            "║      a     │   writing   │   desk?   ║"
            "╚════════════╧═════════════╧═══════════╝"]
           (layout data
                   {:width 40
                    :layout {:cols ["║{ [Cf] │} [Cf] ║" :apply-for [layouts/all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for layouts/first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for layouts/interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for layouts/last-row?]]}})))
    (is (= ["╔════════════╤═════════════╤═══════════╗"
            "║     Alice, │         why │        is ║"
            "╟────────────┼─────────────┼───────────╢"
            "║          a │       raven │      like ║"
            "╟────────────┼─────────────┼───────────╢"
            "║          a │     writing │     desk? ║"
            "╚════════════╧═════════════╧═══════════╝"]
           (layout data
                   {:width 40
                    :layout {:cols ["║{ [Rf] │} [Rf] ║" :apply-for [layouts/all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for layouts/first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for layouts/interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for layouts/last-row?]]}})))))

(deftest built-in-html-layout
  (is (= ["<table>"
          "  <tr><td>Alice</td><td>why</td></tr>"
          "  <tr><td>a</td><td>raven</td></tr>"
          "</table>"]
         (layout "Alice why\na raven" presets/layout-html-table))))

(deftest built-in-layouts-and-predicates
  (is (layouts/first-row? [0 3]))
  (is (pred/first-row? [0 3]))
  (is (layouts/all-cols? [2 4]))
  (is (pred/all-cols? [2 4]))
  (is (= ["┌───┬───┐"
          "│ a │ b │"
          "└───┴───┘"]
         (layout "a b" presets/layout-ascii-box-center)))
  (is (= presets/layout-ascii-box-center layouts/layout-ascii-box-center)))

(deftest validation-errors
  (testing "missing column layout"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                             (layout "a b" {})
                             (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "repeat group without predicate"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                             (layout "a b" {:layout {:cols ["{[L]} [R]"]}})
                             (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "repeat predicate count mismatch"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                             (layout "a b" {:layout {:cols ["{[L]} {[R]}"
                                                      :repeat-for [layouts/all-cols?]]}})
                             (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "odd layout option count"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                              (layout "a b" {:layout {:cols ["[L]" :repeat-for]}})
                              (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "invalid display width function"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                             (layout "a" {:display-width 1
                                          :layout {:cols ["[L]"]}})
                             (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "column count mismatch"
    (is (= :invalid-layout-config
           (:type (ex-data (try
                             (layout "a b c" {:layout {:cols ["[L] [R]"]}})
                             (catch clojure.lang.ExceptionInfo e e)))))))
  (testing "non-string row values"
    (is (= :invalid-rows
           (:type (ex-data (try
                             (layout [["a" 1]] {:layout {:cols ["[L] [R]"]}})
                             (catch clojure.lang.ExceptionInfo e e))))))))
