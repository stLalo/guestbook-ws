(ns guestbook.core
  (:require [reagent.core :as reagent :refer [atom]]
            [guestbook.websocket :as ws]
            [ajax.core :refer [GET POST]]
            )
)



(defn get-messages [messageList]
  (GET "/messages"
    {:headers {"Accepts" "application/transit+json"}
     :handler #(reset! messageList (vec %))})
   (.log js/console messageList)
  )

(defn message-list [messages]
  [:ul.content
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

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
  (let [messageList (atom nil)]
    (get-messages messageList)
(fn []
  [:div.container
   [:div.row
    [:div.col
     [:h2 "Welcome to the Guestbook"]]]
   [:div.row
    [:div.col
     [message-list messageList]]]
   [:div.row
    [:div.col
     [message-form]]]])
    ))

(defonce messages (atom []))

(defn update-messages! [{:keys [message]}]
  (swap! messages #(vec (take 10 (conj % message)))))

(defn mount-components []
  (reagent/render-component [#'home-page] (.getElementById js/document "app")))


(defn init! []
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
(mount-components))