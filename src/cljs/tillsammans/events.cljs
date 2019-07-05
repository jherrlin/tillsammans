(ns tillsammans.events
  (:require
   [re-frame.core :as re-frame]
   [tillsammans.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [chord.client :refer [ws-ch]]
   [cljs.core.async :as async :include-macros true]
   [clojure.spec.alpha :as s]
   [common.spec :as spec]
   [medley.core :as medley]))


(goog-define ws-url "ws://localhost:3449/ws")
(defonce send-chan (async/chan))
(declare handle-message)

;; Websocket stuff
(defn send-msg
  [msg]
  (async/put! send-chan msg))


(defn- send-msgs
  [svr-chan]
  (async/go-loop []
    (when-let [msg (async/<! send-chan)]
      (async/>! svr-chan msg)
      (recur))))


(defn- receive-msgs
  [svr-chan]
  (async/go-loop []
    (if-let [{:keys [message]} (<! svr-chan)]
      (do
        (js/console.log "john-debug receive-msgs:" message)
        (handle-message message)
        (recur))
      (println "Websocket closed"))))


(defn setup-websockets! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-url))]
      (if error
        (println "Something went wrong with the websocket!")
        (do
          (send-msgs ws-channel)
          (receive-msgs ws-channel))))))


(defmulti handle-message (fn [data] (:m-type data)))


(defmethod handle-message :default [data]
  (js/console.log "handle-message :default:" data))


(defmethod handle-message :server-init [data]
  (let [k ::spec/server-init]
    (if (s/valid? k data)
      (re-frame/dispatch [::set-words data])
      (s/explain k data))))


(defmethod handle-message :update [data]
  (do
    (js/console.log "john-debug: handle-message :move" data)
    (let [k ::spec/update]
      (if (s/valid? k data)
        (re-frame/dispatch [::update-word data])
        (s/explain k data)))))


(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))


(re-frame/reg-event-db
 ::set-words
 (fn [db [_ words]]
   (assoc db :words (:words words))))


(re-frame/reg-event-db
 ::update-word
 (fn [db [_ {:keys [id x y] :as word}]]
   (update-in db [:words id] #(assoc % :x x :y y))))



(re-frame/reg-fx
 ::ws-init
 (fn [_]
   (js/console.log "john-debug: ::ws-init")
   (setup-websockets!)))


(re-frame/reg-fx
 ::ws-send!
 (fn [msg]
   (send-msg msg)))


(re-frame/reg-cofx
  ::uuid
  (fn [cofx _]
    (assoc cofx :uuid (medley/random-uuid))))


(re-frame/reg-event-fx
 ::init-app
 [(re-frame/inject-cofx ::uuid)]
 (fn [{:keys [db now uuid] :as cofx} [_]]
   (let [data {:id uuid
               :m-type :client-init}]
     (if (s/valid? ::spec/client-init data)
       {::ws-init nil
        ::ws-send! data
        ;; :db (assoc db :user data :active-panel :chat)
        }
       (s/explain ::spec/client-init data)))))


(re-frame/reg-event-fx
 ::move
 (fn [{:keys [db] :as cofx} [_ id x y]]
   (let [data {:id id
               :m-type :move
               :x x
               :y y}]
     (if (s/valid? ::spec/move data)
       {::ws-send! data
        ;; :db (assoc db :user data :active-panel :chat)
        }
       (s/explain ::spec/move data)))))
