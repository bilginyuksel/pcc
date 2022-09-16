(ns storage
  (:require
   [monger.core :as mg]
   [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(def default-db-values {:host "localhost"
                        :port 27017
                        :db "pcc"
                        :collection "default"})

(defn get-db []
  "Returns a database with default values if not given"
  (let [conn (mg/connect)]
    (mg/get-db conn "pcc")))

(defn insert [coll doc]
  "Inserts a document into a collection"
  (let [db (get-db)] (mc/insert db coll doc)))

(insert "phat" {:name "phat" :age 20})