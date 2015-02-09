(ns chaf-light.util
  (:import goog.date.DateTime))

(defn current-time-iso8601 []
  (.toUTCIsoString (DateTime.)))
