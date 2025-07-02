(ns vtakt-client.project.track.events
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-active-track
 (fn [db [_ track]]
   (assoc db :active-track track)))
