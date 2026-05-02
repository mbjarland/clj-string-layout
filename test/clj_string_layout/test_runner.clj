(ns clj-string-layout.test-runner
  (:require [clj-string-layout.core-test]
            [clojure.test :as test]))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'clj-string-layout.core-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
