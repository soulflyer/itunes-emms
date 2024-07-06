(ns main
  (:require
    [clojure.data.zip.xml :as dzx]
    [clojure.string :refer [lower-case replace]]
    [clojure.xml :as xml]
    [clojure.zip :as zip]
    [com.github.bdesham.clj-plist :as plist]
    [com.rpl.specter :refer [ALL MAP-KEYS submap transform select]]))

(defn track-vector [filename]
  (let [file (java.io.File. filename)]
    (vec (vals (get (plist/parse-plist file) "Tracks")))))

(defn key->keyword [key]
  (keyword (replace (lower-case key) #"\s+" "-")))

(defn track-map [filename & [only]]
  (let [only      (or only [:name :artist :location :rating :play-count :comments])]
    (->> (track-vector filename)
        (transform [ALL MAP-KEYS] key->keyword)
        (select [ALL (submap only)]))))

(comment
  (track-map "sample.xml")
  (track-map "sample.xml" [:name :artist])
  (track-vector "sample.xml")
  (select [ALL (submap ["Location" "Play Count"])] (track-vector "sample.xml"))
  (transform [ALL MAP-KEYS] key->keyword (track-vector "sample.xml"))
  (->> (track-vector "sample.xml")
       (transform [ALL MAP-KEYS] key->keyword)
       (select [ALL (submap [:name :artist :location :rating :play-count :comments])]))
)
