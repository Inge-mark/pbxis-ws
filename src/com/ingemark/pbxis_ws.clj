;; Copyright 2018 Ingemark d.o.o.
;;    Licensed under the Apache License, Version 2.0 (the "License");
;;    you may not use this file except in compliance with the License.
;;    You may obtain a copy of the License at
;;        http://www.apache.org/licenses/LICENSE-2.0
;;    Unless required by applicable law or agreed to in writing, software
;;    distributed under the License is distributed on an "AS IS" BASIS,
;;    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;    See the License for the specific language governing permissions and
;;    limitations under the License.

(ns com.ingemark.pbxis-ws
  (:require (com.ingemark [pbxis :as px] [logging :refer :all])
           [com.ingemark.pbxis.util :as pu :refer (>?>)]
           [com.ingemark.pbxis-ws.homepage :refer :all]
           [com.ingemark.pbxis-ws.wallboard :refer :all]
           [com.ingemark.pbxis-ws.loginpage :refer :all]
           [com.ingemark.pbxis-ws.agentpage :refer :all]
           [clojure.core.strint :refer (<<)]
           [clojure.java.io :as io] [clojure.string :as s] [clojure.data.json :as json]
           [clojure.core.incubator :refer (-?> dissoc-in)]
           [net.cgrand.moustache :refer (app)]
           (aleph [http :as ah] [formats :as af])
           [lamina.core :as m]
           [ring.util.response :as r]
           (ring.middleware [params :refer (wrap-params)]
                            [keyword-params :refer (wrap-keyword-params)]
                            [resource :refer (wrap-resource)]
                            [cookies :refer (wrap-cookies)]))
  (:import java.util.concurrent.TimeUnit))

(defonce poll-timeout (atom [30 TimeUnit/SECONDS]))
(defonce unsub-delay (atom [15 TimeUnit/SECONDS]))
(defonce agents (atom {}))
(defonce sessions (atom {}))
(defonce agents-queues (atom {}))
(defonce extensions (atom {}))
(defonce config (atom {}))

(def EVENT-BURST-MILLIS 100)

(defn ->timer [] (java.util.Timer.))

