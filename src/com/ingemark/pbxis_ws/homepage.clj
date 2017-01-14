(ns com.ingemark.pbxis-ws.homepage
  (require [clojure.string :as s]
           [ring.util.response :as r]
           [clojure.core.strint :refer (<<)]
           [hiccup [core :as h] [element :as e] [page :as p]]
           [com.ingemark.pbxis-ws.utils :as u]))

(defn- texts [agnts qs]
  (s/join "," (concat (for [ag agnts, q qs] (<< "'~{ag}_~{q}_agent_status'"))
                      (for [ag agnts] (<< "'~{ag}_phone_num'"))
                      (for [q qs] (<< "'~{q}_queue_count'")))))

(defn- ext-statuses [agnts]
  (s/join "," (for [ag agnts] (<< "'~{ag}_ext_status'"))))


(defn homepage [type agnts qs]
  (->
   (p/html5
    {:xml? true}
    (u/common-head)
    (p/include-js
      "/pbxis-client.js" (<< "/pbxis-~{type}.js") "/homepage.js")
    (e/javascript-tag (<< "
      function pbxConnection(is_connected) {
        $('#connection').html(is_connected? 'Connected' : 'Disconnected');
        if (!is_connected) {
          $.each([~(texts agnts qs)], function(_,id) {
             $('#'+id).html('---');
          });
          $.each([~(ext-statuses agnts)], function(_,id) {
             $('#'+id).attr('src', '/img/not_inuse.png');
          });
        }
      }
      $(function() {
        pbxStart(
          [~(s/join \",\" (for [ag agnts] (str \\' ag \\')))],
          [~(s/join \",\" (for [q qs] (str \\' q \\')))],
          false);
      });

      $(function () {
        $('[data-toggle=\"tooltip\"]').tooltip()});"))
    [:body
     [:div {:class "jumbotron text-center"}
      [:h1 "PBXIS WS testing page"]
      [:p "Use this page to test PBXIS API"]]
     [:div {:class "container"}
      [:div {:class "row"}
       [:h3 [:span "Connection status: "] [:span {:id "connection"} "Disconnected"]]]
      [:div {:class "row top-buffer"}
       [:ul {:class "nav nav-tabs"}
        [:li {:class "active"} [:a {:data-toggle "tab" :href "#call"} "Call"]]
        [:li [:a {:data-toggle "tab" :href "#redirect"} "Redirect"]]
        [:li [:a {:data-toggle "tab" :href "#park"} "Park"]]
        [:li [:a {:data-toggle "tab" :href "#queue-pan"} "Queue actions"]]]
       [:div {:class "tab-content"}
        [:div {:id "call" :class "tab-pane fade in active"}
         [:form {:id "originate" :onsubmit "return false;"}
          [:div {:class "form-group"}
           [:label {:for "src"} "Agent ext:"]
           [:select {:id "src" :class "form-control"} (for [ag agnts] [:option ag])]
           [:label {:for "dest"} "Number to call:"] [:input {:id "dest" :class "form-control"}]]
          [:button {:class "btn btn-default" :type "submit"} "Issue call!"]]]
        [:div {:id "redirect" :class "tab-pane fade"}
         [:form {:id "redirect" :onsubmit "return false;"}
          [:div {:class "form-group"}
           [:label {:for "agent-or-channel"} "Agent ext.:"]
           [:select {:id "agent-or-channel" :class "form-control"} (for [ag agnts] [:option ag])]
           [:label {:for "redir-dest"} "Redirect to extension:"] [:input {:id "redir-dest" :class "form-control"}]]
          [:button {:type "submit" :class "btn btn-default"} "Issue call transfer"]]]
        [:div {:id "park" :class "tab-pane fade"}
         [:form {:id "park-and-announce" :onsubmit "return false;"}
          [:div {:class "form-group"}
           [:label {:for "agent-or-channel-trans"} "Agent ext.:"]
           [:select {:id "agent-or-channel-trans" :class "form-control"} (for [ag agnts] [:option ag])]]
          [:button {:type "submit" :class "btn btn-default"} "Park current call!"]]]
        [:div {:id "queue-pan" :class "tab-pane fade"}
         [:form {:id "queueaction" :onsubmit "return false;"}
          [:div {:class "form-group"}
           [:label {:for "agent"} "Agent ext:"]
           [:select {:id "agent" :class "form-control"} (for [ag agnts] [:option ag])]
           [:label {:for "queue"} "Issue command on queue:"]
           [:select {:id "queue" :class "form-control"} (for [q qs] [:option q])]
           [:label {:for "agent-name"} "Agent name:"]
           [:input {:id "agent-name" :class "form-control"}]]
          [:button {:id "log-on" :type "button" :class "btn btn-info" :data-toggle "tooltip" :data-placement "top" :title "Add agent to queue"}
           [:span {:class "glyphicon glyphicon-log-in"}]]
          [:button {:id "unpause" :type "button" :class "btn btn-info" :data-toggle "tooltip" :data-placement "top" :title "Unpause agent"}
           [:span {:class "glyphicon glyphicon-headphones"}]]
          [:button {:id "pause" :type "button" :class "btn btn-info" :data-toggle "tooltip" :data-placement "top" :title "Pause agent"}
           [:span {:class "glyphicon glyphicon-pause"}]]
          [:button {:id "log-off" :type "button" :class "btn btn-info" :data-toggle "tooltip" :data-placement "top" :title "Remove agent from queue"}
           [:span {:class "glyphicon glyphicon-log-out"}]]]]]]
      [:div {:class "row top-buffer"}
       (for [q qs]
         [:div {:class "col-md-3 col-sm-3 col-xs-12"}
          [:div {:class "panel panel-success"}
           [:div {:class "panel-heading"} (<< "Number of calls in queue ~{q}:")]
           [:div {:class "panel-body"}
            [:span {:id (<< "~{q}_queue_count")} "---"]]]])]
      [:div {:class "row"}
       (for [agrow (partition-all 4 agnts)]
         (list
           (for [ag agrow]
             [:div {:class "col-md-3 col-sm-3 col-xs-12"}
              [:div {:class "panel panel-info"}
               [:div {:class "panel-heading"}
                [:span {:id (<< "~{ag}_name")} (<< "Agent ~{ag}")]
                [:span {:id (<< "~{ag}_ext_status") :class "pull-right glyphicon glyphicon-phone-alt"}]]
               [:div {:class "panel-body"}
                [:div {:class "row"}
                 [:div {:class "col-md-4 col-sm-4"} [:span "Party"]]
                 [:div {:class "col-md-4 col-sm-4"} [:span {:id (<< "~{ag}_phone_num")}]]]
                (for [q qs]
                  [:div {:class "row"}
                   [:div {:class "col-md-4 col-sm-4"} [:span q]]
                   [:div {:class "col-md-4 col-sm-4"} [:span {:id (<< "~{ag}_~{q}_agent_status") :class "glyphicon"}]]])]]])))]]])
   r/response (r/content-type "text/html") (r/charset "UTF-8")))

