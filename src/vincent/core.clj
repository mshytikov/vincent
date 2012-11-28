(ns vincent.core
  (:gen-class)
  (:use clojure.java.io)
  (:require [clj-time.core :as cljt])
  (:require [clj-time.format :as cljtf])
  (:import (org.apache.commons.io FilenameUtils FileUtils)
           (com.drew.imaging ImageMetadataReader)
           (com.drew.metadata.exif ExifSubIFDDirectory)
           (org.joda.time DateTime)
           (java.util.zip Adler32)))

(println "Hello I'm Vincent")

(def sketch (atom {}))

(def allowed-ext ["jpg" "jpeg"])

(def cwd (System/getProperty "user.dir"))

(defn file-id [f]
  (.getValue ( FileUtils/checksum f (Adler32.))))

(defn file-ext [f]
  (-> f str FilenameUtils/getExtension .toLowerCase))

(defn vincent-file? [f]
  (some  #{(file-ext f)} allowed-ext))

(defn #^org.joda.time.DateTime file-creation-time [f]
  (DateTime.
    (-> f ImageMetadataReader/readMetadata
      (.getDirectory  ExifSubIFDDirectory)
      (.getDate ExifSubIFDDirectory/TAG_DATETIME_ORIGINAL))))


(defn file-name [t id ext]
  (let [prefix (cljtf/unparse (cljtf/formatter "dd__HH'h'_mm'm'_ss's'") t)
        postfix (str "__I" id "I_." ext)]
    (str prefix postfix)))



(defstruct mediafile :fpath :id :ext :year :month :fname)

(defn create-mediafile [f]
  (let [ctime (file-creation-time f)
        id (file-id f)
        fpath (str f)
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




;###### with side effects #####

(defn log [& more]
  (apply println  more))

(defn assoc-in-sketch [p leaf]
  (swap! sketch #(assoc-in % p (get-in % p leaf))))


(defn sketch-insert-mediafile [mf]
  (let [ nodes (mediafile-sketch-path mf)
        path (:fpath mf)]
    (when-not (get-in @sketch nodes)
      (let [vpath (vincent-path mf)]
        (if (-> vpath as-file .exists)
          (assoc-in-sketch nodes vpath)
          (assoc-in-sketch nodes path)))
      (= (get-in @sketch nodes) path))))


(defn move-file [from to]
  (let [i (as-file from)
        o (as-file to)]
    (when-not (.exists o)
      (make-parents o)
      (FileUtils/moveFile i o ))))



(defn move-media-file [mf]
  (let [from (:fpath mf)
        to (vincent-path mf)]
    (move-file from to)
    (log  "[MOVE]" from to)
    :processed))

(defn delete-media-file [mf]
  (let [p (:fpath mf) ]
    (delete-file p)
    (log  "[RM]" p)
    :processed))

(defn organize [f]
  (let [ mf (create-mediafile f)]
    (if (sketch-insert-mediafile mf)
      (send-off *agent* #'move-media-file)
      (send-off *agent* #'delete-media-file))
    mf))


(defn start []
  (let [new-photos-dir (FilenameUtils/concat cwd "new")
        ls (file-seq (file new-photos-dir))
        fs (filter vincent-file? ls)]
    (doall 
      (for [a (map agent fs)]
        (do
          (send-off a organize)
          a)))))


(defn works? [a]
  (not (or (agent-errors a) (= :processed (deref a)))))

(defn -main []
  (time 
    (let [media-agents (start)]
      (while (some works? media-agents) 
        (Thread/sleep 1000))))
  (shutdown-agents)
  (println "Ok"))
