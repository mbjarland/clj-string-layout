(ns clj-string-layout.core-test
  (:require [clojure.test :refer [are deftest is]]
            [clj-string-layout.core :refer [calculate-fills f-expand-fills
                                            f-parse-layout-string layout]]
            [clj-string-layout.layout :refer [all-cols? first-row?
                                              interior-row? last-row?]]))

(deftest invalid-layout-strings
  (are [row-layout? layout-string]
       (thrown? Exception ((f-parse-layout-string row-layout?) [layout-string]))
    false "[]"
    false "[x]"
    false "[x"
    false "x]"
    false "[x][c][r]"
    false "[l][x][r]"
    false "[l][c][x]"
    false "{ [c] "
    false " [c] }"))

(deftest parse-col-and-row-layout-strings
  (are [layout-string row? expected-layout]
       (= {:layout expected-layout} ((f-parse-layout-string row?) [layout-string]))
    "[L]" false [{:col [{:align :l}]}]
    "[C]" false [{:col [{:align :c}]}]
    "[R]" false [{:col [{:align :r}]}]
    "[l]" false [{:col [{:align :l}]}]
    "[c]" false [{:col [{:align :c}]}]
    "[r]" false [{:col [{:align :r}]}]
    "f[L]f" false [{:del [:f]} {:col [{:align :l}]} {:del [:f]}]
    "[fLf]" false [{:col [:f {:align :l} :f]}]
    "[L]" true [{:col [{:align \L}]}]
    "[*]" true [{:col [{:align \*}]}]
    "a[L]b" false [{:del ["a"]} {:col [{:align :l}]} {:del ["b"]}]
    "[L][C][R]" false [{:col [{:align :l}]} {:col [{:align :c}]} {:col [{:align :r}]}]
    "|[L]|[C]|" false [{:del ["|"]} {:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]} {:del ["|"]}]
    "[L]|[C]|" false [{:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]} {:del ["|"]}]
    "|[L]|[C]" false [{:del ["|"]} {:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]}]
    "[L]|[C]" false [{:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]}]
    "|[L][C]|" false [{:del ["|"]} {:col [{:align :l}]} {:col [{:align :c}]} {:del ["|"]}]
    "-[l]-f-[r]-" false [{:del ["-"]} {:col [{:align :l}]} {:del ["-" :f "-"]} {:col [{:align :r}]} {:del ["-"]}]
    "--[l]--f--[r]--" false [{:del ["--"]} {:col [{:align :l}]} {:del ["--" :f "--"]} {:col [{:align :r}]} {:del ["--"]}]
    "--[l]f--f--f[r]--" false [{:del ["--"]} {:col [{:align :l}]} {:del [:f "--" :f "--" :f]} {:col [{:align :r}]} {:del ["--"]}]
    "|f[fl]f|f[rf]f|" false [{:del ["|" :f]} {:col [:f {:align :l}]} {:del [:f "|" :f]} {:col [{:align :r} :f]} {:del [:f "|"]}]))

(deftest calculate-fills-test
  (are [fill-width fill-count fill-chars expected]
       (= expected (calculate-fills fill-width fill-count fill-chars))
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

(deftest expand-fills-test
  (are [layout width col-widths fill-chars expected]
       (= expected ((f-expand-fills width col-widths fill-chars) layout))
    [] 5 [0 0] [\*] []
    [{:del [" "]}] 5 [0 0] [\*] [{:del " "}]
    [{:del ["-"]}] 5 [0 0] [\*] [{:del "-"}]
    [{:del [:f]}] 5 [0 0] [\*] [{:del "*****"}]
    [{:del [:f]}] 5 [1 1] [\*] [{:del "***"}]
    [{:del [" "]} {:del [:f]}] 5 [0 0] [\*] [{:del " "} {:del "****"}]
    [{:del [" " :f]}] 5 [0 0] [\*] [{:del " ****"}]
    [{:del [" " :f]}] 5 [1 1] [\*] [{:del " **"}]
    [{:del [:f " "]}] 5 [0 0] [\*] [{:del "**** "}]
    [{:del [:f " "]}] 5 [1 1] [\* \*] [{:del "** "}]
    [{:del [:f " " :f]}] 5 [1 1] [\* \*] [{:del "* *"}]
    [{:del [:f " " :f]}] 9 [1 1] [\* \*] [{:del "*** ***"}]
    [{:del [:f " " :f]}] 10 [1 1] [\* \*] [{:del "*** ****"}]))

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
    "a b\naa bb" \space \space ["[l]-f-f-[r]"] 0 ["a --- b" "aa---bb"]
    "a*b\naa*bb" \* \* ["[l]*f*f*[r]"] 0 ["a*****b" "aa***bb"]))

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
                    :layout {:cols ["║{ [Lf] │} [Lf] ║" :apply-for [all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})))
    (is (= ["╔════════════╤═════════════╤═══════════╗"
            "║   Alice,   │     why     │     is    ║"
            "╟────────────┼─────────────┼───────────╢"
            "║      a     │    raven    │    like   ║"
            "╟────────────┼─────────────┼───────────╢"
            "║      a     │   writing   │   desk?   ║"
            "╚════════════╧═════════════╧═══════════╝"]
           (layout data
                   {:width 40
                    :layout {:cols ["║{ [Cf] │} [Cf] ║" :apply-for [all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})))
    (is (= ["╔════════════╤═════════════╤═══════════╗"
            "║     Alice, │         why │        is ║"
            "╟────────────┼─────────────┼───────────╢"
            "║          a │       raven │      like ║"
            "╟────────────┼─────────────┼───────────╢"
            "║          a │     writing │     desk? ║"
            "╚════════════╧═════════════╧═══════════╝"]
           (layout data
                   {:width 40
                    :layout {:cols ["║{ [Rf] │} [Rf] ║" :apply-for [all-cols?]]
                             :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                                    ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                                    ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})))))
