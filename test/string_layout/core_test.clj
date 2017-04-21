(ns string-layout.core-test

  (:require [midje.sweet :refer :all]
            [midje.repl :as m]
            [string-layout.core :refer :all]))

(fact "Should throw exception on empty layout string"
      (parse-layout-string "") => (throws AssertionError))

(tabular
  (fact "Should correctly parse layout strings"
        (parse-layout-string ?layout-string) => [?aligns ?spaces])
  ?layout-string ?aligns ?spaces
  " "            []      [" "]
  "[L]"          [:L]    ["" ""])
