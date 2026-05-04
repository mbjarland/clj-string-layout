(ns clj-string-layout.cli-test
  (:require [clj-string-layout.cli :as cli]
            [clojure.test :refer [deftest is]]))

(deftest argument-parsing
  (is (= {:input :tsv
          :format :markdown
          :headers? true
          :escape? true
          :file "data.tsv"}
         (cli/parse-args ["--"
                          "--input" "tsv"
                          "--format" "markdown"
                          "--headers"
                          "data.tsv"])))
  (is (= :cli-argument-error
         (:type (ex-data (try
                           (cli/parse-args ["--wat"])
                           (catch clojure.lang.ExceptionInfo e e)))))))

(deftest csv-rendering
  (is (= ["| Name  | Note |"
          "|:----- |:---- |"
          "| alice | a,b  |"]
         (cli/render {:input :csv
                      :format :markdown
                      :headers? true
                      :escape? true}
                     "Name,Note\nalice,\"a,b\"\n"))))

(deftest tsv-rendering
  (is (= ["a   b"
          "cc  d"]
         (cli/render {:input :tsv
                      :format :plain
                      :headers? false
                      :escape? true}
                     "a\tb\ncc\td\n"))))

(deftest whitespace-rendering
  (is (= ["+----+---+"
          "| a  | b |"
          "+----+---+"
          "| cc | d |"
          "+----+---+"]
         (cli/render {:input :whitespace
                      :format :ascii-grid
                      :headers? false
                      :escape? true}
                     "a b\ncc d\n"))))
