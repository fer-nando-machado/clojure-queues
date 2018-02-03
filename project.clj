(defproject queues "2.0"
  :description "Queues"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [compojure "1.6.0"]
                 [ring/ring-jetty-adapter "1.6.3"] ;https://github.com/ring-clojure/ring/issues/100
                 [midje "1.8.3"]
                 [org.tcrawley/dynapath "1.0.0"] ;https://github.com/tobias/clojure-java-9/issues/3
                 ]
  ;:main ^:skip-aot queues.core
  :plugins [[lein-ring "0.12.1"]
            [lein-midje "3.2.1"]]
  :ring {:handler queues.core/handler}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
