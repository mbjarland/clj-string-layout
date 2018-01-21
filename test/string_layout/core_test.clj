(ns string-layout.core-test
  (:require [midje.sweet :refer :all]
            [string-layout.core :refer :all]
            [string-layout.layout :refer :all]
            [clojure.string :refer [split]]))

(tabular
  (fact "Should throw exception on invalid layout string"
        ((f-parse-layout-string ?row-layout) [?layout-string]) => (throws Exception))
  ?row-layout ?layout-string
  false       "[]"
  false       "[x]"
  false       "[x"
  false       "x]"
  false       "[x][c][r]"
  false       "[l][x][r]"
  false       "[l][c][x]"
  false       "{ [c] "
  false       " [c] }"

  )


(tabular
  (fact "Should correctly parse col layout strings"
        ((f-parse-layout-string ?row) [?layout-string]) => {:layout ?layout})
  ?layout-string       ?row   ?layout
  "[L]"                false  [{:col [{:align :l}]}]
  "[C]"                false  [{:col [{:align :c}]}]
  "[R]"                false  [{:col [{:align :r}]}]
  "[l]"                false  [{:col [{:align :l}]}]
  "[c]"                false  [{:col [{:align :c}]}]
  "[r]"                false  [{:col [{:align :r}]}]
  "f[L]f"              false  [{:del [:f]} {:col [{:align :l}]} {:del [:f]}]
  "[fLf]"              false  [{:col [:f {:align :l} :f]}]
  "[L]"                true   [{:col [{:align \L}]}]
  "[*]"                true   [{:col [{:align \*}]}]
  "a[L]b"              false  [{:del ["a"]} {:col [{:align :l}]} {:del ["b"]}]
  "[L][C][R]"          false  [{:col [{:align :l}]} {:col [{:align :c}]} {:col [{:align :r}]}]
  "|[L]|[C]|"          false  [{:del ["|"]} {:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]} {:del ["|"]}]
  "[L]|[C]|"           false  [{:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]} {:del ["|"]}]
  "|[L]|[C]"           false  [{:del ["|"]} {:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]}]
  "[L]|[C]"            false  [{:col [{:align :l}]} {:del ["|"]} {:col [{:align :c}]}]
  "|[L][C]|"           false  [{:del ["|"]} {:col [{:align :l}]} {:col [{:align :c}]} {:del ["|"]}]
  "-[l]-f-[r]-"        false  [{:del ["-"]} {:col [{:align :l}]} {:del ["-" :f "-"]} {:col [{:align :r}]} {:del ["-"]}]
  "--[l]--f--[r]--"    false  [{:del ["--"]} {:col [{:align :l}]} {:del ["--" :f "--"]} {:col [{:align :r}]} {:del ["--"]}]
  "--[l]f--f--f[r]--"  false  [{:del ["--"]} {:col [{:align :l}]} {:del [:f "--" :f "--" :f]} {:col [{:align :r}]} {:del ["--"]}]
  "|f[fl]f|f[rf]f|"    false  [{:del ["|" :f]} {:col [:f {:align :l}]} {:del [:f "|" :f]} {:col [{:align :r} :f]} {:del [:f "|"]}]
  )

(tabular
  (fact "Should calculate fills correctly"
        (calculate-fills ?fill-width ?fill-count ?fill-chars) => ?expected-result)
  ?fill-width ?fill-count ?fill-chars ?expected-result
  0           1           [\*]        [""]
  1           1           [\*]        ["*"]
  2           1           [\*]        ["**"]
  2           2           [\* \+]     ["*" "+"]
  3           2           [\* \+]     ["*" "++"]
  4           2           [\* \+]     ["**" "++"]
  3           3           [\* \+ \-]  ["*" "+" "-"]
  4           3           [\* \+ \-]  ["*" "+" "--"]
  5           3           [\* \+ \-]  ["*" "++" "--"]
  6           3           [\* \+ \-]  ["**" "++" "--"]
  7           3           [\* \+ \-]  ["**" "++" "---"]
  8           3           [\* \+ \-]  ["**" "+++" "---"]
  9           3           [\* \+ \-]  ["***" "+++" "---"]
  10          3           [\* \+ \-]  ["***" "+++" "----"]
  7           2           [\* \+]     ["***" "++++"]
  20          6           [\1 \2 \3   ; split
                           \4 \5 \6]  ["111" "222" "3333" "444" "555" "6666"]
  )

(tabular
  (fact "Should expands fills correctly"
        ((f-expand-fills ?width ?col-widths ?fill-chars)  ?layout) => ?expected-result)
  ?layout              ?width ?col-widths ?fill-chars ?expected-result
  []                   5           [0 0]       [\*]        []
  [{:del [" "]}]       5           [0 0]       [\*]        [{:del " "}]
  [{:del ["-"]}]       5           [0 0]       [\*]        [{:del "-"}]
  [{:del [:f]}]        5           [0 0]       [\*]        [{:del "*****"}]
  [{:del [:f]}]        5           [1 1]       [\*]        [{:del "***"}]
  [{:del [" "]}          ;split
   {:del [:f]}]        5           [0 0]       [\*]        [{:del " "} {:del "****"}]
  [{:del [" " :f]}]    5           [0 0]       [\*]        [{:del " ****"}]
  [{:del [" " :f]}]    5           [1 1]       [\*]        [{:del " **"}]
  [{:del [:f " "]}]    5           [0 0]       [\*]        [{:del "**** "}]
  [{:del [:f " "]}]    5           [1 1]       [\* \*]     [{:del "** "}]
  [{:del [:f " " :f]}] 5           [1 1]       [\* \*]     [{:del "* *"}]
  [{:del [:f " " :f]}] 9           [1 1]       [\* \*]     [{:del "*** ***"}]
  [{:del [:f " " :f]}] 10          [1 1]       [\* \*]     [{:del "*** ****" }]

  )

