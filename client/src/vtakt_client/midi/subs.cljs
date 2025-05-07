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
   (:selected-midi-output db)))

(re-frame/reg-sub
 ::selected-midi-channel
 (fn [db _]
   (:selected-midi-channel db)))
