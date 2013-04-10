(defproject com.ingemark/pbxis-ws "0.2.21-SNAPSHOT"
  :description "Asterisk Call Center Web Service"
  :url "http://www.inge-mark.hr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["bundle" "forge://pbxis-ws"]]
  :lein-bundle {:filespec ["README.md" "logback.xml"
                           ["pbxis-config.clj.template" "pbxis-config.clj"]]}
  :aliases {"to-release-version" ["thrush" "version-update" ":release," "edit-version"]
            "to-snapshot" ["thrush" "version-update" ":new-snapshot," "edit-version"]
            "release" ["xdo" "git-check-clean," "to-release-version,"
                       "deploy" "clojars," "commit" "New release," "tag,"
                       "to-snapshot," "commit" "New snapshot," "push"]
            "upload-bundle" ["thrush" "uberjar," "bundle" ".," "upload" "bundle"]
            "publish-latest" ["with-checkout" ":latest" "upload-bundle"]}
  :plugins [[lein-nix "0.1.6"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/data.json "0.2.1"]
                 [com.ingemark/pbxis "0.5.11"]
                 [net.cgrand/moustache "1.2.0-alpha2"]
                 [hiccup "1.0.2"]
                 [ring/ring-core "1.1.0" :exclusions [javax.servlet/servlet-api]]
                 [aleph "0.3.0-beta15"]
                 [org.slf4j/slf4j-api "1.7.2"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :jvm-opts ["-Dlogback.configurationFile=logback.xml"]
  :main com.ingemark.pbxis-ws.main)
