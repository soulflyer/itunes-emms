(ns main
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [com.github.bdesham.clj-plist :as plist]
    [com.rpl.specter :refer [ALL MAP-KEYS MAP-VALS submap transform select]])
  (:import
    (java.net URLDecoder)))

(defn key->keyword
  [key]
  (keyword (s/replace (s/lower-case key) #"\s+" "-")))

(defn url->string
  [url]
  (URLDecoder/decode url "UTF-8"))

(defn all-artists
  [tracks]
  ["Moving Hearts" "Timo Maas"])

(defn all-artist-tracks
  [tracks artist]
  {artist (group-by :album (get (group-by :artist tracks) artist))})

(defn track-vector
  ([filename]
   (track-vector filename [:artist :album :name :location :rating :play-count :comments]))
  ([filename only]
   (let [file (java.io.File. filename)
         tracks  (vec (vals (get (plist/parse-plist file) "Tracks")))
         track-vector (->>
                        tracks
                        (transform [ALL MAP-KEYS] key->keyword)
                        (transform [ALL (submap [:location]) MAP-VALS] url->string)
                        (select [ALL (submap only)]))]
     (first (map #(all-artist-tracks track-vector %)
           (all-artists track-vector))))))

(comment
  (track-vector "sample.xml")
  )
;; TODO Sort the vector by Artist, Album and Track-number
;; TODO rename track-vector, its a map now
(defn plist->edn
  [in-file out-file & [only]]
  (let [tracks (if only
                 (track-vector in-file only)
                 (track-vector in-file))]
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
  (track-vector "sample.xml" [:location])
  (track-vector "sample.xml" [:name :artist])
  (track-vector "sample.xml")
  (group-by :album (track-vector "sample.xml"))
  (group-by :artist (track-vector "sample.xml"))

  (all-artists (track-vector "sample.xml"))
  
  (group-by :album (get (group-by :artist (track-vector "sample.xml")) "Moving Hearts"))
  (all-artist-tracks (track-vector "sample.xml") "Moving Hearts")
;; TODO map this^ across all artists
  (map #(all-artist-tracks (track-vector "sample.xml") %)
       (all-artists (track-vector "sample.xml")))
;; TODO now get rid of the extra info in each track (artist and album)
  
  (select [ALL (submap ["Location" "Play Count"])] (track-vector "sample.xml"))
  (transform [ALL MAP-KEYS] key->keyword (track-vector "sample.xml"))
  (->> (track-vector "sample.xml")
       (transform [ALL MAP-KEYS] key->keyword)
       (transform [ALL (submap [:location]) MAP-VALS] url->string))
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3"))
