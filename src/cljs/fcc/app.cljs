(ns fcc.app
  (:require [rum]))

(defonce app-state (atom {:heading "The Greek Alphabet"
                          :items ["Alpha" "Beta" "Gamma"]}))

(rum/defc app < rum/reactive [state]
  [:.our-list {:style {:margin "50px"}}
   [:h3 (:heading @state)]
   [:ol
    (for [x (:items (rum/react state))]
      [:li x])]])

(defn init []
  (rum/mount (app app-state) (.-body js/document)))
