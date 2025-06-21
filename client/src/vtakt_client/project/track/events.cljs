(ns vtakt-client.project.track.events
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-selected-track
 (fn [db [_ track-number]]
   (assoc db :selected-track track-number)))