(defn fixed-rate
  ([f per] (fixed-rate f (->timer) 0 per))
  ([f timer per] (fixed-rate f timer 0 per))
  ([f timer dlay per]
   (let [tt (proxy [java.util.TimerTask] [] (run [] (f)))]
     (.scheduleAtFixedRate timer tt dlay per)
     #(.cancel tt))))


(defn- json-request?
  [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))

(defn wrap-json-params [handle]
  (fn [req]
    (if-let [body (and (json-request? req) (:body req))]
      (let [bstr (slurp body)
            json-params (json/read-str bstr)]
        (handle (assoc req :json-params json-params, :params (merge (:params req) json-params))))
      (handle req))))

(defn- wrap-log-request [handle]
  #(as-> % x (do (logdebug "HTTP request" (:request-method x) (:uri x) (:params x)) x)
         (handle x) (spy "HTTP response" x)))

(def ^:dynamic *mocking-on* false)

(defn- wrap-mockable [handle] #(if *mocking-on* {:status 200} (handle %)))

(defn- ok [resp]
  (m/run-pipeline
   resp
   (fn [r]
     (if (m/channel? r)
       {:status 200, :headers {"Content-Type" "text/event-stream"}
        :body (m/map* #(do (logdebug "Send SSE" %)
                           (str "event: " (:type %) "\n"
                                "data: " (af/encode-json->string (dissoc % :type)) "\n"
                                "\n"))
                      r)}
       (if (nil? r) (r/not-found r) (r/response (af/encode-json->string r)))))))

(defn- ticket [] (spy "New ticket" (-> (java.util.UUID/randomUUID) .toString)))

(def ticket->eventch (atom {}))

(defn- invalidate-ticket [tkt]
  (loginfo "Invalidate ticket" tkt)
  (swap! ticket->eventch #(do (-?> (% tkt) :eventch m/close)
                              (dissoc % tkt))))

(defn- set-ticket-invalidator-schedule [tkt reschedule?]
  (let [newsched
        (when reschedule?
          (logdebug "Schedule invalidator" tkt)
          (apply pu/schedule #(invalidate-ticket tkt) @unsub-delay))]
    (swap! ticket->eventch update-in [tkt :invalidator]
           #(do (when % (logdebug "Cancel invalidator" tkt) (pu/cancel-schedule %))
                newsched))))

(defn- logged? [handler]
  (fn [request]
    (let [token (-> request :params :token)]
      (if (get @sessions token)
        (handler request)
        (r/redirect "login")))))

(defn- login [user passwd local localpass]
  (let [found (= passwd (get @agents user))]
    (if found
      (do
        (logdebug (<< "User ~{user} logged in"))
        (let [token (ticket)]
          (swap! sessions assoc token
                 {:user user
                  :local local
                  :localpass localpass})
          (r/redirect (<< "agentpage?token=~{token}"))))
      (do
        (logdebug "Invalid user name or password")
        (r/redirect "login?loginerror=true")))))

(defn- ticket-for [agnts qs summaryEvents]
  (let [tkt (ticket), eventch (px/event-channel agnts qs summaryEvents)]
    (swap! ticket->eventch assoc-in [tkt :eventch] eventch)
    (m/on-closed eventch #(swap! ticket->eventch dissoc tkt))
    (set-ticket-invalidator-schedule tkt true)
    tkt))

(defn- attach-sink [sinkch tkt]
  (when-let [eventch (and sinkch (>?> @ticket->eventch tkt :eventch))]
    (logdebug "Attach sink" tkt)
    (m/on-closed sinkch #(do (logdebug "Closed sink" tkt)
                             (set-ticket-invalidator-schedule tkt true)))
    (pu/leech eventch sinkch)
    (set-ticket-invalidator-schedule tkt false)
    sinkch))

(defn- sse-channel [tkt]
  (let [sinkch (m/channel)]
    (or (attach-sink sinkch tkt)
        (doto sinkch (m/enqueue {:type "closed"}) m/close))))

(defn- long-poll [tkt]
  (logdebug "long-poll" tkt)
  (when-let [eventch (>?> @ticket->eventch tkt :eventch)]
    (when-let [sinkch (attach-sink (m/channel) tkt)]
      (let [finally #(do (m/close sinkch) %)]
        (if-let [evs (m/channel->seq sinkch)]
          (finally (m/success-result (vec evs)))
          (m/run-pipeline (m/read-channel*
                           sinkch :timeout (apply pu/to-millis @poll-timeout), :on-timeout :xx)
                          {:error-handler finally}
                          (m/wait-stage EVENT-BURST-MILLIS)
                          #(vec (let [evs (m/channel->seq sinkch)]
                                  (if (not= % :xx) (conj evs %) evs)))
                          finally))))))

(defn- websocket-events [ticket]
  (fn [ch _]
    (m/ground ch)
    (let [sinkch (m/channel)]
      (when (attach-sink sinkch ticket)
        (m/join (m/map* #(spy "Send WebSocket" (af/encode-json->string %)) sinkch)
                ch)))))

(defonce stop-server (atom nil))

(defn stop []
  (loginfo "Shutting down")
  (future (when @stop-server
            @(@stop-server)
            (loginfo "Shut down.")
            (reset! stop-server nil)))
  "pbxis service shutting down")

(defn- split [s] (s/split s #","))

(def app-main
  (ah/wrap-ring-handler
    (app
      :middlewares [wrap-params wrap-json-params wrap-keyword-params
                    (wrap-resource "static-content")
                    wrap-log-request wrap-mockable]
      ["login"] {:get  {:params   [loginerror]
                        :response (loginpage loginerror (:ex @extensions))}
                 :post {:params   [user pass local localpass]
                        :response (login user pass local localpass)}}
      ["agentpage"] {:middlewares [logged?]
                     :get         {:params   [token]
                                   :response (let [m (get @sessions token)
                                                   agnt (:local m)
                                                   user (:user m)
                                                   localpass (:localpass m)
                                                   qs (reduce (fn [v k]
                                                                (if (some #(= % user)
                                                                          (get @agents-queues k))
                                                                  (concat v [k])
                                                                  v))
                                                              []
                                                              (keys @agents-queues))
                                                   agentUri (<< "sip:~{agnt}@~{(-> @config :ami :host)}")
                                                   wsServer (get @config :wsServer)]
                                               (agentpage agnt user localpass qs agentUri wsServer))}}
      ["client" type [agnts split] [qs split]] {:get {:response (homepage type agnts qs)}}
      ["wallboard" type [qs split]] {:get {:params   [summaryEvents]
                                           :response (wallboard type qs {:summaryEvents summaryEvents})}}
      ["stop"] {:post {:response (ok (stop))}}
      [ticket "long-poll"] {:get {:response (ok (long-poll ticket))}}
      [ticket "websocket"] {:get (ah/wrap-aleph-handler (websocket-events ticket))}
      [ticket "sse"] {:get {:response (ok (sse-channel ticket))}}
      ["ticket"] {:post {:params [agents queues summaryEvents] :response (ok (ticket-for agents queues summaryEvents))}}
      ["originate" src dest] {:post {:params   [callerId variables]
                                     :response (ok (px/originate-call src dest
                                                                      :caller-id callerId :variables variables))}}
      ["redirect-to" dest] {:post {:params   [agent-or-channel]
                                   :response (ok (px/redirect-call agent-or-channel dest))}}
      ["bridged-channels"] {:get {:params   [extension]
                                  :response (ok (px/find-channels extension))}}
      ["park-and-announce"] {:post {:params   [agent-or-channel]
                                    :response (ok (px/park-and-announce agent-or-channel))}}
      ["queue" &] {:get  [["status"] {:params [queue] :response (ok (px/queue-status queue))}]
                   :post [[action]
                          #(ok (as-> (keyword action) action
                                     (px/queue-action action
                                                      (select-keys (% :params)
                                                                   (into [:queue]
                                                                         (condp = action
                                                                           :add [:agent :memberName :paused]
                                                                           :pause [:agent :paused]
                                                                           :remove [:agent]
                                                                           nil))))))]})))

(defn start []
  (let [cfg (read (java.io.PushbackReader. (io/reader "pbxis-config.clj")))
        _ (swap! agents conj (read (java.io.PushbackReader. (io/reader "agents.clj"))))
        _ (swap! extensions conj (read (java.io.PushbackReader. (io/reader "extensions.clj"))))
        _ (swap! agents-queues conj (read (java.io.PushbackReader. (io/reader "agents-queues.clj"))))
        _ (swap! config conj cfg)
        {{:keys [host username password]} :ami, :keys [port]} cfg
        cfg (dissoc (spy "Configuration" cfg) :port :ami)]
    (swap! poll-timeout #(if-let [t (cfg :poll-timeout-seconds)] [t TimeUnit/SECONDS] %))
    (swap! unsub-delay #(if-let [d (cfg :unsub-delay-seconds)] [d TimeUnit/SECONDS] %))
    (when-let [d (* 1000 (cfg :summary-refresh-seconds))]
      (when (> d 0)
        (fixed-rate #(try (px/publish-q-summary-status)
                          (catch Throwable e
                            (logerror "Summary not sent cause is: " e)))
                    (->timer) 10000 d)))
    (let [stop-ami (px/ami-connect host username password cfg)
          stop-http (ah/start-http-server (var app-main)
                                          {:port port :websocket true})]
      (reset! stop-server #(do (loginfo "Disconnecting from Asterisk Management Interface...")
                               (stop-ami)
                               (loginfo "Stopping HTTP server...")
                               (Thread/sleep 500)
                               (stop-http))))))
