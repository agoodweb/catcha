(ns catcha.handler
  (:use compojure.core)
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream
           nl.captcha.Captcha
           javax.imageio.ImageIO
           java.awt.Color)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :refer [deftemplate content set-attr]]
            [clj-petfinder.core :as pets]
            [clojure.edn :as edn]))

(def ^:dynamic *creds* (edn/read-string (slurp "config/petfinder.edn")))

(deftemplate tpl-catcha "templates/catcha.html"
  [id name picture]
  [:.cat-name] (content name)
  [:.captcha-image :img] (set-attr :src "/api/v1/image")
  [:.cat-picture :img] (set-attr :src picture)
  [:.cat-adopt :a] (set-attr :href (str "http://www.petfinder.com/petdetail/" id)))

(defn random-cat
  []
  (pets/random-pet *creds* {:animal "cat"}))

(defn serve-catcha
  [r]
  (let [cat (random-cat)]
    (tpl-catcha (:id cat) (:name cat) (get-in cat [:media :photos 1 "fpm"]))))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] serve-catcha)
  (GET "/:name" [name] (tpl-catcha name))

  (GET "/api/v1/image"
       []
       {:status 200
        :headers {"Content-Type" "image/png"}
        :body (with-open [baos  (ByteArrayOutputStream.)]
                (ImageIO/write (-> (doto (nl.captcha.Captcha$Builder. 176 50)
                                     .addText
                                     .gimp
                                     (.addBackground (nl.captcha.backgrounds.GradiatedBackgroundProducer.))
                                     (.addNoise (nl.captcha.noise.CurvedLineNoiseProducer.))
                                     .addBorder)
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

