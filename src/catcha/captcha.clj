;; A lot of this is based on https://github.com/itang/clj-captcha/, modified a bit for my needs
(ns catcha.captcha
  (:import java.io.ByteArrayOutputStream
           java.awt.image.BufferedImage
           javax.imageio.ImageIO
           [com.octo.captcha.service.image
            ImageCaptchaService
            DefaultManageableImageCaptchaService]))

(def ^:dynamic *image-captcha-service-instance*
  (delay (DefaultManageableImageCaptchaService.)))

(defn set-image-captcha-service-instance! [inst]
  (alter-var-root #'*image-captcha-service-instance* (constantly inst)))

(defn- image-captcha-service []
  (force *image-captcha-service-instance*))

(defn captcha-challenge-as-jpeg
  ([captcha-id]
     (captcha-challenge-as-jpeg (image-captcha-service) captcha-id))
  ([^ImageCaptchaService image-captcha-service captcha-id]
     (let [jpeg-outputstream (ByteArrayOutputStream.)
           challenge (.getImageChallengeForID image-captcha-service captcha-id)]
       (ImageIO/write challenge "jpeg" jpeg-outputstream)
       (.toByteArray jpeg-outputstream))))

(defn captcha-response-correct?
  ([captcha-id captcha-response]
     (captcha-response-correct?
      (image-captcha-service) captcha-id captcha-response))
  ([^ImageCaptchaService image-captcha-service captcha-id captcha-response]
     (.validateResponseForID image-captcha-service captcha-id captcha-response)))
