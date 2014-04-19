(ns catcha.slurper
  "foo"
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]))

(def ^:dynamic *url* "https://www.findthemissing.org/en/cases/22266/8/")

(def ^:dynamic *base-case-url* "https://www.findthemissing.org/en/cases/")

(def ^:dynamic *base-photo-url* "https://www.findthemissing.org/en/photos/full/")

(def ^:dynamic *photo-thumb-url* "https://www.findthemissing.org/en/photos/thumb/")

(def ^:dynamic *keys* [:status :first-name :middle-name :last-name :alias :date-last-seen :date-entered :age-last-seen :age-now :race :ethnicity :sex :height :weight]) 


(def a-missing-person
  "Just some test data I suppose"
  {:ethnicity "Hispanic/Latino", :status "Missing", :middle-name "Viviana", :weight "150.0", :age-now "Unknown DOB", :last-name "Frassinelli", :first-name "Maria", :sex "Female", :date-entered "10/15/2013", :race "Other", :alias "", :age-last-seen "28 to 28 years old", :height "67.0", :date-last-seen "August 11, 2013 00:00"})

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn get-photo-id
  "Gets a photo id from a document"
  [document]
  (-> (html/select document [:.photo :img])
      first
      :attrs
      :src
      (string/split #"/")
      last))

(defn get-photo-url
  "Retrieves a fullsize photo url from a document"
  [document]
  (str *base-photo-url*
       (get-photo-id document)))

(defn generate-missing-person-map
  "Returns a map representing a missing persons case from the NamUS id"
  [id]
  (let [url (str *base-case-url* id)
        document (fetch-url url)]
    (into
     {:id id
      :case-url url
      :photo-url (get-photo-url document)}
     (zipmap *keys*
             (->> (html/select document
                               [:#case_information :table :.view_field])
                  (map html/text)
                  (map string/trim)
                  (map #(string/replace %1 #"[\n\t]+" " ")))) )))

(defn full-name
  "Returns the full name of a missing person"
  [mp]
  (string/join " "
               (:first-name mp)
               (:middle-name mp)
               (:last-name mp)))

