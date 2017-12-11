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
  (fact "Should correctly parse layout strings"
        (parse-layout-string ?layout-string) => [?aligns ?spaces])
  ?layout-string      ?aligns          ?spaces
  "[L]"               [:L]             [[] []]
  "[C]"               [:C]             [[] []]
  "[R]"               [:R]             [[] []]
  "[l]"               [:L]             [[] []]
  "[c]"               [:C]             [[] []]
  "[r]"               [:R]             [[] []]
  "a[L]b"             [:L]             [["a"] ["b"]]
  "a[C]b"             [:C]             [["a"] ["b"]]
  "a[R]b"             [:R]             [["a"] ["b"]]
  "a[l]b"             [:L]             [["a"] ["b"]]
  "a[c]b"             [:C]             [["a"] ["b"]]
  "a[r]b"             [:R]             [["a"] ["b"]]
  "[L][C][R]"         [:L :C :R]       [[] [] [] []]
  "|[L]|[C]|"         [:L :C]          [["|"] ["|"] ["|"]]
   "[L]|[C]|"         [:L :C]          [[] ["|"] ["|"]]
  "|[L]|[C]"          [:L :C]          [["|"] ["|"] []]
  "[L]|[C]"           [:L :C]          [[] ["|"] []]
  "|[L][C]|"          [:L :C]          [["|"] [] ["|"]]
  "-[l]-f-[r]-"       [:L :R]          [["-"] ["-" :F "-"] ["-"]]
  "--[l]--f--[r]--"   [:L :R]          [["--"] ["--" :F "--"] ["--"]]
  "--[l]f--f--f[r]--" [:L :R]          [["--"] [:F "--" :F "--" :F] ["--"]]

  )


(tabular
  (fact "Should calculate fills correctly"
        (calculate-fills ?fill-width ?fill-count ?align-char) => ?expected-result)
  ?fill-width  ?fill-count   ?align-char ?expected-result
  0            1             \*          [""]
  1            1             \*          ["*"]
  2            1             \*          ["**"]
  2            2             \*          ["*" "*"]
  3            2             \*          ["*" "**"]
  4            2             \*          ["**" "**"]
  3            3             \*          ["*"   "*"   "*"]
  4            3             \*          ["*"   "*"   "**"]
  5            3             \*          ["*"   "**"  "**"]
  6            3             \*          ["**"  "**"  "**"]
  7            3             \*          ["**"  "**"  "***"]
  8            3             \*          ["**"  "***" "***"]
  9            3             \*          ["***" "***" "***"]
  10           3             \*          ["***" "***" "****"]
  7            2             \*          ["***" "****"]
  20           6             \*          ["***" "***" "****" "***" "***" "****"]
)

(tabular
  (fact "Should expands fills correctly"
        (expand-fills ?spaces ?fill-width ?col-widths ?align-char) => ?expected-result)
  ?spaces            ?fill-width   ?col-widths ?align-char ?expected-result
  [[]]               5             [0 0]       \*          [[]]
  [[" "]]            5             [0 0]       \*          [[" "]]
  [["-"]]            5             [0 0]       \*          [["-"]]
  [[:F]]             5             [0 0]       \*          [["*****"]]
  [[:F]]             5             [1 1]       \*          [["***"]]
  [[" "] [:F]]       5             [0 0]       \*          [[" "] ["****"]]
  [[" "] [:F]]       5             [1 1]       \*          [[" "] ["**"]]
  [[:F] [" "]]       5             [0 0]       \*          [["****"] [" "]]
  [[:F] [" "]]       5             [1 1]       \*          [["**"] [" "]]
  [[:F] [" "] [:F]]  5             [1 1]       \*          [["*"] [" "] ["*"]]
  [[:F] [" "] [:F]]  9             [1 1]       \*          [["***"] [" "] ["***"]]
  [[:F] [" "] [:F]]  10            [1 1]       \*          [["***"] [" "] ["****"]]
  )

(tabular
  (fact "Should correctly lay out simple expressions"
    (layout
        ?rows
        ?layout-string
        {:width ?width :align-char \space}) => ?expected-result)
        ?rows                    ?layout-string   ?width   ?expected-result
        "a b"                    "[L] [R]"        20       ["a b"]
        "a b"                    "[L] [R]"         0       ["a b"]
        "a b"                    "[R] [L]"        20       ["a b"]
        "a b"                    "[R] [L]"         0       ["a b"]
        "a b"                    "[R]f[L]"        20       ["a                  b"]
        "a b"                    "f[R] [L]f"      20       ["        a b         "]
        "a b\naa bb"             "[L] [R]"        20       ["a   b"  "aa bb"]
        "a b\naa bb"             "[L] [R]"         0       ["a   b"  "aa bb"]
        "a b\naa bb"             "[L]  [R]"       20       ["a    b"  "aa  bb"]
        "a b\naa bb"             "[L]  [R]"        0       ["a    b"  "aa  bb"]
        "a b"                    "[L]f[R]"        20       ["a                  b"]
        "a b"                    "[L]f[R]"         0       ["ab"]
        "a b\naa bb"             "[L]f[R]"        10       ["a        b"  "aa      bb"]
        "a b\naa bb"             "[L]f[R]"         0       ["a  b"  "aabb"]
        "a b\naa bb"             "f[R] [R]"       10       ["      a  b"  "     aa bb"]
        "a b\naa bb"             "f[R] [R]"        0       [" a  b"  "aa bb"]
        "a b\naa bb"             "[R] [R]f"       10       [" a  b     "  "aa bb     "]
        "a b\naa bb"             "[R] [R]f"        0       [" a  b"  "aa bb"]

  )
