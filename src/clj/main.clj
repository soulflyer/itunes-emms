(ns main
  (:require
    [util :refer [key->keyword url->string truncate-location track-map->edn]]
    [stats :refer [count->stats]]
    [sticker :refer [comment->sticker rating->sticker]]
    [clojure.edn :as edn]
    [clojure.pprint :as pp]
    [com.github.bdesham.clj-plist :as plist]
    [com.rpl.specter :refer [ALL MAP-KEYS MAP-VALS submap transform select selected? must setval NONE compact]]))

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

(defn plist->edn
  [plist-file edn-file]
  (track-map->edn (track-map plist-file) edn-file))

(defn track-data
  [edn-file]
  (doall (edn/read-string (slurp edn-file))))

(defn rated-tracks
  [track-data]
  (select [MAP-VALS MAP-VALS ALL (selected? (must :rating))] track-data))

(defn counted-tracks
  [track-data]
  (select [MAP-VALS MAP-VALS ALL (selected? (must :play-count))] track-data))

(defn commented-tracks
  [track-data]
  (select [MAP-VALS MAP-VALS ALL (selected? (must :comments))] track-data))

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
  (spit "track-data.edn" (with-out-str (pp/pprint (track-map "Library.xml"))))
  ;; or (Untested)
  (plist->edn "Library.xml" "track-data.edn")  
  ;;And read it back in like this:
  (get-in (track-data "track-data.edn") ["Yes" "Going For The One"])

  ;; List all the tracks that have comments
  (select [MAP-VALS MAP-VALS ALL (selected? (must :comments))] (track-data "track-data.edn"))

  (first (commented-tracks (track-data "track-data.edn")))
  ;; =>
  ;; {:name "Hole In The Sky",
  ;; :location
  ;; "file:///Users/iain/Music/Collection/Shining/Animal/10 Hole In The Sky.mp3",
  ;; :comments "Meh"}

  (comment->sticker (first (commented-tracks track-data)))
  ;; puts the first comment into the sticker db

  ;; Backup the db first...
  (map comment->sticker (commented-tracks (track-data "track-data.edn")))
  ;; Gave lots of errors, :err "MPD error: No such song\n" but did put lots of comments in the db
  ;; This is because I did some cleanup in the time between starting using emms and writing this
  ;; Some of the pathnames no longer match. When they do, it works fine.
  
  ;; Similarly for ratings:
  (map rating->sticker (rated-tracks (track-data "track-data.edn")))

  ;; This is working, but there is lots of information not needed in the edn file.
  ;; Lets remove all the tracks, albums and artists that don't contain any of the info we need.
  ;; For instance, remove all the tracks that have no play count or rating.

  (track-map->edn
    (setval [(compact MAP-VALS MAP-VALS ALL)
              #(and
                 (= nil (:play-count %))
                 (= nil (:rating %)))]
             NONE
             (track-data "track-data.edn"))
    "track-data-compact.edn")
  
  ;; now run the map on the compacted edn file
  (map rating->sticker (rated-tracks (track-data "track-data-compact.edn")))

  ;; Now lets do the play-counts
  (counted-tracks (track-data "track-data-compact.edn"))
  (count->stats (first (counted-tracks (track-data "track-data-compact.edn"))))
  (map count->stats (counted-tracks (track-data "track-data-compact.edn")))

  )
