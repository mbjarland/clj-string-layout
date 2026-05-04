(ns clj-string-layout.table-test
  (:require [clj-string-layout.table :as table]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(deftest named-formats
  (is (contains? (table/formats) :markdown))
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

(deftest column-formatting
  (is (= ["Name   Price"
          "apple  $1.50"]
         (table/table {:format :plain
                       :columns [{:key :name :title "Name"}
                                 {:key :price :title "Price"
                                  :align :right
                                  :format #(format "$%.2f" (double %))}]
                       :rows [{:name "apple" :price 1.5}]}))))

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

(deftest string-and-seq-entry-points
  (let [spec {:format :ascii-grid
              :headers ["A" "B"]
              :rows [["x" "y"]]}]
    (is (= (seq (table/table spec)) (table/table-seq spec)))
    (is (= (str/join \newline (table/table spec))
           (table/table-str spec)))))
