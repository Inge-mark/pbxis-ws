(defproject com.ingemark/pbxis-ws "1.0.0"
  :description "Asterisk Call Center Web Service"
  :url "http://www.inge-mark.hr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["bundle" "forge://pbxis-ws"]]
  :lein-bundle {:filespec ["README.md" "logback.xml"
                           ["pbxis-config.clj.template" "pbxis-config.clj"]]}
  :aliases {"release" ["xdo"
                       "git-check-clean"
                       ["thrush" ["version-update" ":release"] "edit-version"]
                       ["deploy" "clojars"]
                       ["commit" "New release"]
                       "tag"
                       ["thrush" ["version-update" ":new-snapshot"] "edit-version"]
                       ["commit" "New snapshot"]
                       "push"]
            "publish-latest" ["with-checkout" ":latest"
                              "thrush" "uberjar," "bundle" ".," "upload" "bundle"]}
  :plugins [[lein-nix "0.1.9"]]
  :dependencies [[com.ingemark/pbxis "1.0.0"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/data.json "0.2.1"]
                 [net.cgrand/moustache "1.2.0-alpha2"]
                 [hiccup "1.0.2"]
                 [ring/ring-core "1.1.0" :exclusions [javax.servlet/servlet-api]]
                 [aleph "0.3.0-beta15"]
                 [org.slf4j/slf4j-api "1.7.2"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :jvm-opts ["-Dlogback.configurationFile=logback.xml"]
  :main com.ingemark.pbxis-ws.main
  :profiles {:uberjar {:aot :all}})
