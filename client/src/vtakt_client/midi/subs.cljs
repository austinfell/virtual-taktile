(ns vtakt-client.midi.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::midi-outputs
 (fn [db _]
   (:midi-outputs db)))

(re-frame/reg-sub
 ::selected-midi-output-for-track
 (fn [db _]
   (get-in db [:per-track-midi-data (:selected-track db) :midi-output])))

(re-frame/reg-sub
 ::selected-midi-channel-for-track
 (fn [db _]
   (get-in db [:per-track-midi-data (:selected-track db) :midi-channel])))


