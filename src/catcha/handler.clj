(ns catcha.handler
  (:use compojure.core)
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream
           nl.captcha.Captcha
           javax.imageio.ImageIO)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [catcha.captcha :as captcha]
            [net.cgrand.enlive-html :refer [deftemplate content set-attr]]))

(deftemplate tpl-catcha "templates/catcha.html"
  [name]
  [:.cat-name] (content name)
  [:.captcha-image :img] (set-attr :src "/api/v1/image"))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] (tpl-catcha "Caty McCat"))
  (GET "/:name" [name] (tpl-catcha name))

  (GET "/api/v1/image"
       []
       {:status 200
        :headers {"Content-Type" "image/png"}
        :body (with-open [baos  (ByteArrayOutputStream.)]
                (ImageIO/write (-> (doto (nl.captcha.Captcha$Builder. 176 50)
                                     .addText)
                                   .build
                                   .getImage) "png" baos)
                (.flush baos)
                (ByteArrayInputStream. (.toByteArray baos)))
        })

  (GET "/api/v1/iframe" {params :params}
       (if-let [key (:key params)]
         key
         "Sorry, you need to supply an API key as a parameter"))

  
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

