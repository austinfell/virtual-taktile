(ns vtakt-server.dev
  (:require [vtakt-server.core :as v]))

(defn restart []
  (v/stop-server!)
  (v/start-server! 8002))

(defn reset-db []
  (reset! v/db-conn nil)
  (restart))

(restart)
(reset-db)

