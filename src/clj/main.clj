(ns main
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [com.github.bdesham.clj-plist :as plist]
    [com.rpl.specter :refer [ALL MAP-KEYS MAP-VALS submap transform select]])
  (:import
    (java.net URLDecoder)))

(defn track-vector
  [filename]
  (let [file (java.io.File. filename)]
    (vec (vals (get (plist/parse-plist file) "Tracks")))))

(defn key->keyword
  [key]
  (keyword (s/replace (s/lower-case key) #"\s+" "-")))

(defn url->string
  [url]
  (URLDecoder/decode url "UTF-8"))

(defn track-map
  ([filename]
   (track-map filename [:artist :album :name :location :rating :play-count :comments]))
  ([filename only]
   (->> (track-vector filename)
        (transform [ALL MAP-KEYS] key->keyword)
        (transform [ALL (submap [:location]) MAP-VALS] url->string)
        (select [ALL (submap only)]))))

;; TODO Sort the vector by Artist, Album and Track-number
;; TODO Combine track-vector and track-map

(defn plist->edn
  [in-file out-file & [only]]
  (let [tracks (if only
                 (track-map in-file only)
                 (track-map in-file))]
    (with-open [wtr (io/writer out-file)]
      (binding [*out* wtr]
        (println "[")
        (doseq [track tracks]
          (print "{")
          (doseq [row track]
            (pr (key row) (val row))
            (print " "))
          (println "}"))
        (print "]")))))

(comment
  (plist->edn "sample.xml" "/tmp/plist.edn")
  (track-map "sample.xml" [:location])
  (track-map "sample.xml" [:name :artist])
  (track-vector "sample.xml")
  (select [ALL (submap ["Location" "Play Count"])] (track-vector "sample.xml"))
  (transform [ALL MAP-KEYS] key->keyword (track-vector "sample.xml"))
  (->> (track-vector "sample.xml")
       (transform [ALL MAP-KEYS] key->keyword)
       (transform [ALL (submap [:location]) MAP-VALS] url->string))
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3")
)
