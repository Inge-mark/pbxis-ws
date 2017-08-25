(ns com.ingemark.pbxis-ws.agentpage
  (require [clojure.string :as s] [ring.util.response :as r]
           [clojure.core.strint :refer (<<)]
           [hiccup [core :as h] [element :as e] [page :as p]]
           [com.ingemark.pbxis-ws.utils :as u]))


(defn agentpage [agnt user localpass qs agentUri wsServer]
  (let [type "websocket"]
    (->
     (p/html5
       {:xml? true}
       (u/common-head
         [:script {:src "sip-0.7.5.min.js" :type "text/javascript"}])
       (p/include-js
         "/pbxis-client.js" (<< "/pbxis-~{type}.js") "/agent.js")
       (e/javascript-tag (<< "

      $(function() {
        pbxStart(
          [\"~{agnt}\"],
          [~(s/join \",\" (for [q qs] (str \\' q \\')))],
          false);
        loginAndUnpause([\"~{agnt}\"], \"~{user}\",\n
        [~(s/join \",\" (for [q qs] (str \\' q \\')))]);
      });

      $(function () {
        $('[data-toggle=\"tooltip\"]').tooltip()});"))
       [:body
        [:div {:class "container"}
         [:div {:class "jumbotron"}
          [:h1 "Agent page"]
          [:h3 (<< "Logged in: ~{user} (~{agnt})")]]
         [:div {:class "row" :style "min-height: 1vw" }]
         [:div {:class "row"}
          [:div {:class "col-xs-6"}
           [:div {:class "row text-center"}
            [:div {:class "col-xs-8"}
             "Queue name"]
            [:div {:class "col-xs-4"}
             "Status on queue"]]
           (for [q qs]
             [:div {:class "row text-center"}
              [:div {:class "col-xs-8"}
               [:p (<< "~{q} - ") [:span {:id (<< "~{q}_queue_count") :class "badge"}]]]
              [:div {:class "col-xs-4"}
               [:span {:id (<< "~{q}_agent_status") :class "glyphicon glyphicon-pause agent-glyphico"
                       :data-toggle "tooltip"
                       :title "change state on queue"}]]])]
          [:div {:class "col-xs-6"}
           [:div {:class "row"}
            [:div {:class "col-xs-10 callerinfo-font"}
             [:p {:id "caller-info"}
              "free"]]
            [:div {:class "col-xs-2"}
             [:a {:class "btn btn-danger" :onclick "logoutAgent();"} "Logout"]]]]]]
        [:video {:id "localVideo" :muted "mute"}]
        [:video {:id "remoteVideo"}]])
     r/response (r/content-type "text/html") (r/charset "UTF-8"))))
