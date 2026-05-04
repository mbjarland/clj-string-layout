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

(deftest unicode-width-helpers
  (is (= 2 (width/unicode-width "界")))
  (is (= 2 (width/unicode-width "\uFF21")))
  (is (= 1 (width/unicode-width (str "a" "\u0301"))))
  (is (= 0 (width/codepoint-width 0x0301)))
  (is (= 2 (width/codepoint-width (int \界)))))

(deftest unicode-aware-layout-widths
  (is (= ["界 x "
          "ab yy"]
         (layout [["界" "x"]
                  ["ab" "yy"]]
                 {:display-width width/unicode-width
                  :layout {:cols ["[R] [L]"]}}))))

(deftest terminal-width-strips-ansi-and-counts-wide-glyphs
  (let [red-wide (str "\u001B[31m" "界" "\u001B[0m")]
    (is (= 2 (width/terminal-width red-wide)))
    (is (= [(str red-wide " x ")
            "ab yy"]
           (layout [[red-wide "x"]
                    ["ab" "yy"]]
                   {:display-width width/terminal-width
                    :layout {:cols ["[R] [L]"]}})))))
