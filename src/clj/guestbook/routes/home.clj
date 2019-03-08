(ns guestbook.routes.home
  (:require [guestbook.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [ring.util.response :refer [response]]
            [guestbook.db.core :as db]
            [clojure.java.io :as io]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn about-page [request]
  (layout/render  request "about.html"))

(defroutes home-routes
  (GET "/" request (home-page request))
  (GET "/messages" [] (response (db/get-messages)) )
  (GET "/about" request (about-page request)))

