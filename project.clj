(defproject string-layout "0.1.0-SNAPSHOT"
  :description "A library for laying out strings in rows and columns"
  :url "https://bitbucket.org/mbjarland/clj-string-layout"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [instaparse "1.4.8"]
                 [com.rpl/specter "1.0.5"]]
                 ;;[org.clojure/spec.alpha "0.1.143"]]
                 ;;[clojure-future-spec "1.9.0-alpha15"]]
  :profiles {:dev {:dependencies
                   [[midje "1.9.1"]
                    [org.clojure/test.check "0.9.0"]]
                   :plugins [[lein-midje "3.2.1"]]}})