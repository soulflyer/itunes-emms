(ns main
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [clojure.string :as s]
    [clojure.pprint :as pp]
    [com.github.bdesham.clj-plist :as plist]
    [com.rpl.specter :refer [ALL MAP-KEYS MAP-VALS submap transform select selected? must setval NONE compact]])
  (:import
    (java.net URLDecoder)))

(defn key->keyword
  [key]
  (keyword (s/replace (s/lower-case key) #"\s+" "-")))

(defn url->string
  [url]
  (URLDecoder/decode url "UTF-8"))

(defn truncate-location
  "Locations are stored in the plist as file:// urls but mpc expects only the last part
of the path, containing just the artist/album/track"
  [pathname]
  (let [split (s/split pathname #"/")
        length (count split)
        artist (nth split (- length 3))
        album  (nth split (- length 2))
        track  (nth split (- length 1))]
    (str artist "/" album "/" track)))

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

(defn comment->sticker
  [track]
  (let [comments (:comments track)
        filename (truncate-location (:location track))]
    (print filename)
    (println (:err (sh/sh "mpc" "sticker" filename "set" "comment" comments)))))

(defn rating->sticker
  [track]
  (let [itunes-rating (/ (:rating track) 20)
        mpd-rating    (* itunes-rating 2)
        filename      (truncate-location (:location track))
        command       (sh/sh "mpc" "sticker" filename "set" "rating" (str mpd-rating))]
    ;; (print (str filename ": " itunes-rating " " mpd-rating))
    (when (> (count (:err command)) 0)
      (print (str filename " " (:err command))))))

(defn track-map->edn
  [track-map edn-file]
  (spit edn-file (with-out-str (pp/pprint track-map))))

(defn plist->edn
  [plist-file edn-file]
  (track-map->edn (track-map plist-file) edn-file))

;; (defn plist->edn [plist-file edn-file]
;;   (spit edn-file (with-out-str (pp/pprint (track-map plist-file)))))

(defn track-data
  [edn-file]
  (doall (edn/read-string (slurp edn-file))))

(defn rated-tracks
  [track-data]
  (select [MAP-VALS MAP-VALS ALL (selected? (must :rating))] track-data))

(comment
  (def track-data (edn/read-string (slurp "track-data.edn")))
  (def rated-tracks (select [MAP-VALS MAP-VALS ALL (selected? (must :rating))] (track-data "track-data.edn")))
  (map rating->sticker rated-tracks)

  (def no-unplayed (setval [MAP-VALS MAP-VALS ALL #( = nil (:play-count %))] NONE track-data))

  (def no-unplayed (setval [(compact MAP-VALS MAP-VALS ALL) #( = nil (:play-count %))] NONE track-data))
  
  (sh/sh "open" "/Users/iain/Music/Collection/TimoMaas/Loud/10 To Get Down.mp3")
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
  (def track-data (edn/read-string (slurp "track-data.edn")))
  (get-in track-data ["Yes" "Going For The One"])
  ;; or
  (get-in (track-data "track-data.edn") ["Yes" "Going For The One"])

  ;; List all the tracks that have comments
  (select [MAP-VALS MAP-VALS ALL (selected? (must :comments))] track-data)

  (def commented-tracks (select [MAP-VALS MAP-VALS ALL (selected? (must :comments))] track-data))
  (first commented-tracks)
  ;; =>
  ;; {:name "Hole In The Sky",
  ;; :location
  ;; "file:///Users/iain/Music/Collection/Shining/Animal/10 Hole In The Sky.mp3",
  ;; :comments "Meh"}

  (comment->sticker (first commented-tracks))
  ;; puts the first comment into the sticker db

  ;; Backup the db first...
  (map comment->sticker commented-tracks)
  ;; Gave lots of errors, :err "MPD error: No such song\n" but did put lots of comments in the db
  ;; This is because I did some cleanup in the time between starting using emms and writing this
  ;; Some of the pathnames no longer match. When they do, it works fine.
  
  (truncate-location "file:///Users/iain/Music/Collection/TimoMaas/Loud/10 To Get Down.mp3")
  ;; => "TimoMaas/Loud/10 To Get Down.mp3"
  
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3")
  ;; => "file:///Users/iain/Music/Collection/TimoMaas/Loud/10 To Get Down.mp3"

  ;;I edited the edn file to correct some paths. Did these to update all the comments and check for errors.
  (def track-data (edn/read-string (slurp "track-data.edn")))
  (def commented-tracks (select [MAP-VALS MAP-VALS ALL (selected? (must :comments))] track-data))
  (map comment->sticker commented-tracks)

  ;; Similarly for ratings:
  (map rating->sticker (rated-tracks (track-data "track-data.edn")))

  ;; This is working, but there is lots of information not needed in the edn file.
  ;; Lets remove all the tracks, albums and artists that don't contain any of the info we need.
  ;; For instance, remove all the tracks that have no play count.

  (track-map->edn
    (setval [(compact MAP-VALS MAP-VALS ALL) #( = nil (:play-count %))] NONE (track-data "track-data.edn"))
    "track-data-compact.edn")
  (map rating->sticker (rated-tracks (track-data "track-data-compact.edn")))
  ;; Ah. There are less errors now. This must be because some tracks have ratings but no play count...
  ;; Better adjust it to keep anything that has a rating or a play count

   (track-map->edn
     (setval [(compact MAP-VALS MAP-VALS ALL)
              #(and
                 (= nil (:play-count %))
                 (= nil (:rating %)))]
             NONE
             (track-data "track-data.edn"))
    "track-data-compact.edn")
   (map rating->sticker (rated-tracks (track-data "track-data-compact.edn")))
   
  )
