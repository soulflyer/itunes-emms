(ns stats
  (:require
   [util :refer [truncate-location]]
   [next.jdbc :as jdbc]
   [clojure.java.shell :as sh]))

(def db-spec
  {:dbtype "sqlite"
   :dbname "stats.db"})

(def ds (jdbc/get-datasource db-spec))

(defn count->stats
  "This version only detects missing tracks using mpc. As yet it doesn't touch the stats db"
  [track]
  (let [location (truncate-location (:location track))
        ;;track-data (jdbc/execute-one! ds ["select * from song where uri = ?" location])
        command (sh/sh "mpc" "sticker" location "get" "rating")]
    (when (re-find #"MPD error: No such song" (:err command) )
      (print (str location " " (:err command))))
    ;;(str (:name track) ": " (:play-count track) " : " location " :-: " (:song/play_count track-data))
    ))

(comment

  (def ds (jdbc/get-datasource db-spec))
  (jdbc/execute! ds ["select * from song"])
 
)
