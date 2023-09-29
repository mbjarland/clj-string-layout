(defproject mbjarland/clj-string-layout "1.0.2"
  :description "A library for laying out strings in rows and columns"
  :url "https://github.org/mbjarland/clj-string-layout"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse "1.4.12"]
                 [com.rpl/specter "1.1.4"]]
  :profiles {:dev {:dependencies [[midje "1.10.9"]]
                   :plugins      [[lein-midje "3.2.2"]]}})