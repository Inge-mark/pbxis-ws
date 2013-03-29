;; Copyright 2012 Inge-mark d.o.o.
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
  (require (com.ingemark [pbxis :as px] [logging :refer :all])
           [com.ingemark.pbxis.util :as pu :refer (>?>)]
           [com.ingemark.pbxis-ws.homepage :refer :all]
           [clojure.java.io :as io] [clojure.string :as s] [clojure.data.json :as json]
           [clojure.core.incubator :refer (-?> dissoc-in)]
           [net.cgrand.moustache :refer (app)]
           (aleph [http :as ah] [formats :as af])
           [lamina.core :as m]
           [ring.util.response :as r]
           (ring.middleware [params :refer (wrap-params)]
                            [keyword-params :refer (wrap-keyword-params)]
                            [resource :refer (wrap-resource)]
                            [file-info :refer (wrap-file-info)]))
  (import java.util.concurrent.TimeUnit))

(defonce poll-timeout (atom [30 TimeUnit/SECONDS]))
(defonce unsub-delay (atom [15 TimeUnit/SECONDS]))
(def EVENT-BURST-MILLIS 100)

(defn- json-request?
  [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))

(defn wrap-json-params [handle]
  (fn [req]
    (if-let [body (and (json-request? req) (:body req))]
      (let [bstr (slurp body)
            json-params (json/read-str bstr)] #_[:key-fn keyword]
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

(defn- ticket-for [agnts qs]
  (let [tkt (ticket), eventch (px/event-channel agnts qs)]
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
                  wrap-file-info (wrap-resource "static-content")
                  wrap-log-request wrap-mockable]
    ["client" type [agnts split] [qs split]] {:get {:response (homepage type agnts qs)}}
    ["stop"] {:post {:response (ok (stop))}}
    [ticket "long-poll"] {:get {:response (ok (long-poll ticket))}}
    [ticket "websocket"] {:get (ah/wrap-aleph-handler (websocket-events ticket))}
    [ticket "sse"] {:get {:response (ok (sse-channel ticket))}}
    ["ticket"] {:post {:params [agents queues] :response (ok (ticket-for agents queues))}}
    ["originate" src dest] {:post {:params [callerId]
                                   :response (ok (px/originate-call src dest callerId))}}
    ["queue" "status"] {:get {:params [queue] :response (ok (px/queue-status queue))}}
    ["queue" action]
    {:post #(ok (as-> (keyword action) action
                      (px/queue-action
                       action
                       (spy action (select-keys (% :params)
                                                (into [:queue]
                                                      (condp = action
                                                        :add [:interface :memberName :paused]
                                                        :pause [:interface :paused]
                                                        :remove [:interface]
                                                        nil)))))))})))

(defn start []
  (let [cfg (read (java.io.PushbackReader. (io/reader "pbxis-config.clj")))
        {{:keys [host username password]} :ami, :keys [port]} cfg
        cfg (dissoc (spy "Configuration" cfg) :port :ami)]
    (swap! poll-timeout #(if-let [t (cfg :poll-timeout-seconds)] [t TimeUnit/SECONDS] %))
    (swap! unsub-delay #(if-let [d (cfg :unsub-delay-seconds)] [d TimeUnit/SECONDS] %))
    (let [stop-ami (px/ami-connect host username password cfg)
          stop-http (ah/start-http-server (var app-main)
                                          {:port port :websocket true})]
      (reset! stop-server #(do (loginfo "Disconnecting from Asterisk Management Interface...")
                               (stop-ami)
                               (loginfo "Stopping HTTP server...")
                               (Thread/sleep 500)
                               (stop-http))))))
