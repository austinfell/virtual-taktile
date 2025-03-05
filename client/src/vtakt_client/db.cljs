(ns vtakt-client.db
  (:require
   [vtakt-client.components.keyboard :as kb]))

(def default-db
  {:name "re-frame"
   :keyboard-root (kb/create-note :c 4)})
