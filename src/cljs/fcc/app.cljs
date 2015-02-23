(ns fcc.app
  (:require [rum]))

(def letters ["alpha" "beta" "gamma" "delta" "epsilon" "zeta" "eta"
              "theta" "iota" "kappa" "lambda" "mu" "nu" "xi" "omicron"
              "pi" "rho" "sigma" "tau" "upsilon" "phi" "chi" "psi" "omega"])

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

(defn update-state []
  (let [letter-count (count (:letters @app-state))]
    (if (< letter-count (count letters))
      (swap! app-state update-in [:letters] conj (nth letters letter-count)))))

(defn init []
  (rum/mount (app app-state) (.-body js/document))
  (.setInterval js/window update-state 300))
