(ns fcc.app
  (:require [rum]))

(defonce app-state (atom {:login-view true}))
;(swap! app-state assoc :login-view false)

(rum/defc signup < rum/reactive [data]
  [:.signup
   [:h3 "Signup"]
   [:input {:placeholder "email"}]
   [:input {:placeholder "password" :type "password"}]
   [:button.ui.signup {:on-click #(js/alert (pr-str (select-keys @data [:email :password])))}
    "Signup"]
   [:span.alt
    " or "
    [:button.linklike {:on-click #(js/alert "show-login")} "Login"]]])

(rum/defc login < rum/reactive [data]
  [:.login
   [:h3 "Login"]
   [:input {:placeholder "email"}]
   [:input {:placeholder "password" :type "password"}]
   [:button.ui.signup {:on-click #(js/alert (pr-str (select-keys @data [:email :password])))}
    "Login"]
   [:span.alt
    " or "
    [:button.linklike {:on-click #(js/alert "show-signup")} "Signup"]]])

(rum/defc app < rum/reactive [state]
  (if (:current-user (rum/react state))
    [:div#app
     "Logged in!"]
    [:#auth
     (if (:login-view (rum/react state))
       (login (rum/cursor state [:login]))
       (signup (rum/cursor state [:signup])))]))

(defn init []

  (add-watch app-state :global-watch
             (fn [_ _ _ n]
               (.log js/console (pr-str n)))))

  (rum/mount (app app-state) (.-body js/document))
