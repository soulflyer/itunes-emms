(ns util
  (:require [clojure.string :as s]
            [clojure.pprint :as pp])
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

(defn track-map->edn
  [track-map edn-file]
  (spit edn-file (with-out-str (pp/pprint track-map))))

(comment
  (truncate-location "file:///Users/iain/Music/Collection/TimoMaas/Loud/10 To Get Down.mp3")
  ;; => "TimoMaas/Loud/10 To Get Down.mp3"
  
  (url->string "file:///Users/iain/Music/Collection/TimoMaas/Loud/10%20To%20Get%20Down.mp3")
  ;; => "file:///Users/iain/Music/Collection/TimoMaas/Loud/10 To Get Down.mp3"
)
