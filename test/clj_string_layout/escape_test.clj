(ns clj-string-layout.escape-test
  (:require [clj-string-layout.core :refer [layout]]
            [clj-string-layout.escape :as escape]
            [clj-string-layout.presets :as presets]
            [clojure.test :refer [deftest is]]))

(deftest html-escaping
  (is (= "&lt;td title=&quot;Tom &amp; &#39;Jerry&#39;&quot;&gt;"
         (escape/html "<td title=\"Tom & 'Jerry'\">")))
  (is (= ""
         (escape/html nil))))

(deftest markdown-cell-escaping
  (is (= "a\\\\b\\|c<br>d"
         (escape/markdown-cell "a\\b|c\r\nd")))
  (is (= ""
         (escape/markdown-cell nil))))

(deftest csv-cell-escaping
  (is (= "plain"
         (escape/csv-cell "plain")))
  (is (= "\"a,b\""
         (escape/csv-cell "a,b")))
  (is (= "\"a\"\"b\""
         (escape/csv-cell "a\"b")))
  (is (= "\"a\nb\""
         (escape/csv-cell "a\nb")))
  (is (= ""
         (escape/csv-cell nil))))

(deftest tsv-cell-escaping
  (is (= "a\\tb\\\\c\\nd"
         (escape/tsv-cell "a\tb\\c\r\nd")))
  (is (= ""
         (escape/tsv-cell nil))))

(deftest org-cell-escaping
  (is (= "a\\vert{}b<br>c"
         (escape/org-cell "a|b\r\nc")))
  (is (= ""
         (escape/org-cell nil))))

(deftest rst-cell-escaping
  (is (= "a\\\\b c"
         (escape/rst-cell "a\\b\r\nc")))
  (is (= ""
         (escape/rst-cell nil))))

(deftest log-safe-escaping
  (is (= "a\\tb\\nc\\rd\\\\e\\u001B"
         (escape/log-safe (str "a\tb\nc\rd\\e" "\u001B"))))
  (is (= ""
         (escape/log-safe nil))))

(deftest map-cells-and-layout-integration
  (is (= [["&lt;a&gt;" "x&amp;y"]]
         (escape/map-cells escape/html [["<a>" "x&y"]])))
  (is (= [["&lt;a&gt;"] ["x&amp;y"]]
         (doall (escape/map-cell-seq escape/html [["<a>"] ["x&y"]]))))
  (is (= ["<table>"
          "  <tr><td>&lt;a&gt;</td><td>x&amp;y</td></tr>"
          "</table>"]
         (layout (escape/map-cells escape/html [["<a>" "x&y"]])
                 presets/layout-html-table))))
