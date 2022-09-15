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

(defn create-async-websocket-handler [req]
  (println "Got an async websocket connection!")
  (->
   (d/let-flow [socket (http/websocket-connection req)]
               (s/connect socket socket)
               (s/put-all! socket ["Hello" "World"])
               nil)
   (d/catch (fn [_] invalid-ws-connection))))

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