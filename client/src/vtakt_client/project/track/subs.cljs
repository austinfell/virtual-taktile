(ns vtakt-client.project.track.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::selected-track
 (fn [db _]
   (:selected-track db)))

(re-frame/reg-sub
 ::available-tracks
 (fn [db _]
   (:available-tracks db)))
