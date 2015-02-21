(ns fcc.async
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [<!]]
            [fcc.util :as util]
            [fcc.firebase :as fb]))

(defonce event-bus (async/chan))
(def event-feed (async/mult event-bus))

(defn send! [m]
  (fn [ev]
    (async/put! event-bus m)
    (.stopPropagation ev)))

(defmulti handle first)

(defmethod handle :default
  [[t d] app-state]
  (.warn js/console (str "No handler for " t)))

(defmethod handle :account/new
  [[t userdata] app-state]
  (fb/create-user userdata event-bus))

(defmethod handle :account/signup-error
  [[t message] app-state]
  (swap! app-state assoc-in [:signup :error] message))

(defmethod handle :account/login-error
  [[t message] app-state]
  (swap! app-state assoc-in [:login :error] message))

(defmethod handle :account/created
  [[t userdata] app-state]
  (fb/login-user userdata event-bus))

;; (defmethod handle :account/persisted
;;   [[t userdata]]
;;   (fb/login-user userdata event-bus))

(defmethod handle :session/new
  [[t userdata] app-state]
  (fb/login-user userdata event-bus))

(defmethod handle :session/created
  [[t auth] app-state]
  (fb/ensure-user-persisted auth event-bus)
  (swap! app-state dissoc :login auth)
  (swap! app-state dissoc :signup auth)
  (swap! app-state assoc :current-user auth))

(defmethod handle :session/destroy
  [[t auth] app-state]
  (fb/de-authenticate)
  (swap! app-state dissoc :current-user))

(defmethod handle :ui/show-signup
  [[t auth] app-state]
  (swap! app-state dissoc :login-view))

(defmethod handle :ui/show-login
  [[t auth] app-state]
  (swap! app-state assoc :login-view true))

(defmethod handle :message/new
  [[t msg] app-state]
  (let [uid     (-> @app-state :current-user :uid)
        message {:uid uid :body msg :ts (util/current-time-iso8601)}]
    (swap! app-state update-in [:messages] (fnil conj []) message)
    (fb/save-message message)))

(defmethod handle :message/in
  [[t msg] app-state]
  (swap! app-state update-in [:messages] (fnil conj (sorted-set-by util/ts)) msg))

(defmethod handle :user/in
  [[t user] app-state]
  (swap! app-state assoc-in [:users (:uid user)] user))
