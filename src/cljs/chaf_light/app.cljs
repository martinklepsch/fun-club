(ns chaf-light.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [<!]]
            [chaf-light.async :as as]
            [chaf-light.firebase :as fb]
            [chaf-light.util :as util]
            [rum])
  (:import goog.date.DateTime))

(defonce app-state (atom {:login-view true}))

;; (defn async-mixin [read-chan callback]
;;   {:init {:chans {:mounted (async/chan)}}
;;    :will-unmont (fn [{{mounted :mounted} :chans}]
;;                   (async/close! mounted))
;;    :did-mount (fn [{{mounted :mounted} :chans}]
;;                 (go-loop []
;;                   (when-some [[v ch] (async/alts! [read-chan mounted])]
;;                     (callback v)
;;                     (recur))))})

(rum/defc gravatar [email]
  (let [s (if email (util/md5 email) "not-found")]
    [:img.avatar {:src (str "http://www.gravatar.com/avatar/" s "?s=30&d=retro")}]))

(rum/defc input < rum/reactive [ref attrs]
  [:input (merge {:type "text"
                  :value @ref
                  :on-change #(reset! ref (.. % -target -value))}
                 attrs)])

(rum/defc signup < rum/reactive [data]
  [:.signup.sc-form
   (if (:error (rum/react data))
     [:span.error (:error (rum/react data))])
   (input (rum/cursor data [:email]) {:placeholder "Email"})
   (input (rum/cursor data [:password]) {:placeholder "Password" :type "password"})
   [:button.ui.signup {:on-click (as/send! [:account/new (select-keys @data [:email :password])])}
    "Signup"]
   [:span.alt
    " or "
    [:button.linklike {:on-click (as/send! [:ui/show-login])} "Login"]]])

(rum/defc login < rum/reactive [data]
  [:.login.sc-form
   (if (:error (rum/react data))
     [:span.error (:error (rum/react data))])
   (input (rum/cursor data [:email]) {:placeholder "Email"})
   (input (rum/cursor data [:password]) {:placeholder "Password" :type "password"})
   [:button.ui.signup {:on-click (as/send! [:session/new (select-keys @data [:email :password])])}
    "Login"]
   [:span.alt
    " or "
    [:button.linklike {:on-click (as/send! [:ui/show-signup])} "Signup"]]])

(rum/defc current-user [user]
  [:#current-user.cf
   (gravatar (-> user :password :email))
   [:span (-> user :password :email)]
   [:button.ui.logout {:on-click (as/send! [:session/destroy])}
    "Logout"]])

(rum/defc message-view < rum/reactive [m]
  [:.message
   (gravatar (-> m :user :email))
   [:span.message-body
    (:body m)]])

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
                     :on-key-down (textarea-submit #(async/put! as/event-bus [:message/new %]))}
                 attrs)])

(rum/defc message-form [user]
  [:.new-message-wrap
   [:.new-message
    (gravatar (-> user :password :email))
    (textarea {:placeholder "Type new message..."})]])

(def scroll-on-update
  {:did-update (fn [_]
                 (.log js/console "scroll!")
                 (.scrollBy js/window 0 10000))})

(rum/defc messages-view < rum/reactive scroll-on-update [messages]
  [:#messages
   (when (seq messages)
     (for [m messages]
       (rum/with-props message-view m :rum/key (:ts m))))])

(defn denormalize-message-data [state]
  (into (sorted-set-by util/ts)
        (for [m (:messages state)]
          (assoc m :user (get-in state [:users (:uid m)])))))

(rum/defc app < rum/reactive [state]
  (if (:current-user (rum/react state))
    [:div#app
     (current-user (:current-user (rum/react state)))
     (messages-view (denormalize-message-data (rum/react state)))
     (message-form (:current-user (rum/react state)))]
    [:#auth
     (if (:login-view (rum/react state))
       (login (rum/cursor state [:login]))
       (signup (rum/cursor state [:signup])))]))

(defn init []
  (fb/get-auth as/event-bus)
  (fb/user-feed as/event-bus)
  (fb/message-feed as/event-bus)

  (let [dbg (async/chan)]
    (async/tap as/event-feed dbg)
    (go-loop [v (<! dbg)]
      ;; (.log js/console (pr-str v))
      (recur (<! dbg))))

  (let [bus (async/chan)]
    (async/tap as/event-feed bus)
    (go-loop []
      (as/handle (<! bus) app-state)
      (recur)))

  (add-watch app-state :global-watch
             (fn [_ _ _ n]
               (.log js/console (pr-str (:users n)))))

  (rum/mount (app app-state) (.-body js/document)))
