(defproject slatescript-tools "0.1.0-SNAPSHOT"
  :description "Tools for validating translated Microsoft Word (.docx) documents"
  :url "https://slatescript.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :main ^:skip-aot slatescript-tools.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
