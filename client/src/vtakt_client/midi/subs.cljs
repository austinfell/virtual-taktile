(ns vtakt-client.midi.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::midi-outputs
 (fn [db _]
   (:midi-outputs db)))

(re-frame/reg-sub
 ::selected-midi-output
 (fn [db _]
   (get db :selected-midi-output)))

(re-frame/reg-sub
 ::selected-midi-channel-for-track
 (fn [db _]
   (try
     (get-in
      db
      [:current-project :patterns (db :active-pattern) :tracks (db :active-track) :midi-channel]
      0)
     (catch js/Error e 0))))
