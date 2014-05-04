(ns catcha.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :refer [deftemplate content set-attr]]
            [clj-petfinder.core :as pets]
            [clojure.edn :as edn]
            [catcha.captcha :as captcha :refer [new-captcha]]))

(def ^:dynamic *creds*
  "Load credentials from settings file"
  (edn/read-string (slurp "config/petfinder.edn")))

(deftemplate tpl-catcha "templates/catcha.html"
  [id name picture]
  [:.cat-name] (content (if (> (count name) 13) (str (subs name 0 10) "...") name))
  [:.captcha-image :img] (set-attr :src "/api/v1/image")
  [:.cat-picture :div] (set-attr :style (str "background: url(" picture ") no-repeat center center; -webkit-background-size: cover; -moz-background-size: cover; -o-background-size: cover; background-size: cover; border-radius: 8px; height: 86px;"))
  [:.cat-adopt :a] (set-attr :href (str "http://www.petfinder.com/petdetail/" id)))

(defn random-cat-with-picture
  []
  (let [cat  (pets/random-pet *creds* {:animal "cat"})
        picture (get-in cat [:media :photos 1 "fpm"])]
    (if picture
      cat
      (random-cat-with-picture))))

(defn serve-catcha
  [r]
  (let [cat (random-cat-with-picture)]
    (tpl-catcha (:id cat) (:name cat) (get-in cat [:media :photos 1 "fpm"]))))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] serve-catcha)
  (GET "/:name" [name] (tpl-catcha name))

  (GET "/api/v1/image"
       []
       {:status 200
        :headers {"Content-Type" "image/png"}
        :body {:image (new-captcha)}
        })

  (GET "/api/v1/iframe" {params :params}
       (if-let [key (:key params)]
         key
         "Sorry, you need to supply an API key as a parameter"))


  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
