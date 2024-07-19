(ns covers
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [file]]))

(defn make-covers
  "This version only adds the larger versions of the cover art. It could just as easily create them all."
  [directory]
  (println directory)
  (let [cover-file (str directory "/cover.jpg")
        has-cover (.exists (file cover-file))]
    (if has-cover
      (do (sh "magick" cover-file "-resize" "240x240" (str directory "/cover_large.jpg"))
          (sh "magick" cover-file "-resize" "480x480" (str directory "/cover_huge.jpg")))
      (str "No cover.jpg found in " directory))))

(defn make-artist-covers
  "Makes covers in every album subdirectory of <artist>. Artist must have a trailing / "
  [artist make-cover-fn]
  (map #(make-cover-fn (str artist %))
       (map
         #(.getName %)
         (filter
           #(.isDirectory %)
           (.listFiles (file artist))))))

(defn artists
  "Gives a list of artists folders found in <music-directory>"
  [music-directory]
  (map
    #(.getName %)
    (filter
      #(.isDirectory %)
      (.listFiles (file music-directory)))))

(defn make-all-covers
  "Does <make-covers-fn> in every album directory in <music-directory>"
  [music-directory make-cover-fn]
  (map #(make-artist-covers (str music-directory % "/") make-cover-fn)
       (artists music-directory)))

(comment
  (sh "ls")
  (make-covers "/Users/iain/Music/Collection/Compilations/Random-Swing")
  (map #(make-covers (str "/Users/iain/Music/Collection/Compilations/" %)))

  (make-artist-covers "/Users/iain/Music/Collection/Compilations/" make-covers)
  (artists "/Users/iain/Music/Collection/")

  (make-all-covers "/Users/iain/Music/Collection/" make-covers)
)
