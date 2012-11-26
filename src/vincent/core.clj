(ns vincent.core
  (:use clojure.java.io)
  (:require [clj-time.core :as cljt])
  (:require [clj-time.format :as cljtf])
  (:require digest)
  (:import (org.apache.commons.io FilenameUtils)
           (com.drew.imaging ImageMetadataReader)
           (com.drew.metadata.exif ExifSubIFDDirectory)
           (org.joda.time DateTime)))

(println "Hello I'm Vincent")

(def cwd
  "/tmp/vincent")
;  (System/getProperty "user.dir"))


(def sketch (atom {}))

(defn file-id [fpath]
  (digest/md5 (as-file fpath)))

(defn file-ext [fpath]
  (FilenameUtils/getExtension fpath))

(defn #^org.joda.time.DateTime file-creation-time [fpath]
  (DateTime.
    (-> fpath as-file ImageMetadataReader/readMetadata
      (.getDirectory  ExifSubIFDDirectory)
      (.getDate ExifSubIFDDirectory/TAG_DATETIME_ORIGINAL))))


(defn file-name [t id ext]
  (let [prefix (cljtf/unparse (cljtf/formatter "dd__HH'h'_mm'm'_ss's'") t)
       postfix (str "__I" id "I_." ext)]
  (str prefix postfix)))


(println (file-name (cljt/date-time 1986 10 04 19 3 27 456) "asdfasdf" "jpg"))


(defstruct mediafile :fpath :id :ext :year :month :fname)

(defn create-mediafile [fpath]
  (let [ctime (file-creation-time fpath)
        id (file-id fpath)
        ext (file-ext fpath)
        y  (cljt/year ctime)
        m  (.toString ctime "MMMM")
        fname (file-name ctime id ext)]
    (struct mediafile fpath id  ext y m fname)))


(defn mediafile-sketch-path [mf]
  (let [{:keys [year month day fname]} mf]
    [year month  fname]))

(defn vincent-path [mf]
  (reduce #(FilenameUtils/concat %1 (str %2) ) cwd (mediafile-sketch-path mf)))


(defn sketch-insert-mediafile [mf]
  (let [ nodes (mediafile-sketch-path mf)
        path (:fpath mf)]
    (swap! sketch #(assoc-in % nodes (get-in % nodes path) ))
    (= (get-in @sketch nodes) path)))


(defn move-file [from to]
  (let [i (as-file from)
        o (as-file to)]
    (when-not (.exists o)
      (make-parents o)
      (copy i o ))
    (delete-file i)))


(defn organize [mf]
  (let [ fpath (:fpath mf)]
    (if (sketch-insert-mediafile mf)
      ( move-file fpath (vincent-path mf))
      (delete-file fpath))
    mf))


;(def t (create-mediafile "/tmp/vincent/new/1.jpg"))
;(def t (create-mediafile "/tmp/vincent/new/2.jpg"))


(defn start []
  (let [new-photos-dir (FilenameUtils/concat cwd "new")]
    (doseq [f (file-seq (file new-photos-dir))]
      (send-off (create-file-agent f) organize))))

(defn create-file-agent [fpath]
  (agent (create-mediafile(fpath))))

(start)

