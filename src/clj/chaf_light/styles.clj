(ns chaf-light.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]))

(defstyles screen
  (let [body (rule :body)
        auth (rule :#auth)]
    (body
     {:font-family "Helvetica Neue"
      :font-size   "16px"
      :line-height 1.5})
    (auth
     {:width  "400px"
      :border "1px gray solid"})))
