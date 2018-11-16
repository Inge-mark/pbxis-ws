(ns com.ingemark.pbxis-ws.wallboard
  (:require [clojure.string :as s]
           [ring.util.response :as r]
           [clojure.core.strint :refer (<<)]
           [hiccup [core :as h] [element :as e] [page :as p]]
           [com.ingemark.pbxis-ws.utils :as u]))
(defn- texts [qs]
  (s/join "," (concat (for [q qs] (<< "'~{q}_queue_count'")))))

(defn value-block [title id class]
  [:div {:class "col-xs-4 text-center"}
   [:div {:class (<< "sidebar-content ~{class}")}
    [:p {:class "box-title-small"} title]
    [:p {:class "box-value-small" :id id} 12]]])

(defn chart [id class]
  [:div {:class "col-xs-4"}
   [:div {:id id :class class}]])

(defn wallboard [type qs summaryEvents]
  (->
   (p/html5
    {:xml? true}
    (u/common-head)
    (p/include-js
      "/pbxis-client.js" (<< "/pbxis-~{type}.js") "/homepage.js"
      "/justgage-1.2.2/justgage.js"
      "/justgage-1.2.2/raphael-2.1.4.min.js")
    (e/javascript-tag (<< "
      function pbxConnection(is_connected) {
        $('#connection').html(is_connected? 'Connected' : 'Disconnected');
        if (!is_connected) {
          $.each([~(texts qs)], function(_,id) {
             $('#'+id).html('---');
           });
        }
      }
      $(function() {
             pbxStart([],[~(s/join \",\" (for [q qs] (str \\' q \\')))], true);
      });"))
    [:body
     [:div {:class "container"}
      (for [q qs]
        [:div {:class "row"}
         [:div {:class "row"}
          [:div {:class "queue-title"}
           [:h1 (<< "Queue ~{q}")]]]
         [:div {:class "row"}
          [:div {:class "col-xs-4 text-center"}
           [:div {:class "main-content"}
            [:p {:class "box-title"} "Waiting"]
            [:p {:class "box-value" :id (<< "~{q}_queue_count")} 12]]]
          [:div {:class "col-xs-8"}
           [:div {:class "row"}
            (value-block "Giveup" (<< "~{q}_abandoned") "giveup")
            (value-block "Received" (<< "~{q}_completed") "received")
            (value-block "Max.wait(sec)" (<< "~{q}_longestHoldTime") "holdtime")]
           [:div {:class "row"}
            (value-block "Logged" (<< "~{q}_loggedIn") "logged")
            (value-block "Available" (<< "~{q}_available") "available")
            (value-block "Avg.wait(sec)" (<< "~{q}_holdTime") "holdtime")]]]
         [:div {:class "row"}
          [:div {:class "col-xs-4 text-center"}
           [:div {:class "table-responsive table-font"}
            [:table {:class "table table-bordered"}
             [:thead
              [:tr [:th "Agent"] [:th "Local"]
               [:th "Calls"] [:th "Status"]]]
             [:tbody {:id (<<"~{q}_agents")}]]]]
          [:div {:class "col-xs-8"}
           [:div {:class "row agent-list"}
            (chart (<< "~{q}_callsChart") "sidebar-graph")
            (chart (<< "~{q}_callsSLAChart") "sidebar-graph")
            (value-block "SLA (sec)" (<< "~{q}_serviceLevel") "holdtime")]]]
         ])]])
   r/response (r/content-type "text/html") (r/charset "UTF-8")))

