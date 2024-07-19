(ns sticker
  (:require [util :refer [truncate-location]]
            [clojure.java.shell :as sh]))

(defn comment->sticker
  "Uses mpc to set the value of comment for a track in the sticker db"
  [track]
  (let [comments (:comments track)
        filename (truncate-location (:location track))]
    (print filename)
    (println (:err (sh/sh "mpc" "sticker" filename "set" "comment" comments)))))

(defn rating->sticker
  "Uses mpc to set the value of rating for a track in the sticker db"
  [track]
  (let [itunes-rating   (/ (:rating track) 20)   ;; 0 to 5
        raw-mpd-rating  (* itunes-rating 2)      ;; 0 to 10
        mpd-rating      (if (= 2 raw-mpd-rating)
                          1
                          raw-mpd-rating)
        filename        (truncate-location (:location track))
        command         (sh/sh "mpc" "sticker" filename "set" "rating" (str mpd-rating))]

    (when (> (count (:err command)) 0)
      (print (str filename " " (:err command))))))
