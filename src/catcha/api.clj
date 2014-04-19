(ns catcha.api
  (:require [clj-petfinder.core :as petfinder]
            [catcha.captcha :as captcha]))

(defn- make-uuid
  "Make a new UUID"
  []
  (str (java.util.UUID/randomUUID)))

(defn make-captcha
  "Make a new captcha datastructure"
  []
  (let [uuid (make-uuid)]
    {:uuid uuid}))


