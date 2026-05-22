(ns clj-string-layout.bb-test-runner
  "Babashka-flavoured entry point for the test suite.

  Runs every namespace from the JVM test runner except clj-string-layout.
  property-test, which depends on org.clojure/test.check (not bundled with
  Babashka). The full suite still runs under the JVM via `clojure -M:test`."
  (:require [clj-string-layout.cli-test]
            [clj-string-layout.core-test]
            [clj-string-layout.escape-test]
            [clj-string-layout.table-test]
            [clj-string-layout.width-test]
            [clojure.test :as test]))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'clj-string-layout.cli-test
                                              'clj-string-layout.core-test
                                              'clj-string-layout.escape-test
                                              'clj-string-layout.table-test
                                              'clj-string-layout.width-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
