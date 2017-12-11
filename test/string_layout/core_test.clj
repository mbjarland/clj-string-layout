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
  ?layout-string    ?aligns          ?spaces
  "[L]"             [:L]             ["" ""]
  "[L][C][R]"       [:L :C :R]       ["" "" "" ""]
  "|[L]|[C]|"       [:L :C]          ["|" "|" "|"]
   "[L]|[C]|"       [:L :C]          ["" "|" "|"]
  "|[L]|[C]"        [:L :C]          ["|" "|" ""]
  "[L]|[C]"         [:L :C]          ["" "|" ""]
  "|[L][C]|"        [:L :C]          ["|" "" "|"])

(tabular
  (fact "Should expands fills"
        (expand-fills ?spaces ?fill-width ?col-widths ?align-char) => ?expected-result)
  ?spaces      ?fill-width   ?col-widths ?align-char ?expected-result
  [""]         5             [0 0]       \*          [""]
  [" "]        5             [0 0]       \*          [" "]
  ["-"]        5             [0 0]       \*          ["-"]
  [:F]         5             [0 0]       \*          ["*****"]
  [:F]         5             [1 1]       \*          ["***"]
  [" " :F]     5             [0 0]       \*          [" " "****"]
  [" " :F]     5             [1 1]       \*          [" " "**"]
  [:F " "]     5             [0 0]       \*          ["****" " "]
  [:F " "]     5             [1 1]       \*          ["**" " "]
  )

(tabular
  (fact "Should correctly lay out simple expressions"
        (let [rows (mapv #(split % #" ") (split ?rows #"\n"))]
          (layout
            rows
            ?layout-string
            {:width ?width :align-char \space}) => ?expected-result))
        ?rows                    ?layout-string   ?width   ?expected-result
        "a b"                    "[L] [R]"        20       ["a b"]
        "a b"                    "[L] [R]"         0       ["a b"]
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
        "a b\naa bb"             "[R] [R]f"        0       [" a  b"  "aa bb"])
