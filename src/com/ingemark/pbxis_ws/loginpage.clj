(ns com.ingemark.pbxis-ws.loginpage
  (:require [clojure.string :as s] [ring.util.response :as r]
            [clojure.core.strint :refer (<<)]
            [hiccup [core :as h] [element :as e] [page :as p]]
            [com.ingemark.pbxis-ws.utils :as u]))


(defn loginpage
  ([loginerror extensions]
   (->
     (p/html5
      {:xml? false}
      (u/common-head [:script {:src "login.js" :type "text/javascript"}])
      [:body
       [:div {:class "modal-dialog"}
        [:div {:class "loginmodal-container"}
         (when loginerror
           [:div {:class "alert alert-danger"} "Invalid user name or password"])
         [:div {:id "message" :class "alert alert-danger hidden"} "Local is mandatory and it have to be number"]
         [:h1 "Login to your account"]
         [:form {:id "form" :method "POST"}
          [:input {:type "text" :name "user" :placeholder "Username"}]
          [:input {:type "password" :name "pass" :placeholder "Password"}]
          [:select {:id "local" :name "local" :class "form-control"} (for [ex extensions] [:option ex])]
          [:input {:type "password" :name "localpass" :placeholder "Local password"}]
          [:input {:id "submit" :type "submit" :name "login" :class "login loginmodal-submit" :value "Login"}]]]]])
     r/response (r/content-type "text/html") (r/charset "UTF-8"))))
