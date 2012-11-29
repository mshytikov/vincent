(ns vincent.test.core
  (:use [vincent.core])
  (:use [clojure.test]))

(def created-at
  (org.joda.time.DateTime. 2012 1 24 11 8 13 org.joda.time.DateTimeZone/UTC))

(deftest test-file-name
   (is (= "24__11h_08m_13s__I123456I_.jpg" (file-name created-at 123456 "jpg"))))
