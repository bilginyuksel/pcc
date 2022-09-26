(ns pcc
  (:require
   [compojure.core :as compojure :refer [GET]]
   [ring.middleware.params :as params]
   [compojure.route :as route]
   [aleph.http :as http]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.bus :as bus]))

(def invalid-ws-connection
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body "Invalid WebSocket connection."})

(defn create-websocket-handler [req]
  (println "Got a websocket connection!")
  (if-let [socket (try @(http/websocket-connection req)
                       (catch Exception e nil))]
    (s/connect socket socket)
    (invalid-ws-connection)))

;; chatrooms
(def chatrooms (bus/event-bus))

(defn create-async-websocket-handler [req]
  (println "Got an async websocket connection!")
  (->
   #_{:clj-kondo/ignore [:unresolved-symbol]}
   (d/let-flow [socket (http/websocket-connection req)]
               (d/let-flow [room (s/take! socket)]
                          ;; take all messages from the chatroom, and feed them to the client
                           (s/connect (bus/subscribe chatrooms room) socket)
                           ;; take all messages from the client, and publish it to the room 
                           (s/consume #(bus/publish! chatrooms room %)
                                      (->> socket
                                           (s/map #(str "text: " %))
                                           (s/buffer 100))))
               nil)
   (d/catch (fn [_] invalid-ws-connection))))

(defn create-chat-room [req] (println "Creating chat room!"))

(defn join-chat-room [req] (println "Joining chat room!"))

(def handler
  (params/wrap-params
   (compojure/routes
    (GET "/ws" [] create-async-websocket-handler))
   (route/not-found "no such page")))

(def server (http/start-server handler {:port 8080}))


;; (let [conn @(create-websocket-client "ws://localhost:8080/ws")]
  ;; (s/put-all! conn ["hello" "world"])
  ;; (s/put-all! conn ["something" "is" "happening"])

;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn)
;;   @(s/take! conn))

;; (.close server)