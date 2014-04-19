(ns catcha.petfinder
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]
        [clojure.string :only [join]]
        [clojure.set :only [rename-keys]]
        [slingshot.slingshot :only [throw+]]
        [digest :only [md5]]
        [clojure.pprint :only [pprint]]) 
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as http]))


(def ^:private base-url "http://api.petfinder.com/")

(def *api-key* "95b2c9cbc7645f893835fdb9dd371138")
(def *api-secret* "838525f9c79d571d42d5f0525b5c5dfe")

(defn- url-for
  "Returns a full URL "
  [path]
  (str base-url path))

(defn- api-call
  ([path]
     (api-call path nil))
  ([path params]
     (let [query-params (into {:key *api-key*} params)
           _ (println (http/get (url-for path) {:query-params query-params}) )
           xml-resp (-> (http/get (url-for path) {:query-params query-params})
                        :body
                        .getBytes
                        java.io.ByteArrayInputStream.
                        xml/parse
                        zip/xml-zip)
           status (xml1-> xml-resp :header :status :code text)
           message (xml1-> xml-resp :header :status :message text)]
       (if (not= status "100")
         (throw+ {:type :petfinder :code status :message message} )
         xml-resp))))

(defn- signed-api-call
  "Wraps 'api-call' with a signature."
  ([path]
     (signed-api-call path nil))
  ([path params]
     (let [query-params (into {:key *api-key*} params)
           param-str (join "&" (map #(str (name (first %)) "=" (second %)) query-params))
           sig (md5 (str *api-secret* param-str))]
       (api-call path (into params {:sig sig})))))

(defn- pet->map
  "Returns a map constructed from a pet xml record."
  [pet]
  ;; I should find a less ugly way of doing this 
  {:id (xml1-> pet :id text)
   :shelter-id (xml1-> pet :shelterId text)
   :shelter-pet-id (xml1-> pet :shelterPetId text)
   :name (xml1-> pet :name text)
   :animal (xml1-> pet :animal text)
   :breeds (xml-> pet :breeds :breed text)
   :mix (xml1-> pet :mix text)
   :age (xml1-> pet :age text)
   :sex (xml1-> pet :sex text)
   :size (xml1-> pet :size text)
   :options (xml-> pet :options :option text)
   :description (xml1-> pet :description text)
   :last-update (xml1-> pet :lastUpdate text)
   :media {:photos (xml-> pet :media :photos :photo text)}
   :contact {:address1 (xml1-> pet :contact :address1 text)
             :address2 (xml1-> pet :contact :address2 text)
             :city (xml1-> pet :contact :city text)
             :state (xml1-> pet :contact :state text)
             :zip (xml1-> pet :contant :zip text)
             :fax (xml1-> pet :contact :fax text)
             :email (xml1-> pet :contact :email text)}})

(defn- shelter->map
  "Returns a map constructed from a shelter xml record."
  [shelter]
  {:id (xml1-> shelter :id text)
   :name (xml1-> shelter :name text)
   :address1 (xml1-> shelter :address1 text)
   :address2 (xml1-> shelter :address2 text)
   :city (xml1-> shelter :city text)
   :state (xml1-> shelter :state text)
   :zip (xml1-> shelter :zip text)
   :country (xml1-> shelter :country text)
   :latitude (xml1-> shelter :latitude text)
   :longitude (xml1-> shelter :longitude text)
   :phone (xml1-> shelter :phone text)
   :fax (xml1-> shelter :contact :fax text)
   :email (xml1-> shelter :contact :email text)
   })

(defn token
  "Returns a token valid for a timed session (usually 60 minutes)."
  []
  (let [xml-resp (signed-api-call "auth.getToken")]
    (xml1-> xml-resp :auth :token text)))

(defn breeds
  "Returns a list of breeds for a particular animal.."
  [animal]
  (let [xml-resp (signed-api-call "breed.list" {:animal animal})]
    (xml-> xml-resp :breeds :breed text)))

(defn pet
  "Returns a record for a single pet."
  [id]
  (xml1-> (api-call "pet.get" {:id id})
          :pet
          pet->map))

;; I found that that the output=basic parameter returns the same
;; result as output=full. Considering the petfinder API has this bug,
;; I won't support user controlled output for now, but in the future
;; 'get-random-pet-id' can be implemented
(defn random-pet
  "Returns a full record for a randomly selected pet. Pass in
  characterists of the pet to return in optional params map:
    :animal    => type of animal (barnyard, bird, cat, dog, horse, pig, reptile, smallfurry)
    :breed     => breed of animal (use breeds.list for a list of valid breeds)
    :size      => size of animal (S=small, M=medium, L=large, XL=extra-large)
    :sex       => M=male, F=female
    :location  => the ZIP/postal code or city and state the animal
               should be located (NOTE: the closest possible animal
               will be selected)
    :shelter-id => ID of the shelter that posted the pet"
  ([] (random-pet {}))
  ([params]
     (let [params (into {:output "full"} params) ;full output by default
           params (rename-keys params {:shelter-id :shelterid})] ;API expects param to be without hyphen or capitalization)
       (xml1-> (api-call "pet.getRandom" params)
               :pet
               pet->map))))

(defn find-pets
  "Searches for pets according to the criteria you provde and returns
  a collection of pet records matching your search. The results will
  contain at most 'count' records per query, and a 'lastOffset'
  tag. To retrieve the next result set, use the 'lastOffset' value as
  the offset to the next 'find-pet' call. Returns _basic_ records.
  'location' is the ZIP/postal code or city and state the animal
  should be located (NOTE: the closest possible animal will be
  selected)

  Optional Params are:
    :animal    => type of animal (barnyard, bird, cat, dog, horse, pig, reptile, smallfurry)
    :breed     => breed of animal (use breeds.list for a list of valid breeds)
    :size      => size of animal (S=small, M=medium, L=large, XL=extra-large)
    :sex       => M=male, F=female
    
    :age       => age of the animal (Baby, Young, Adult, Senior)
    :offset    => set this to the value of lastOffset returned by a
                  previous call to pet.find, and it will retrieve the
                  next result set.
    :count     => how many records to return for this particular API
                  call (default is 25)"

  ([location] (find-pets location {}))
  ([location params] 
     (let [params (into {:location location :output "full"} params)] ;full output by default
       (xml-> (api-call "pet.find" params)
              :pets
              :pet
              pet->map))))

(defn shelter
  "Returns a record for a single shelter"
  [id]
  (xml1-> (api-call "shelter.get" {:id id})
         :shelter
         shelter->map))

(defn find-shelters
  "Returns a collection of shelter records near 'location'. 

  Optional params are:
    :name      => full or partial shelter name
    :offset    => set this to the value of lastOffset returned by a
                  previous call to pet.find, and it will retrieve the
                  next result set.
    :count     => how many records to return for this particular API
                  call (default is 25)"
  ([location] (find-shelters location {}))
  ([location params]
     (let [params (into {:location location} params)]
       (xml-> (api-call "shelter.find" params)
              :shelters
              :shelter
              shelter->map))))

(defn find-shelters-by-breed
  "Optional params are:
    :offset    => set this to the value of lastOffset returned by a
                  previous call to pet.find, and it will retrieve the
                  next result set.
    :count     => how many records to return for this particular API
                  call (default is 25)"
  ([animal breed] (find-shelters-by-breed animal breed {}))
  ([animal breed params]
     (let [params (into {:animal animal :breed breed} params)]
       (xml-> (api-call "shelter.listByBreed" params)
              :shelters
              :shelter
              shelter->map))))




(defn shelter-pets
  "Returns a collection of pet records in a given shelter.

  Optional params are:
    :offset    => set this to the value of lastOffset returned by a
                  previous call to pet.find, and it will retrieve the
                  next result set.
    :count     => how many records to return for this particular API
                  call (default is 25)"

  ([id] (shelter-pets id {}))
  ([id params]
     (let [params (into {:id id :output "full"} params)]
       (xml-> (api-call "shelter.getPets" params)
              :pets
              :pet
              pet->map))))
