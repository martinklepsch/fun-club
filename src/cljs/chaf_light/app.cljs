(ns chaf-light.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [<!]]
            [chaf-light.firebase :as fb]
            [chaf-light.util :as util]
            [rum])
  (:import goog.date.DateTime))

(defonce app-state (atom {}))
(defonce event-bus (async/chan))
(def event-feed (async/mult event-bus))

(defn send! [m]
  (fn [ev]
    (async/put! event-bus m)
    (.stopPropagation ev)))

;; (defn async-mixin [read-chan callback]
;;   {:init {:chans {:mounted (async/chan)}}
;;    :will-unmont (fn [{{mounted :mounted} :chans}]
;;                   (async/close! mounted))
;;    :did-mount (fn [{{mounted :mounted} :chans}]
;;                 (go-loop []
;;                   (when-some [[v ch] (async/alts! [read-chan mounted])]
;;                     (callback v)
;;                     (recur))))})

(rum/defc input < rum/reactive [ref attrs]
  [:input (merge {:type "text"
                  :value @ref
                  :style {:width 100}
                  :on-change #(reset! ref (.. % -target -value))}
                 attrs)])

(rum/defc signup < rum/reactive [data]
  [:.signup-form
   (if (:error (rum/react data))
     [:span.error (:error (rum/react data))])
   (input (rum/cursor data [:email]) {:placeholder "Email"})
   (input (rum/cursor data [:password]) {:placeholder "Password" :type "password"})
   [:button.signup {:on-click (send! [:account/new (select-keys @data [:email :password])])}
    "Signup"]])

(rum/defc login < rum/reactive [data]
  [:.login-form
   (if (:error (rum/react data))
     [:span.error (:error (rum/react data))])
   (input (rum/cursor data [:email]) {:placeholder "Email"})
   (input (rum/cursor data [:password]) {:placeholder "Password" :type "password"})
   [:button.signup {:on-click (send! [:session/new (select-keys @data [:email :password])])}
    "Login"]])

(rum/defc current-user [user]
  [:#current-user
   [:p "Current user: " (-> user :auth :uid)]
   [:p "Email address: " (-> user :password :email)]
   [:button.signup {:on-click (send! [:session/destroy])}
    "Logout"]])

(rum/defc message-view < rum/reactive [m]
  [:.message
   [:span {:style {:color :red}}
    (:uid m) "said:"]
   (:body m)])

(defn- textarea-submit [callback]
  (fn [e]
    (if (and (== (.-keyCode e) 13) ;; enter
             (not (.-shiftKey e))) ;; no shift
      (do
        (callback (.. e -target -value))
        (set! (.. e -target -value) "")
        (.preventDefault e)))))

(rum/defc textarea < rum/reactive [attrs]
  [:textarea (merge {:autofocus true
                     :on-key-down (textarea-submit #(async/put! event-bus [:message/new %]))}
                 attrs)])

(rum/defc message-form []
  (textarea {:placeholder "Type new message"}))

(rum/defc messages-view < rum/reactive [messages]
  [:#messages
   (when (seq @messages)
     (for [m @messages]
       (rum/with-props message-view m :rum/key (:ts m))))
   (message-form)])

(rum/defc app < rum/reactive [state]
  (if (:current-user (rum/react state))
    [:div
     (current-user (:current-user (rum/react state)))
     (messages-view (rum/cursor state [:messages]))]
    [:#auth
     (signup (rum/cursor state [:signup]))
     (login (rum/cursor state [:login]))]))

(defmulti handle first)

(defmethod handle :default
  [[t d]]
  (.warn js/console (str "No handler for " t)))

(defmethod handle :account/new
  [[t userdata]]
  (fb/create-user userdata event-bus))

(defmethod handle :account/signup-error
  [[t message]]
  (swap! app-state assoc-in [:signup :error] message))

(defmethod handle :account/created
  [[t userdata]]
  (fb/login-user userdata event-bus))

;; (defmethod handle :account/persisted
;;   [[t userdata]]
;;   (fb/login-user userdata event-bus))

(defmethod handle :session/new
  [[t userdata]]
  (fb/login-user userdata event-bus))

(defmethod handle :session/created
  [[t auth]]
  (fb/ensure-user-persisted auth event-bus)
  (swap! app-state assoc :current-user auth))

(defmethod handle :session/destroy
  [[t auth]]
  (fb/de-authenticate)
  (swap! app-state dissoc :current-user))

(defmethod handle :message/new
  [[t msg]]
  (let [uid     (-> @app-state :current-user :uid)
        message {:uid uid :body msg :ts (util/current-time-iso8601)}]
    (swap! app-state update-in [:messages] (fnil conj []) message)
    (fb/save-message message)))

(defn ts [x y]
  (< (:ts x) (:ts y)))

(defmethod handle :message/in
  [[t msg]]
  (swap! app-state update-in [:messages] (fnil conj (sorted-set-by ts)) msg))

(defn init []
  (fb/get-auth event-bus)
  (fb/message-feed event-bus)

  (let [dbg (async/chan)]
    (async/tap event-feed dbg)
    (go-loop [v (<! dbg)]
      (.log js/console (pr-str v))
      (recur (<! dbg))))

  (let [bus (async/chan)]
    (async/tap event-feed bus)
    (go-loop []
      (handle (<! bus))
      (recur)))

  (add-watch app-state :global-watch
             (fn [_ _ _ n]
               (.log js/console (pr-str (:messages n)))))

  (rum/mount (app app-state) (.-body js/document)))
