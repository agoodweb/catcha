(ns catcha.handler
  (:use compojure.core)
  (:import java.io.ByteArrayInputStream
           )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [catcha.captcha :as captcha]
            [net.cgrand.enlive-html :refer [deftemplate content]]))

(deftemplate tpl-catcha "templates/catcha.html"
  [name]
  [:.cat-name] (content name))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] (tpl-catcha "Caty McCat"))
  (GET "/:name" [name] (tpl-catcha name))

  (GET "/api/v1/blah"
       []
       {:status 200
        :headers {"Content-Type" "image/jpeg"}
        :body (new java.io.ByteArrayInputStream (captcha/captcha-challenge-as-jpeg "blah"))})

  (GET "/api/v1/iframe" {params :params}
       (if-let [key (:key params)]
         key
         "Sorry, you need to supply an API key as a parameter"))

  
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

