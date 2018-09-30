(ns com.ingemark.pbxis-ws.main (:gen-class)
    (:use com.ingemark.pbxis-ws))

(defn -main [& args]
  (System/setProperty "logback.configurationFile" "logback.xml")
  (start))
