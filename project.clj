(defproject tennis-app "0.1.0-SNAPSHOT"
  :description "Tennis programming challenge for DataRock."
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [dev.weavejester/medley "1.8.0"]]
  :main ^:skip-aot tennis-app.core
  :repl-options {:init-ns tennis-app.core}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
