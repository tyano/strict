(require '[cljs.build.api :as b])

(println "Building ...")

(let [start (System/nanoTime)]
  (b/build
   (b/inputs "test" "src" )
   {:main 'strict.tests
    :output-to "out/tests.js"
   :output-dir "out/tests"
    :target :nodejs
    :pretty-print true
    :optimizations :advanced
    :language-in  :ecmascript-2017
    :language-out :ecmascript-2017
    :verbose true})
  (println "... done. Elapsed" (/ (- (System/nanoTime) start) 1e9) "seconds"))
