(ns chaf-light.firebase
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! <!]]
            [pani.cljs.core :as pani]))

(defonce fb (js/Firebase. "https://chaf-light.firebaseio.com/"))

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
  (let [uid (:uid auth)
        res (pani/get-in fb [:users uid])]
    (go
      (let [user (<! res)]
        (when-not user
          (pani/set! fb [:users uid] auth)
          (put! ch [:account/persisted uid]))))))

(defn on [ref type cb]
  (let [ev-type (-> type name (clojure.string/replace "-" "_"))
        callb   #(cb (js->clj (.val %) :keywordize-keys true))]
    (.off ref ev-type callb) ; ensure idempotency
    (.on ref ev-type callb)))

(defn message-feed [ch]
  (let [ref (pani/walk-root fb [:messages])]
    (on ref :child-added #(put! ch [:message/in %]))))

(defn save-message [message]
  (pani/push! fb [:messages] message))
;; (go (.log js/console (<! (pani-get-in fb [:users "simplelogin:10"]))))
;; (.log js/console nil)
