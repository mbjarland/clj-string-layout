(ns string-layout.spec-test
  (:require [midje.sweet :refer [fact tabular]]
            [midje.repl :as m]
            [string-layout.spec :as sls]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))

(tabular
  (fact "Should correctly conform layout strings"
        (s/conform :string-layout.spec/layout-string ?layout-string) => ?expected-value)
  ?layout-string    ?expected-value
  " "               [[:col-padding " "]]
  "foo"             [[:col-padding "foo"]]
  "[L]"             [[:col-align \L]]
  "a[L]b[C]c[R]d"   [[:col-padding "a"]
                     [:col-align \L]
                     [:col-padding "b"]
                     [:col-align \C]
                     [:col-padding "c"]
                     [:col-align \R]
                     [:col-padding "d"]]
  "a[L]"             [[:col-padding "a"] [:col-align \L]]
  "[L]a"             [[:col-align \L] [:col-padding "a"]]
  ""                ::s/invalid
  "["               ::s/invalid
  "]"               ::s/invalid
  "[]"              ::s/invalid
  "[a]"             ::s/invalid
  "[L]foo["         ::s/invalid
  "[L]foo[T]"       ::s/invalid
  )