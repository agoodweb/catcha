(ns catcha.captcha
  (import  nl.captcha.Captcha
           javax.imageio.ImageIO
           java.awt.Color
           java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream)
  (:require [taoensso.carmine :as car :refer (wcar)]
            [crypto.random]))

(def server1-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(def EXPIRE_SECONDS
  "length of time for captchas to expire"
  60)

(defn new-captcha
  []
  "Generate a new captcha image and text"
  (let [key (crypto.random/hex 10)
        captcha-builder (doto (nl.captcha.Captcha$Builder. 176 50)
                          .addText
                          .gimp
                          (.addBackground (nl.captcha.backgrounds.GradiatedBackgroundProducer.))
                          (.addNoise (nl.captcha.noise.CurvedLineNoiseProducer.))
                          .addBorder)

        captcha (.build captcha-builder)]
    (wcar* (car/set key (.getAnswer captcha) :ex EXPIRE_SECONDS))
    (with-open [baos  (ByteArrayOutputStream.)]
      (ImageIO/write (.getImage captcha) "png" baos)
      (.flush baos)
      {:key key
       :image (ByteArrayInputStream. (.toByteArray baos))
       :answer (.getAnswer captcha)})))

(defn check-answer
  [key answer]
  "Checks the answer for a given key"
  (= (wcar* (car/get key))
     answer))