(tabular
  (fact "Should correctly lay out simple expressions"
        (layout
          ?rows
          {:word-split-char ?split-c
           :align-char ?align-c
           :width ?width
           :layout {:cols ?col-layout}}) => ?expected-result)
  ?rows        ?align-c ?split-c ?col-layout     ?width ?expected-result
  "a b"        \space   \space   ["[L] [R]"]     20     ["a b"]
  "a b"        \space   \space   ["[L] [R]"]      0     ["a b"]
  "a b"        \space   \space   ["[R] [L]"]     20     ["a b"]
  "a b"        \space   \space   ["[R] [L]"]      0     ["a b"]
  "a b"        \space   \space   ["[R]f[L]"]     20     ["a                  b"]
  "a b"        \space   \space   ["f[R] [L]f"]   20     ["        a b         "]
  "a b\naa bb" \space   \space   ["[L] [R]"]     20     ["a   b" "aa bb"]
  "a b\naa bb" \space   \space   ["[L] [R]"]      0     ["a   b" "aa bb"]
  "a b\naa bb" \space   \space   ["[L]  [R]"]    20     ["a    b" "aa  bb"]
  "a b\naa bb" \space   \space   ["[L]  [R]"]     0     ["a    b" "aa  bb"]
  "a b"        \space   \space   ["[L]f[R]"]     20     ["a                  b"]
  "a b"        \space   \space   ["[L]f[R]"]      0     ["ab"]
  "a b\naa bb" \space   \space   ["[L]f[R]"]     10     ["a        b" "aa      bb"]
  "a b\naa bb" \space   \space   ["[L]f[R]"]      0     ["a  b" "aabb"]
  "a b\naa bb" \space   \space   ["f[R] [R]"]    10     ["      a  b" "     aa bb"]
  "a b\naa bb" \space   \space   ["f[R] [R]"]     0     [" a  b" "aa bb"]
  "a b\naa bb" \space   \space   ["[R] [R]f"]    10     [" a  b     " "aa bb     "]
  "a b\naa bb" \space   \space   ["[R] [R]f"]     0     [" a  b" "aa bb"]
  "a b\naa bb" \space   \space   ["[l]-f-f-[r]"]  0     ["a --- b" "aa---bb"]
  "a b\naa bb" \space   \space   ["[l]-f-f-[r]"]  0     ["a --- b" "aa---bb"]
  "a*b\naa*bb" \*       \*       ["[l]*f*f*[r]"]  0     ["a*****b" "aa***bb"]
  )


(fact "should lay out correctly with simple L justified col layout"
      (layout
        (str "Alice, why is" \newline
             "a raven like" \newline
             "a writing desk?")
        {:width 40
         :layout {:cols  [" [L] [L] [L] "]}})
      =>
      [" Alice, why     is    "
       " a      raven   like  "
       " a      writing desk? "])

(fact "Should layout correctly using L justified nc layout"
      (layout
           (str "Alice, why is" \newline
                "a raven like" \newline
                "a writing desk?")
           {:width 40
            :layout {:cols  ["║{ [Lf] │} [Lf] ║" :apply-for [all-cols?]]
                     :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                            ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                            ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})
        =>
      ["╔════════════╤═════════════╤═══════════╗"
       "║ Alice,     │ why         │ is        ║"
       "╟────────────┼─────────────┼───────────╢"
       "║ a          │ raven       │ like      ║"
       "╟────────────┼─────────────┼───────────╢"
       "║ a          │ writing     │ desk?     ║"
       "╚════════════╧═════════════╧═══════════╝"])

(fact "Should layout correctly using C justified nc layout"
      (layout
        (str "Alice, why is" \newline
             "a raven like" \newline
             "a writing desk?")
        {:width 40
         :layout {:cols  ["║{ [Cf] │} [Cf] ║" :apply-for [all-cols?]]
                  :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                         ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                         ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})
      =>
      ["╔════════════╤═════════════╤═══════════╗"
       "║   Alice,   │     why     │     is    ║"
       "╟────────────┼─────────────┼───────────╢"
       "║      a     │    raven    │    like   ║"
       "╟────────────┼─────────────┼───────────╢"
       "║      a     │   writing   │   desk?   ║"
       "╚════════════╧═════════════╧═══════════╝"])

(fact "Should layout correctly using R justified nc layout"
      (layout
        (str "Alice, why is" \newline
             "a raven like" \newline
             "a writing desk?")
        {:width 40
         :layout {:cols  ["║{ [Rf] │} [Rf] ║" :apply-for [all-cols?]]
                  :rows [["╔{═[═f]═╤}═[═f]═╗" :apply-for first-row?]
                         ["╟{─[─f]─┼}─[─f]─╢" :apply-for interior-row?]
                         ["╚{═[═f]═╧}═[═f]═╝" :apply-for last-row?]]}})
      =>
      ["╔════════════╤═════════════╤═══════════╗"
       "║     Alice, │         why │        is ║"
       "╟────────────┼─────────────┼───────────╢"
       "║          a │       raven │      like ║"
       "╟────────────┼─────────────┼───────────╢"
       "║          a │     writing │     desk? ║"
       "╚════════════╧═════════════╧═══════════╝"])