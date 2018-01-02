(ns string-layout.core-test
  (:require [midje.sweet :refer :all]
            [midje.repl :as m]
            [string-layout.core :refer :all]
            [clojure.string :refer [split]]))

(tabular
  (fact "Should throw exception on invalid layout string"
        (parse-layout-string ?layout-string) => (throws Exception))
  ?layout-string
  ""
  " "
  "bogus"
  "[]"
  "[x]"
  "[l][c][x]"
  )

;class StringLayoutSpecification extends Specification {
;  @Unroll
;  def "Expect rows #rows, layoutString #layoutString and width #width to result in #expectedResult"() {
;    setup:
;      def layout = new StringLayout(width: width, layoutString: layoutString)
;      def formatted = layout.layout(rows)
;
;    expect:
;      expectedResult == formatted
;      if ('fill' in layoutString ) formatted.collect { it.length() } == [width] * formatted.size()
;
;    where:
;      rows                   | layoutString   | width || expectedResult
;      "a b"                  | '[L] [R]'      | 20    || ["a b"]
;      "a b"                  | '[L] [R]'      |  0    || ["a b"]
;      "a b\naa bb"           | '[L] [R]'      | 20    || ["a   b", "aa bb"]
;      "a b\naa bb"           | '[L] [R]'      |  0    || ["a   b", "aa bb"]
;      "a b\naa bb"           | '[L]  [R]'     | 20    || ["a    b", "aa  bb"]
;      "a b\naa bb"           | '[L]  [R]'     |  0    || ["a    b", "aa  bb"]
;      "a b"                  | '[L]fill[R]'   | 20    || ["a                  b"]
;      "a b"                  | '[L]fill[R]'   |  0    || ["ab"]
;      "a b\naa bb"           | '[L]fill[R]'   | 10    || ["a        b", "aa      bb"]
;      "a b\naa bb"           | '[L]fill[R]'   |  0    || ["a  b", "aabb"]
;      "a b\naa bb"           | 'fill[R] [R]'  | 10    || ["      a  b", "     aa bb"]
;      "a b\naa bb"           | 'fill[R] [R]'  |  0    || [" a  b", "aa bb"]
;      "a b\naa bb"           | '[R] [R]fill'  | 10    || [" a  b     ", "aa bb     "]
;      "a b\naa bb"           | '[R] [R]fill'  |  0    || [" a  b", "aa bb"]
;  }


(tabular
  (fact "Should correctly parse col layout strings"
        (parse-layout-string ?layout-string false) => ?layout)
  ?layout-string      ?layout
  "[L]"               [{:delim []} {:align :L} {:delim []}]
  "[C]"               [{:delim []} {:align :C} {:delim []}]
  "[R]"               [{:delim []} {:align :R} {:delim []}]
  "[l]"               [{:delim []} {:align :L} {:delim []}]
  "[c]"               [{:delim []} {:align :C} {:delim []}]
  "[r]"               [{:delim []} {:align :R} {:delim []}]
  "a[L]b"             [{:delim ["a"]} {:align :L} {:delim ["b"]}]
  "a[C]b"             [{:delim ["a"]} {:align :C} {:delim ["b"]}]
  "a[R]b"             [{:delim ["a"]} {:align :R} {:delim ["b"]}]
  "a[l]b"             [{:delim ["a"]} {:align :L} {:delim ["b"]}]
  "a[c]b"             [{:delim ["a"]} {:align :C} {:delim ["b"]}]
  "a[r]b"             [{:delim ["a"]} {:align :R} {:delim ["b"]}]
  "[L][C][R]"         [{:delim []} {:align :L} {:delim []} {:align :C} {:delim []} {:align :R} {:delim []}]
  "|[L]|[C]|"         [{:delim ["|"]} {:align :L} {:delim ["|"]} {:align :C} {:delim ["|"]}]
   "[L]|[C]|"         [{:delim []}    {:align :L} {:delim ["|"]} {:align :C} {:delim ["|"]}]
  "|[L]|[C]"          [{:delim ["|"]} {:align :L} {:delim ["|"]} {:align :C} {:delim []}]
  "[L]|[C]"           [{:delim []}    {:align :L} {:delim ["|"]} {:align :C} {:delim []}]
  "|[L][C]|"          [{:delim ["|"]} {:align :L} {:delim []}    {:align :C} {:delim ["|"]}]
  "-[l]-f-[r]-"       [{:delim ["-"]}  {:align :L} {:delim ["-" :F "-"]}         {:align :R} {:delim ["-"]}]
  "--[l]--f--[r]--"   [{:delim ["--"]} {:align :L} {:delim ["--" :F "--"]}       {:align :R} {:delim ["--"]}]
  "--[l]f--f--f[r]--" [{:delim ["--"]} {:align :L} {:delim [:F "--" :F "--" :F]} {:align :R} {:delim ["--"]}]

  )


(tabular
  (fact "Should calculate fills correctly"
        (calculate-fills ?fill-width ?fill-count ?fill-chars) => ?expected-result)
  ?fill-width  ?fill-count   ?fill-chars ?expected-result
  0            1             [\*]                 [""]
  1            1             [\*]                 ["*"]
  2            1             [\*]                 ["**"]
  2            2             [\* \+]              ["*" "+"]
  3            2             [\* \+]              ["*" "++"]
  4            2             [\* \+]              ["**" "++"]
  3            3             [\* \+ \-]           ["*"   "+"   "-"]
  4            3             [\* \+ \-]           ["*"   "+"   "--"]
  5            3             [\* \+ \-]           ["*"   "++"  "--"]
  6            3             [\* \+ \-]           ["**"  "++"  "--"]
  7            3             [\* \+ \-]           ["**"  "++"  "---"]
  8            3             [\* \+ \-]           ["**"  "+++" "---"]
  9            3             [\* \+ \-]           ["***" "+++" "---"]
  10           3             [\* \+ \-]           ["***" "+++" "----"]
  7            2             [\* \+]              ["***" "++++"]
  20           6             [\1 \2 \3 \4 \5 \6]  ["111" "222" "3333" "444" "555" "6666"]
)

(tabular
  (fact "Should expands fills correctly"
        (expand-fills ?layout ?fill-width ?col-widths ?fill-chars) => ?expected-result)
  ?layout                         ?fill-width   ?col-widths ?fill-chars ?expected-result
  []                              5             [0 0]       [\*]        []
  [{:delim [" "]}]                5             [0 0]       [\*]        [{:delim [" "]}]
  [{:delim "-"}]                  5             [0 0]       [\*]        [{:delim "-"}]
  [{:delim [:F]}]                 5             [0 0]       [\*]        [{:delim ["*****"]}]
  [{:delim [:F]}]                 5             [1 1]       [\*]        [{:delim ["***"]}]
  [{:delim [" "]} {:delim [:F]}]  5             [0 0]       [\*]        [{:delim [" "]} {:delim ["****"]}]
  [{:delim [" " :F]}]             5             [0 0]       [\*]        [{:delim [" " "****"]}]
  [{:delim [" " :F]}]             5             [1 1]       [\*]        [{:delim [" " "**"]}]
  [{:delim [:F " "]}]             5             [0 0]       [\*]        [{:delim ["****" " "]}]
  [{:delim [:F " "]}]             5             [1 1]       [\* \*]     [{:delim ["**" " "]}]
  [{:delim [:F " " :F]}]          5             [1 1]       [\* \*]     [{:delim ["*" " " "*"]}]
  [{:delim [:F " " :F]}]          9             [1 1]       [\* \*]     [{:delim ["***" " " "***"]}]
  [{:delim [:F " " :F]}]          10            [1 1]       [\* \*]     [{:delim ["***" " " "****"]}]
  )

(tabular
  (fact "Should correctly lay out simple expressions"
    (layout
        ?rows
        {:col-layout ?col-layout :width ?width :align-char ?align-c :split-char ?split-c}) => ?expected-result)
        ?rows        ?align-c ?split-c ?col-layout  ?width ?expected-result
        "a b"        \space   \space   "[L] [R]"       20     ["a b"]
        "a b"        \space   \space   "[L] [R]"        0     ["a b"]
        "a b"        \space   \space   "[R] [L]"       20     ["a b"]
        "a b"        \space   \space   "[R] [L]"        0     ["a b"]
        "a b"        \space   \space   "[R]f[L]"       20     ["a                  b"]
        "a b"        \space   \space   "f[R] [L]f"     20     ["        a b         "]
        "a b\naa bb" \space   \space   "[L] [R]"       20     ["a   b"  "aa bb"]
        "a b\naa bb" \space   \space   "[L] [R]"        0     ["a   b"  "aa bb"]
        "a b\naa bb" \space   \space   "[L]  [R]"      20     ["a    b"  "aa  bb"]
        "a b\naa bb" \space   \space   "[L]  [R]"       0     ["a    b"  "aa  bb"]
        "a b"        \space   \space   "[L]f[R]"       20     ["a                  b"]
        "a b"        \space   \space   "[L]f[R]"        0     ["ab"]
        "a b\naa bb" \space   \space   "[L]f[R]"       10     ["a        b"  "aa      bb"]
        "a b\naa bb" \space   \space   "[L]f[R]"        0     ["a  b"  "aabb"]
        "a b\naa bb" \space   \space   "f[R] [R]"      10     ["      a  b"  "     aa bb"]
        "a b\naa bb" \space   \space   "f[R] [R]"       0     [" a  b"  "aa bb"]
        "a b\naa bb" \space   \space   "[R] [R]f"      10     [" a  b     "  "aa bb     "]
        "a b\naa bb" \space   \space   "[R] [R]f"       0     [" a  b"  "aa bb"]
        "a b\naa bb" \space   \space   "[l]-f-f-[r]"    0     ["a --- b"  "aa---bb"]
        "a b\naa bb" \space   \space   "[l]-f-f-[r]"    0     ["a --- b"  "aa---bb"]
        "a*b\naa*bb" \*       \*       "[l]*f*f*[r]"    0     ["a*****b"  "aa***bb"]
  )
