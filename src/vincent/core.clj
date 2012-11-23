(ns vincent.core
  (:use clojure.java.io)
  (:require digest)
  (:import (org.apache.commons.io FilenameUtils)
           (com.drew.imaging ImageMetadataReader)
           (com.drew.metadata.exif ExifSubIFDDirectory)))

(println "Hello I'm Vincent")

(def cwd
  (System/getProperty "user.dir"))

;;(defn create-file-agent [fpath]
  ;;(agent fpath))

;;(def photo-tree {})
;;
(def world (ref {}))

(defn file-id [fpath]
  (digest/md5 (as-file fpath)))

(defn file-ext [fpath]
  (FilenameUtils/getExtension fpath))

(defn file-creation-time [fpath]
    (-> fpath as-file ImageMetadataReader/readMetadata
      (.getDirectory  ExifSubIFDDirectory)
      (.getDate ExifSubIFDDirectory/TAG_DATETIME_ORIGINAL)))

; :year :month :day :leaf
(comment 
(defn file-tree-path )
  )

(defn add-world-node [w n]
  (alter w assoc n (ref {})))

(defn move-file [from to]
  (copy from to) (delete-file from))

(create-node
  (fpath newpath))


(defn optimaze
  [fpath]
  (let [ ftp (file-tree-path fpath)]
    (if (exists world ftp)
      (delete-file fpath)
      (apply move-file (create-node world fpath)))))

  
(println (file-id tf))






;;(def organize [fpath]
  ;;"method provide behavior of one particular file"
  ;;(let [ fi file-info(fpath)]











