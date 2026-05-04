(ns clj-string-layout.test-runner
  (:require [clj-string-layout.cli-test]
            [clj-string-layout.core-test]
            [clj-string-layout.escape-test]
            [clj-string-layout.property-test]
            [clj-string-layout.table-test]
            [clj-string-layout.width-test]
            [clojure.test :as test]))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'clj-string-layout.cli-test
                                              'clj-string-layout.core-test
                                              'clj-string-layout.escape-test
                                              'clj-string-layout.property-test
                                              'clj-string-layout.table-test
                                              'clj-string-layout.width-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
