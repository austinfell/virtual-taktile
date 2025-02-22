(ns vtakt-client.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.views :as views]
   [vtakt-client.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
