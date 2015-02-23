(ns fcc.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [<!]]
            [rum]))

(defonce app-state (atom {:login-view true}))
;(swap! app-state assoc :login-view false)

(defonce event-bus (async/chan))

(defn send! [m]
  (fn [ev]
    (async/put! event-bus m)
    (.stopPropagation ev)))

(defmulti handle first)

(defmethod handle :default
  [[t d] app-state]
  (.warn js/console (str "No handler for " t)))

(defmethod handle :login
  [[t d] app-state]
  (swap! app-state assoc :current-user true))

(defmethod handle :show-login
  [[t d] app-state]
  (swap! app-state assoc :login-view true))

(defmethod handle :show-signup
  [[t d] app-state]
  (swap! app-state assoc :login-view false))

(defn long-enough? [n str]
  (> (count str) n))

(rum/defc input < rum/reactive [ref attrs]
  [:input (merge {:value @ref
                  :on-change #(reset! ref (.. % -target -value))}
                 attrs)])

(rum/defc validating-input < rum/reactive [ref test-fn attrs]
  (let [style (when (seq @ref)
                {:style {:border-color (if (test-fn @ref) "lime" "red")}})]
    (input ref (merge attrs style))))

(rum/defc signup < rum/reactive [data]
  [:.signup
   [:h3 "Signup"]
   (validating-input (rum/cursor data [:email])
                     #(long-enough? 5 %)
                     {:placeholder "email"})
   (validating-input (rum/cursor data [:password])
                     #(long-enough? 10 %)
                     {:placeholder "password" :type "password"})
   [:button.ui.signup {:on-click (send! [:signup (select-keys @data [:email :password])])}
    "Signup"]
   [:span.alt
    " or "
    [:button.linklike {:on-click (send! [:show-login])} "Login"]]])

(rum/defc login < rum/reactive [data]
  [:.login
   [:h3 "Login"]
   (validating-input (rum/cursor data [:email])
                     #(long-enough? 5 %)
                     {:placeholder "email"})
   (validating-input (rum/cursor data [:password])
                     #(long-enough? 10 %)
                     {:placeholder "password" :type "password"})
   [:button.ui.signup {:on-click (send! [:login (select-keys @data [:email :password])])}
    "Login"]
   [:span.alt
    " or "
    [:button.linklike {:on-click (send! [:show-signup])} "Signup"]]])

(rum/defc app < rum/reactive [state]
  (if (:current-user (rum/react state))
    [:div#app
     "Logged in!"]
    [:#auth
     (if (:login-view (rum/react state))
       (login (rum/cursor state [:login]))
       (signup (rum/cursor state [:signup])))]))

(defn init []

  (go-loop [v (<! event-bus)]
    (.log js/console (pr-str v))
    (handle v app-state)
    (recur (<! event-bus)))

  (add-watch app-state :global-watch
             (fn [_ _ _ n]
               (.log js/console (pr-str n)))))

  (rum/mount (app app-state) (.-body js/document))
