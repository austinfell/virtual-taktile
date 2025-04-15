(ns vtakt-client.midi.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::midi-outputs
 (fn [db _]
   (:midi-outputs db)))
