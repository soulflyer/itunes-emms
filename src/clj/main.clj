(ns main
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [clojure.pprint :as pp]
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

(defn all-artist-tracks
  [tracks artist]
  {artist (group-by :album (get (group-by :artist tracks) artist))})

(defn remove-artist-and-album
  [track-map]
  (apply dissoc track-map [:artist :album]))

(defn track-map
  ([filename]
   (track-map filename [:name :location :rating :play-count :comments]))
  ([filename only]
   (let [file (java.io.File. filename)
         tracks           (vec (vals (get (plist/parse-plist file) "Tracks")))
         only             (into only [:album :artist])
         track-vector     (->>
                            tracks
                            (transform [ALL MAP-KEYS] key->keyword)
                            (transform [ALL (submap [:location]) MAP-VALS] url->string)
                            (select [ALL (submap only)]))
         artists          (into [] (set (select [ALL :artist] track-vector)))]
     (transform [MAP-VALS MAP-VALS ALL]
                remove-artist-and-album
                (into {}
                      (map #(all-artist-tracks track-vector %)
                           artists))))))
;;TODO blank out the empty ones (albums and artists)

(comment
  (track-map "sample.xml")
  (track-map "Library.xml" [:comments])
  (spit "/tmp/out.edn" (with-out-str (pp/pprint (track-map "Library.xml"))))
  )

;; TODO This may not work since track-map changed
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
  (track-map "sample.xml")
  (keys (track-map "sample.xml"))
  (group-by :album (track-map "sample.xml"))
  (group-by :artist (track-map "sample.xml"))

  (group-by :album (get (group-by :artist (track-map "sample.xml")) "Moving Hearts"))
  (all-artist-tracks (track-map "sample.xml") "Moving Hearts")
  
;; TODO now get rid of the extra info in each track (artist and album)
  
  (select [ALL (submap ["Location" "Play Count"])] (track-map "sample.xml"))
  (transform [ALL MAP-KEYS] key->keyword (track-map "sample.xml"))
  (->> (track-map "sample.xml")
       (transform [ALL MAP-KEYS] key->keyword)
       (transform [ALL (submap [:location]) MAP-VALS] url->string))
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3"))
