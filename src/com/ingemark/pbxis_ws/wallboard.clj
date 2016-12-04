(ns com.ingemark.pbxis-ws.wallboard
  (require [clojure.string :as s] [ring.util.response :as r]
           [clojure.core.strint :refer (<<)]
           [hiccup [core :as h] [element :as e] [page :as p]]))
(defn- texts [qs]
  (s/join "," (concat (for [q qs] (<< "'~{q}_queue_count'")))))

(defn wallboard [type qs]
  (->
   (p/html5
    {:xml? true}
    [:head
     [:title "PBXIS"]
     [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:href "/bootstrap/css/bootstrap.min.css" :rel "stylesheet"}]
     [:link {:href "/style.css" :rel "stylesheet"}]
     [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js" :type "text/javascript"}]
     [:script {:src "/bootstrap/js/bootstrap.min.js" :type "text/javascript"}]]
    (p/include-js
      "/pbxis-client.js" (<< "/pbxis-~{type}.js") "/homepage.js"
      "/justgage-1.2.2/justgage.js"
      "/justgage-1.2.2/raphael-2.1.4.min.js")
    (e/javascript-tag (<< "
      function pbx_connection(is_connected) {
        $('#connection').html(is_connected? 'Connected' : 'Disconnected');
        if (!is_connected) {
          $.each([~(texts qs)], function(_,id) {
             $('#'+id).html('---');
           });
        }
      }
      $(function() {
             pbx_start([],[~(s/join \",\" (for [q qs] (str \\' q \\')))]);
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
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content giveup"}
              [:p {:class "box-title-small"} "Giveup"]
              [:p {:class "box-value-small" :id (<< "~{q}_abandoned")} 12]]]
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content received"}
              [:p {:class "box-title-small"} "Received"]
              [:p {:class "box-value-small" :id (<< "~{q}_completed")} 12]]]
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content holdtime"}
              [:p {:class "box-title-small"} "Max.wait(sec)"]
              [:p {:class "box-value-small" :id (<< "~{q}_longestHoldTime")} 12]]]]
           [:div {:class "row"}
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content logged"}
              [:p {:class "box-title-small"} "Logged"]
              [:p {:class "box-value-small" :id (<< "~{q}_loggedIn")} 12]]]
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content available"}
              [:p {:class "box-title-small"} "Available"]
              [:p {:class "box-value-small" :id (<< "~{q}_available")} 12]]]
            [:div {:class "col-xs-4 text-center"}
             [:div {:class "sidebar-content holdtime"}
              [:p {:class "box-title-small"} "Avg.wait(sec)"]
              [:p {:class "box-value-small" :id (<< "~{q}_holdTime")} 999]]]]]]
         [:div {:class "row"}
          [:div {:class "col-xs-4 text-center"}]
          [:div {:class "col-xs-8"}
           [:div {:class "row"}
            [:div {:class "col-xs-4"}
             [:div {:id (<< "~{q}_callsChart") :class "sidebar-graph"}]]
            [:div {:class "col-xs-4"}
             [:div {:id (<< "~{q}_callsSLAChart") :class "sidebar-graph"}]]]]]
         ])]])
   r/response (r/content-type "text/html") (r/charset "UTF-8")))

