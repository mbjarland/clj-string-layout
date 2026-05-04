(ns clj-string-layout.core-test
  (:require [clj-string-layout.core :refer [layout]]
            [clj-string-layout.layout :as layouts]
            [clj-string-layout.parser :as parser]
            [clj-string-layout.predicates :as pred]
            [clj-string-layout.render :as render]
            [clojure.test :refer [are deftest is testing]]))

(defn- column
  ([align]
   (column align 0))
  ([align fills]
   {:type :column :align align :fills fills :fill-widths []}))

(defn- text [value]
  {:type :text :value value})

(def fill {:type :fill})

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
         (layout "Alice why\na raven" layouts/layout-html-table))))

(deftest built-in-layouts-and-predicates
  (is (layouts/first-row? [0 3]))
  (is (pred/first-row? [0 3]))
  (is (layouts/all-cols? [2 4]))
  (is (pred/all-cols? [2 4]))
  (is (= ["┌───┬───┐"
          "│ a │ b │"
          "└───┴───┘"]
         (layout "a b" layouts/layout-ascii-box-center))))

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
