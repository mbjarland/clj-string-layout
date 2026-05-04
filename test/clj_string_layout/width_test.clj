(ns clj-string-layout.width-test
  (:require [clj-string-layout.core :refer [layout]]
            [clj-string-layout.width :as width]
            [clojure.test :refer [deftest is]]))

(def red (str "\u001B[31m" "red" "\u001B[0m"))

(deftest ansi-sequence-helpers
  (let [link (str "\u001B]8;;https://example.com\u0007"
                  "link"
                  "\u001B]8;;\u0007")]
    (is (= "red" (width/strip-ansi red)))
    (is (= 3 (width/ansi-width red)))
    (is (= "link" (width/strip-ansi link)))
    (is (= 4 (width/ansi-width link)))))

(deftest ansi-aware-layout-widths
  (is (= [(str red "  x ")
          "blue yy"]
         (layout [[red "x"]
                  ["blue" "yy"]]
                 {:display-width width/ansi-width
                  :layout {:cols ["[L] [L]"]}}))))
