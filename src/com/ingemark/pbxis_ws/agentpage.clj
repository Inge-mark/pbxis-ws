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
      });
      $(function() {
        initUserAgent(\"~{agentUri}\", \"~{wsServer}\", \"~{agnt}\", \"~{localpass}\");
      });

      $(function () {
        $('[data-toggle=\"tooltip\"]').tooltip()});"))
       [:body
        [:div {:class "container"}
         [:div {:class "row" :style "min-height: 1vw" }]
         [:div {:class "row"}
          [:div {:class "col-xs-6"}
           [:div {:class "row text-center"}
            [:div {:class "col-xs-4"}
             "queue-name"]
            [:div {:class "col-xs-4"}
             "status"]
            [:div {:class "col-xs-4"}
             "waiting"]]
           (for [q qs]
             [:div {:class "row text-center"}
              [:div {:class "col-xs-4"}
               [:p q]]
              [:div {:class "col-xs-4"}
               [:span {:id (<< "~{q}_agent_status") :class "glyphicon glyphicon-pause agent-glyphico"
                       :data-toggle "tooltip"
                       :title "change state on queue"}]]
              [:div {:id (<< "~{q}_queue_count") :class "col-xs-4"}
               "0"]])]
          [:div {:class "col-xs-6"}
           [:div {:class "row" :style "min-height: 1.5vw"}]
           [:div {:class "row"}
            [:div {:class "col-xs-4"}
             [:p "caller number"]]
            [:div {:class "col-xs-2"}
             [:span {:class "glyphicon glyphicon-earphone agent-glyphico"
                     :data-toggle "tooltip" :title "answer" :style "color:green"}]]
            [:div {:class "col-xs-4"}
             [:p "talking to"]]
            [:div {:class "col-xs-2"}
             [:span {:class "glyphicon glyphicon-earphone agent-glyphico"
                     :data-toggle "tooltip" :title "hangup" :style "color:red"}]]]
           [:div {:class "row"}
            [:div {:class "col-xs-4"}
             [:input {:id "number-to-dial" :type "text" :placeholder "number to dial"}]]
            [:div {:class "col-xs-2"}
             [:span {:id "click-to-dial" :class "glyphicon glyphicon-earphone agent-glyphico"
                     :data-toggle "tooltip" :title "dial" :style "color:green"}]]]]]]
        [:video {:id "localVideo" :muted "mute"}]
        [:video {:id "remoteVideo"}]])
     r/response (r/content-type "text/html") (r/charset "UTF-8"))))
