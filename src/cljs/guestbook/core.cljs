(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [guestbook.websocket :as ws]
            [ajax.core :refer [GET POST]]
            )
)

(defonce messages (atom []))

(defn get-messages [messages]
  (GET "/messages"
    {:headers {"Accepts" "application/transit+json"}
     :handler #(reset! messages (vec %))}
    )
  )

(defn message-list [messages]
  [:ul
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     [:li message])])

(defn message-form []
    (let [fieldValue (atom nil)]
        (fn []
            [:div.container
                [:div.form-group
                    [:p "Name:"
                        [:input.form-control 
                            {:type :text
                             :placeholder "Type in your name"
                             :value (:name @fieldValue)
                             :on-change #(swap! fieldValue assoc :name (-> % .-target .-value))
                             }]]
                    [:p "Message:"
                        [:textarea.form-control
                            {:rows 4
                             :cols 50
                             :value (:message @fieldValue)
                             :on-change #(swap! fieldValue assoc :message (-> % .-target .-value))}
                             ]]
                    [:input.btn.btn-primary 
                        { :type :submit
                          :on-click #(ws/send-transit-message! @fieldValue)
                          :value "comment"}]]]
        )
    )
)


(defn home-page []
    [:div.container
        [:div.row
            [:div.col
                [:h2 "Welcome to the Guestbook"]]]
        [:div.row
            [:div.col
             (get-messages messages)
             [message-list messages]]]
        [:div.row
                [:div.col
                    [message-form]]]])

(defn update-messages! [{:keys [message]}]
  (swap! messages #(vec (take 10 (conj % message)))))

(defn mount-components []
  (reagent/render-component [#'home-page] (.getElementById js/document "app")))


(defn init! []
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
(mount-components))