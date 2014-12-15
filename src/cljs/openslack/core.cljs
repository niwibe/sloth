(ns openslack.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cats.core :refer [mlet-with]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [weasel.repl :as ws-repl]
            [openslack.routing :refer [start-history!]]
            [openslack.state :as st]
            [openslack.xmpp :as xmpp]
            [openslack.async :as async]
            [openslack.views :as views]
            [cats.core :as m]
            [cats.monad.either :as either]))

;; Enable println

(enable-console-print!)
(add-watch st/state :log (fn [_ _ _ newval]
                           (println "State transition! ")
                           (println newval)))

;; XMPP

(def xmpp-config {:jid "kim@niwi.be"
                  :password "korea"
                  :transports ["bosh"]
                  :boshURL "http://niwi.be:5280/http-bind"})
(def client (xmpp/create-client xmpp-config))

(set! js/window.cl client)
(swap! st/state assoc :client client)

;; Start XMPP session

(go
  (let [mv (<! (mlet-with async/either-pipeline-monad
                [user (xmpp/start-session client)
                 roster (xmpp/get-roster client)
                 room (xmpp/join-room client "testroom@conference.niwi.be" "kim")]
                (m/return {:user user, :roster roster, :room room})))]
    (when (either/right? mv)
      (xmpp/send-presence client)
      (xmpp/update-capabilities client)
      (let [{:keys [user roster room]} (either/from-either mv)]
        (swap! st/state assoc :user user)
        (swap! st/state assoc :roster roster)
        (swap! st/state assoc :features (xmpp/get-features client))
        (swap! st/state st/join-room room)))))

; TODO: roster-updating process
(def roster-updates-chan (xmpp/roster-updates client))
(go (loop []
      (let [rupdate (<! roster-updates-chan)
            subscription (:subscription rupdate)
            jid (:jid rupdate)
            bare (:bare jid)]
        (case (:subscription rupdate)
          :none :none ; TODO: fill this in
          :both :both
          :to   :to
          :from :from))
      (recur)))

; Chat updating process
(def chats-chan (xmpp/chats client))
(go-loop [chat (<! chats-chan)]
 (swap! st/state st/add-chat chat)
 (recur (<! chats-chan)))

; TODO: chat-states updating process
(def chat-states-chan (xmpp/chat-states client))
(go-loop [chat-state (<! chat-states-chan)]
  (println "chat-state: " chat-state)
  (recur (<! chat-states-chan)))


; TODO: subscription-managing process
(def subscriptions-chan (xmpp/subscriptions client))
(def unsubscriptions-chan (xmpp/unsubscriptions client))

;; Enable browser enabled repl.
;; (ws-repl/connect "ws://localhost:9001")

(defn main
  []
  (start-history!)
  (om/root views/app st/state {:target (js/document.querySelector "#app")})
)

(main)
