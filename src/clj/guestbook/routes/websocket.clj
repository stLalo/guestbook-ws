(ns guestbook.routes.websocket
    (:require [compojure.core :refer [GET defroutes wrap-routes]]
              [clojure.tools.logging :as log]
              [immutant.web.async :as async]
              [struct.core :as st]
              [cognitect.transit :as transit]
              [guestbook.db.core :as db]))



(def message-schema
  [[:name
    st/required
    st/string]

   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(> (count %) 9)}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))

(defn save-message! [message]
    (if-let [errors (validate-message message)]
      {:errors errors}
      (do
        (db/save-message! message)
        message
      )
    )
)

(defonce channels (atom #{}))

(defn encode-transit [message]
  (let [out
        (java.io.ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer message)
    (.toString out)))

(defn decode-transit [message]
  (let [in (java.io.ByteArrayInputStream. (.getBytes message))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn handle-message! [channel message]
  (let [response (-> message 
                     decode-transit
                     (assoc :timestamp (java.util.Date.))
                     save-message! )]
    (if (:errors response)
      (async/send! channel (encode-transit response))
      (doseq [channel @channels]
        (async/send! channel (encode-transit response)))))
  )

(defn notify-clients! [channel msg]
  (handle-message! channel msg)
    )

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))


(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defroutes websocket-routes
  (GET "/ws" [] ws-handler))
