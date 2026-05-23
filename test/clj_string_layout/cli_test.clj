(ns clj-string-layout.cli-test
  (:require [clj-string-layout.cli :as cli]
            [clojure.test :refer [deftest is testing]]))

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
  (is (= 40
         (:width (cli/parse-args ["--from" "csv" "--to" "ascii-grid" "--width" "40"]))))
  (is (= :tsv
         (:input (cli/parse-args ["--from" "tsv"]))))
  (is (true?
        (:fill? (cli/parse-args ["--from" "csv" "--to" "box" "--width" "30" "--fill"]))))
  (is (= :cli-argument-error
         (:type (ex-data (try
                           (cli/parse-args ["--wat"])
                           (catch clojure.lang.ExceptionInfo e e))))))
  (is (= :cli-argument-error
         (:type (ex-data (try
                           (cli/parse-args ["--width" "wide"])
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

(deftest fill-expands-toward-width
  (let [lines (cli/render {:input :csv
                           :format :box
                           :headers? true
                           :escape? true
                           :width 30
                           :fill? true}
                          "name,qty\napple,12\n")]
    (is (= 30 (count (first lines))))
    (is (every? #(= 30 (count %)) lines))))

(deftest streaming-csv-row-seq
  (testing "basic rows"
    (with-open [r (java.io.StringReader. "a,b\nfoo,bar\nbaz,qux\n")]
      (is (= [["a" "b"] ["foo" "bar"] ["baz" "qux"]]
             (vec (cli/csv-row-seq r))))))
  (testing "quoted comma"
    (with-open [r (java.io.StringReader. "name,note\nalice,\"a, b\"\n")]
      (is (= [["name" "note"] ["alice" "a, b"]]
             (vec (cli/csv-row-seq r))))))
  (testing "doubled quotes inside quoted field"
    (with-open [r (java.io.StringReader. "name,quote\nalice,\"said \"\"hi\"\"\"\n")]
      (is (= [["name" "quote"] ["alice" "said \"hi\""]]
             (vec (cli/csv-row-seq r))))))
  (testing "embedded newline inside quoted field assembles a logical row"
    (with-open [r (java.io.StringReader. "a,b\nfoo,\"line1\nline2\"\nbar,baz\n")]
      (is (= [["a" "b"] ["foo" "line1\nline2"] ["bar" "baz"]]
             (vec (cli/csv-row-seq r))))))
  (testing "is lazy — doesn't consume until realised"
    (let [reads (atom 0)
          r (proxy [java.io.Reader] []
              (read
                ([] (swap! reads inc) -1)
                ([cbuf] (swap! reads inc) -1)
                ([cbuf off len] (swap! reads inc) -1))
              (close [] nil))]
      (cli/csv-row-seq r)             ; should NOT read yet
      (is (zero? @reads)))))

(deftest streaming-tsv-row-seq
  (with-open [r (java.io.StringReader. "a\tb\nfoo\tbar\n")]
    (is (= [["a" "b"] ["foo" "bar"]]
           (vec (cli/tsv-row-seq r))))))

(deftest cli-main-streams-width-free-formats
  (testing "csv output streams through the writer-sink path"
    (let [out (java.io.StringWriter.)]
      (with-redefs [clj-string-layout.cli/open-reader
                    (fn [_] (java.io.BufferedReader.
                              (java.io.StringReader.
                                "name,qty\napple,12\npear,4\n")))]
        (binding [*out* out]
          (cli/-main "--from" "csv" "--to" "csv" "--headers")))
      (is (= "name,qty\napple,12\npear,4\n" (str out)))))
  (testing "tsv output"
    (let [out (java.io.StringWriter.)]
      (with-redefs [clj-string-layout.cli/open-reader
                    (fn [_] (java.io.BufferedReader.
                              (java.io.StringReader.
                                "name,qty\napple,12\n")))]
        (binding [*out* out]
          (cli/-main "--from" "csv" "--to" "tsv")))
      (is (= "name\tqty\napple\t12\n" (str out)))))
  (testing "pipe output"
    (let [out (java.io.StringWriter.)]
      (with-redefs [clj-string-layout.cli/open-reader
                    (fn [_] (java.io.BufferedReader.
                              (java.io.StringReader.
                                "a,b\nfoo,bar\n")))]
        (binding [*out* out]
          (cli/-main "--from" "csv" "--to" "pipe")))
      (is (= "a|b\nfoo|bar\n" (str out))))))

(deftest streaming-whitespace-row-seq
  (testing "trims, splits, skips blank lines"
    (with-open [r (java.io.StringReader. "  a   b\n\n  cc d  \n")]
      (is (= [["a" "b"] ["cc" "d"]]
             (vec (cli/whitespace-row-seq r)))))))

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
