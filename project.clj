(defproject catcha "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [org.clojure/data.zip "0.1.1"]
                 [enlive "1.1.5"]
                 [com.octo.captcha/jcaptcha "1.0"]
                 [clj-http "0.9.0"]
                 [clj-petfinder "0.1.0-SNAPSHOT"]]
  ;; for com.jhlabs:imaging:jar:01012005
  :repositories [["jahia" "http://maven.jahia.org/maven2"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler catcha.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
