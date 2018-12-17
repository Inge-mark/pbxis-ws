(defproject com.ingemark/pbxis-ws "2.0.8-SNAPSHOT"
  :description "Asterisk Call Center Web Service"
  :url "http://www.ingemark.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["bundle" "forge://pbxis-ws"]]
  :lein-bundle {:filespec ["README.md" "logback.xml"
                           ["pbxis-config.clj.template" "pbxis-config.clj"]]}
  :aliases {"release" ["xdo"
                       "git-check-clean"
                       ["thrush" ["version-update" ":release"] "edit-version"]
                      ;["deploy" "clojars"]
                       ["commit" "New release"]
                       "tag"
                       ["thrush" ["version-update" ":new-snapshot"] "edit-version"]
                       ["commit" "New snapshot"]
                       "push"]
            "publish-latest" ["with-checkout" ":latest"
                              "thrush" "uberjar," "bundle" ".," "upload" "bundle"]}
  :plugins [[lein-nix "0.1.9"]]
  :dependencies [[com.ingemark/pbxis "2.0.7"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [net.cgrand/moustache "1.2.0-alpha2"]
                 [hiccup "1.0.5"]
                 [ring/ring-core "1.7.0" :exclusions [javax.servlet/servlet-api]]
                 [io.netty/netty "3.9.9.Final"] ; upgrade from Aleph's default
                                                ; Netty 3.9.0 to 3.9.9 to fix
                                                ; the problem with content
                                                ; length and gzip encoding
                 [aleph "0.3.3" :exclusions [io.netty/netty]]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3"]]
  :jvm-opts ["-Dlogback.configurationFile=logback.xml"]
  :main com.ingemark.pbxis-ws.main
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]
                                  [org.clojure/spec.alpha "0.2.176"]
                                  [org.clojure/core.specs.alpha "0.2.44"]]}})
