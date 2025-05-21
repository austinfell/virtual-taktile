(defproject vtakt-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [metosin/reitit "0.8.0-alpha1"]
                 [ring/ring-defaults "0.6.0"]
                 [ring/ring-core "1.13.0"]
                 [ring-cors "0.1.13"]
                 [ring/ring-json "0.5.1"]
                 [info.sunng/ring-jetty9-adapter "0.36.1"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [compojure "1.7.0"]
                 [org.clojure/core.async "1.7.701"]
                 [org.clojure/tools.logging "1.3.0"]
                 [overtone "0.16.3331"]]
  :main ^:skip-aot vtakt-server.core
  :target-path "target/%s"
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler vtakt-server.core/default-handler}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
