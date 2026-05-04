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
