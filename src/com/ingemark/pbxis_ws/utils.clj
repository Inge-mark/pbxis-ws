(ns com.ingemark.pbxis-ws.utils)


(defn common-head [& args]
  (conj [:head [:title "PBXIS"]
           [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
           [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
           [:link {:href "/bootstrap/css/bootstrap.min.css" :rel "stylesheet"}]
           [:link {:href "/style.css" :rel "stylesheet"}]
           [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js" :type "text/javascript"}]
           [:script {:src "/bootstrap/js/bootstrap.min.js" :type "text/javascript"}]
           [:script {:src "/utils.js" :type "text/javascript"}]]
          args))
