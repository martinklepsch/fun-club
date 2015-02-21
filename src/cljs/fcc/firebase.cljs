(ns fcc.firebase
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! <!]]
            [matchbox.core :as mb]
            [matchbox.async :as mba]))

(defonce fb (mb/connect "https://fun-club-chat.firebaseio.com/"))

(defn get-auth [ch]
  (if-let [auth (.getAuth fb)]
    (put! ch [:session/created (js->clj auth :keywordize-keys true)])))

(defn de-authenticate []
  (.unauth fb))

(defn login-user [{:keys [email password] :as userdata} ch]
  (.authWithPassword fb
    (clj->js userdata)
    (fn [error auth]
      (if error
        (put! ch [:account/login-error (.-message error)])
        (put! ch [:session/created (js->clj auth :keywordize-keys true)])))))

(defn create-user [{:keys [email password] :as userdata} ch]
  (.createUser fb
    (clj->js userdata)
    (fn [error]
      (if error
        (put! ch [:account/signup-error (.-message error)])
        (put! ch [:account/created userdata])))))

(defn ensure-user-persisted [auth ch]
  (let [uid  (:uid auth)
        info {:email    (-> auth :password :email)
              :provider (-> auth :provider)}
        res  (mba/deref-in< fb [:users uid])]
    (go
      (let [user (<! res)]
        (when-not user
          (mb/reset-in! fb [:users uid] info)
          (put! ch [:account/persisted uid]))))))

;; (defn on [ref type cb]
;;   (let [ev-type (-> type name (clojure.string/replace "-" "_"))
;;         callb   #(cb (js->clj (.val %) :keywordize-keys true))]
;;     (.off ref ev-type callb) ; ensure idempotency
;;     (.on ref ev-type callb)))

(defn message-feed [ch]
  (mb/listen-to fb [:messages] :child-added #(put! ch [:message/in (second %)])))

(defn user-feed [ch]
  (mb/listen-to fb [:users] :child-added
                #(put! ch [:user/in (merge (second %) {:uid (first %)})])))
  ;; (let [ref (pani/walk-root fb [:users])]
  ;;   (on ref :child-added #(put! ch [:user/in %]))))

(defn save-message [message]
  (mb/conj-in! fb [:messages] message))
