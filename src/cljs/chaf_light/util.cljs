(ns chaf-light.util
  (:require [goog.crypt :as crypt ])
  (:import goog.date.DateTime
           goog.crypt.Md5))

(defn ts [x y]
  (< (:ts x) (:ts y)))

(defn md5 [str]
  (let [c (Md5.)]
    (.update c str)
    (crypt/byteArrayToHex (.digest c))))

(defn current-time-iso8601 []
  (.toUTCIsoString (DateTime.)))
