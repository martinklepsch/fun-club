(ns fcc.app
  (:require [rum]))

(defonce app-state (atom {:heading "The Greek Alphabet"
                          :letters ["alpha" "beta" "gamma"]}))

(rum/defc app < rum/reactive [state]
  [:.our-list {:style {:margin "50px"}}
   [:h3 (:heading (rum/react state))]
   [:ol
    (for [x (:letters (rum/react state))]
      [:li x])]])

(defn update-state []
  (swap! app-state update-in [:letters] conj "delta"))

(defn init []
  (rum/mount (app app-state) (.-body js/document)))
