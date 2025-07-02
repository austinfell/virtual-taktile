(ns vtakt-client.project.track.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-track
 (fn [db _]
   (:active-track db)))
