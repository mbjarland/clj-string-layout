(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as deploy]))

(def lib 'com.github.mbjarland/clj-string-layout)
(def version "1.0.3")

(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def basis (delay (b/create-basis {:project "deps.edn"})))

(def pom-data
  [[:description "A Clojure library for laying out strings in rows and columns"]
   [:url "https://github.com/mbjarland/clj-string-layout"]
   [:licenses
    [:license
     [:name "Eclipse Public License 1.0"]
     [:url "https://www.eclipse.org/legal/epl-v10.html"]]]
   [:scm
    [:url "https://github.com/mbjarland/clj-string-layout"]
    [:connection "scm:git:git://github.com/mbjarland/clj-string-layout.git"]
    [:developerConnection "scm:git:ssh://git@github.com/mbjarland/clj-string-layout.git"]]])

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :pom-data pom-data})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (doseq [file ["README.md" "CHANGELOG.md" "LICENSE"]]
    (b/copy-file {:src file
                  :target (str class-dir "/" file)}))
  (b/jar {:class-dir class-dir
          :jar-file jar-file})
  (println "Built" jar-file))

(defn install [_]
  (jar nil)
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir})
  (println "Installed" lib version "locally"))

(defn deploy [_]
  (jar nil)
  (deploy/deploy {:installer :remote
                  :artifact jar-file
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
