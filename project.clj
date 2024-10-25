(defproject org.clojars.t_yano/strict "2.1.0"
  :description "A structural validation library for Clojure(Script)"
  :url "https://github.com/tyano/strict"
  :license {:name "public domain"
            :url "https://unlicense.org/"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [org.clojure/clojurescript "1.11.132" :scope "provided"]
                 [funcool/cuerdas "2.2.1"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :codeina {:sources ["src"]
            :reader :clojure
            :target "doc/dist/latest/api"}
  :plugins [[funcool/codeina "0.5.0"]
            [lein-ancient "0.7.0" :exclusions [org.clojure/tools.reader]]]

  :profiles {:dev {:source-paths ["dev/src"]}})
