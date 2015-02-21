(ns chaf-light.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]))

(defstyles clearfix
  [:.cf [:&:before :&:after {:content "\" \""
                             :display "table"}]]
  [:.cf [:&:after {:clear "both"}]]
  [:.cf {:*zoom 1}])

(defstyles avatar
  [:.avatar {:border-radius "999px"
             :min-width "30px"
             :height "30px"
             :margin-right "1em"}])

(defstyles inputs
  [:button :input {:outline "none"
                   :transition ".4s"}]
  [:button {:background-color "white"
            :border "none"}]
  [:button.linklike {:border-bottom "1px dotted white"
                     :color "blue"
                     :padding 0}
   [:&:hover {:border-bottom "1px dotted blue"}]]
  [:button.ui :input {:border "2px solid #ddd"}]
  [:button.ui {:padding "2px 7px"}
   [:&:focus :&:hover {:border "2px solid #333"}]]
  [:input {:display "block"
           :padding "5px 7px"
           :margin-bottom "10px"
           :width "100%"}
   [:&:focus {:border "2px solid #333"}]])

(defstyles current-user
  [:#current-user {:display "-webkit-flex"}]
  [:#current-user {:display "flex"
                   :position "fixed"
                   :background "white"
                   :top 0
                   :align-items "center"
                   :width "100%"
                   :padding "15px"
                   :border-bottom "2px solid #ddd"}
   [:img {:margin-right "1em"}]
   [:.logout {:margin-left "auto"}]])

(defstyles messages
  [:#messages {:max-width "640px"
               :margin "64px auto 83px auto"
               :padding "15px"}
   [:.message {:display "-webkit-flex"}]
   [:.message {:display "flex"
               :margin-bottom "1em"}]])

(defstyles message-form
  [:.new-message-wrap {:position "fixed"
                       :background "white"
                       :bottom 0
                       :width "100%"
                       :border-top "1px solid #ddd"}]
  [:.new-message {:display "-webkit-flex"}]
  [:.new-message {:display "flex"
                  :padding "15px"
                  :max-width "640px"
                  :margin "0 auto"}
   [:textarea {:border "none"
               :resize "none"
               :width "100%"}
    [:&:focus {:outline "none"}]]])

(defstyles base
  [:* {:box-sizing "border-box"}]
  [:body
   {:font-family "Helvetica Neue"
    :font-size   "16px"
    :line-height 1.5}]
  [:#auth
   {:margin "4em auto"
    :width  "66%"}])

(defstyles screen
  base
  clearfix
  inputs
  current-user
  avatar
  messages
  message-form)
