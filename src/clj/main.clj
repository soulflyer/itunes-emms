(ns main
  (:require
    [clojure.edn :as edn]
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

;; TODO blank out the empty ones (albums and artists)

(comment
  ;; track-map can take a vector of keywords. There is no point including :artist or :album
  ;; those will be added anyway and used to build the data structure.
  (track-map "sample.xml")
  (track-map "sample.xml" [:location])
  (track-map "sample.xml" [:name :play-count])
  (track-map "Library.xml" [:comments])

  ;; track-map is slow with a decent sized library, so use def to store it in memory.
  ;; Editing the data by hand is easier in edn format than the original plist format.
  ;; Store it in a file like this:
  (spit "/tmp/out.edn" (with-out-str (pp/pprint (track-map "Library.xml"))))
  ;;And read it back in like this:
  (def track-data (edn/read-string (slurp "out.edn")))
  
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3")
)
