(ns clj-string-layout.table-test
  (:require [clj-string-layout.table :as table]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(deftest named-formats
  (is (contains? (table/formats) :markdown))
  (is (contains? (table/formats) :markdown-left))
  (is (contains? (table/formats) :markdown-center))
  (is (contains? (table/formats) :markdown-right))
  (is (contains? (table/formats) :box))
  (is (contains? (table/formats) :double-box))
  (is (contains? (table/formats) :unicode-box))
  (is (contains? (table/formats) :unicode-double-box))
  (is (contains? (table/formats) :ascii-box))
  (is (contains? (table/formats) :ascii-double-box))
  (is (= :left (:default-align (table/format-info :plain))))
  (is (= :unknown-table-format
         (:type (ex-data (try
                           (table/table {:format :unknown :rows [["x"]]})
                           (catch clojure.lang.ExceptionInfo e e)))))))

(deftest table-with-column-specs-and-map-rows
  (is (= ["| Name  | Qty |"
          "|:----- | ---:|"
          "| apple |  12 |"
          "| pear  |   4 |"]
         (table/table {:format :markdown
                       :columns [{:key :name :title "Name"}
                                 {:key :qty :title "Qty" :align :right}]
                       :rows [{:name "apple" :qty 12}
                              {:name "pear" :qty 4}]}))))

(deftest markdown-alignment-formats
  (is (= ["| Name | Qty |"
          "|:---- |:--- |"
          "| a    | 12  |"]
         (table/table {:format :markdown-left
                       :headers ["Name" "Qty"]
                       :rows [["a" "12"]]})))
  (is (= ["| Name | Qty |"
          "|:----:|:---:|"
          "|   a  |  12 |"]
         (table/table {:format :markdown-center
                       :headers ["Name" "Qty"]
                       :rows [["a" "12"]]})))
  (is (= ["| Name | Qty |"
          "| ----:| ---:|"
          "|    a |  12 |"]
         (table/table {:format :markdown-right
                       :headers ["Name" "Qty"]
                       :rows [["a" "12"]]}))))

(deftest column-formatting
  (is (= ["Name   Price"
          "apple  $1.50"]
         (table/table {:format :plain
                       :columns [{:key :name :title "Name"}
                                 {:key :price :title "Price"
                                  :align :right
                                  :format #(format "$%.2f" (double %))}]
                       :rows [{:name "apple" :price 1.5}]}))))

(deftest markdown-without-headers-omits-rule
  (is (= ["| a | 1 |"
          "| b | 2 |"]
         (table/table {:format :markdown
                       :rows [["a" "1"] ["b" "2"]]})))
  (is (= ["| a | 1 |"
          "| b | 2 |"]
         (table/table {:format :markdown-center
                       :rows [["a" "1"] ["b" "2"]]}))))

(deftest unknown-alignment-throws
  (is (= :invalid-table-column
         (:type (ex-data (try
                           (table/table {:format :plain
                                         :columns [{:key 0 :title "x" :align :nope}]
                                         :rows [["x"]]})
                           (catch clojure.lang.ExceptionInfo e e)))))))

(deftest overflow-policies
  (is (= ["Text"
          "a..."]
         (table/table {:format :plain
                       :columns [{:key 0 :title "Text" :width 4
                                  :overflow :ellipsis}]
                       :rows [["abcdef"]]})))
  (is (= ["Txt"
          "abc"
          "def"]
         (table/table {:format :plain
                       :columns [{:key 0 :title "Txt" :width 3
                                  :overflow :wrap}]
                       :rows [["abcdef"]]})))
  (testing "overflow errors"
    (is (= :table-cell-overflow
           (:type (ex-data (try
                             (table/table {:format :plain
                                           :columns [{:key 0 :title "Text"
                                                      :width 3
                                                      :overflow :error}]
                                           :rows [["abcdef"]]})
                             (catch clojure.lang.ExceptionInfo e e))))))))

(deftest escaped-output-formats
  (is (= ["\"a,b\""
          "\"x\"\"y\""]
         (table/table {:format :csv
                       :headers ["a,b"]
                       :rows [["x\"y"]]})))
  (is (= ["a\\tb"]
         (table/table {:format :tsv
                       :rows [["a\tb"]]})))
  (is (= ["<table>"
          "  <tr><th>&lt;Name&gt;</th></tr>"
          "  <tr><td>a&amp;b</td></tr>"
          "</table>"]
         (table/table {:format :html
                       :headers ["<Name>"]
                       :rows [["a&b"]]}))))

(deftest box-drawing-table-format
  (let [single ["┌───┬───┐"
                "│ A │ B │"
                "├───┼───┤"
                "│ x │ y │"
                "└───┴───┘"]
        double ["╔═══╦═══╗"
                "║ A ║ B ║"
                "╠═══╬═══╣"
                "║ x ║ y ║"
                "╚═══╩═══╝"]]
    (doseq [format [:box :unicode-box :ascii-box]]
      (is (= single
             (table/table {:format format
                           :headers ["A" "B"]
                           :rows [["x" "y"]]}))))
    (doseq [format [:double-box :unicode-double-box :ascii-double-box]]
      (is (= double
             (table/table {:format format
                           :headers ["A" "B"]
                           :rows [["x" "y"]]}))))))

(deftest footers-render-below-data
  (is (= ["┌───────┬─────┐"
          "│ Item  │ Qty │"
          "├───────┼─────┤"
          "│ apple │ 12  │"
          "├───────┼─────┤"
          "│ pear  │ 4   │"
          "├───────┼─────┤"
          "│ Total │ 16  │"
          "└───────┴─────┘"]
         (table/table {:format :box
                       :headers ["Item" "Qty"]
                       :rows [["apple" "12"] ["pear" "4"]]
                       :footers [["Total" "16"]]})))
  (is (= ["<table>"
          "  <tr><th>Item</th><th>Qty</th></tr>"
          "  <tr><td>apple</td><td>12</td></tr>"
          "  <tr><td>Total</td><td>16</td></tr>"
          "</table>"]
         (table/table {:format :html
                       :headers ["Item" "Qty"]
                       :rows [["apple" "12"]]
                       :footers [["Total" "16"]]}))))

(deftest title-banner
  (is (= ["   Inventory   "
          "┌───────┬─────┐"
          "│ Name  │ Qty │"
          "├───────┼─────┤"
          "│ apple │ 12  │"
          "└───────┴─────┘"]
         (table/table {:format :box
                       :title "Inventory"
                       :headers ["Name" "Qty"]
                       :rows [["apple" "12"]]})))
  (is (= ["<table>"
          "  <caption>Inventory</caption>"
          "  <tr><th>Name</th></tr>"
          "  <tr><td>x</td></tr>"
          "</table>"]
         (table/table {:format :html
                       :title "Inventory"
                       :headers ["Name"]
                       :rows [["x"]]}))))

(deftest html-honors-raw
  (is (= [["<table>"]
          ["  <tr><th>A</th></tr>"]
          ["  <tr><td>x</td></tr>"]
          ["</table>"]]
         (table/table {:format :html
                       :headers ["A"]
                       :rows [["x"]]
                       :raw? true}))))

(deftest string-and-seq-entry-points
  (let [spec {:format :ascii-grid
              :headers ["A" "B"]
              :rows [["x" "y"]]}]
    (is (= (seq (table/table spec)) (table/table-seq spec)))
    (is (= (str/join \newline (table/table spec))
           (table/table-str spec)))))
