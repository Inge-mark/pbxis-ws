(defproject com.ingemark/pbxis-ws "0.2.12-SNAPSHOT"
  :description "Asterisk Call Center Web Service"
  :url "http://www.inge-mark.hr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :lein-release {:deploy-via :none}
  :deploy-repositories [["bundle" "forge://pbxis-ws"]]
  :aliases {"bundle" "bundle-pbxis-ws"
            "publish-checkout" ["thrush" "bundle," "upload" "bundle"]
            "publish-latest" ["with-checkout" ":latest" "publish-checkout"]}
  :plugins [[lein-thrush "0.1.1"]
            [com.ingemark/lein-release "2.1.1"]
            [com.ingemark/lein-with-checkout "0.1.0"]
            [com.ingemark/lein-bundle-pbxis-ws "0.1.5"]
            [com.ingemark/lein-upload "0.1.3"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/data.json "0.2.1"]
                 [com.ingemark/pbxis "0.5.7"]
                 [net.cgrand/moustache "1.2.0-alpha2"]
                 [hiccup "1.0.2"]
                 [ring/ring-core "1.1.0" :exclusions [javax.servlet/servlet-api]]
                 [aleph "0.3.0-beta15"]
                 [org.slf4j/slf4j-api "1.7.2"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :jvm-opts ["-Dlogback.configurationFile=logback.xml"]
  :main com.ingemark.pbxis-ws.main)
