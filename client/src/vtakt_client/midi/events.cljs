(ns vtakt-client.midi.events
  (:require
   [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
 ::set-midi-outputs
 (fn [{:keys [db]} [_ midi-outputs]]
   {:db (-> db
            (assoc :midi-outputs midi-outputs)
            (update :selected-midi-output
                    #(if (get midi-outputs %)
                       %
                       (first (keys midi-outputs)))))}))

;; TODO - Long term, this is going to actually be a per track allocation.
;; That means selected midi output is going to be a lot more sophisticated than this.
;; We'll need to dynamically store track->midi mappings and then as set midi outputs is called,
;; we'll need to fix any selections that were rendered invalid... But we also probably want to
;; be smart about "resetting" once the midi device is back... Or maybe we let it stay in an
;; invalid state until the user explictly redefines it...
;;
;; AKA - TBD.
(reg-event-fx
 ::set-selected-midi-output
 (fn [{:keys [db]} [_ selected-midi-output]]
   {:db (assoc db :selected-midi-output selected-midi-output)}))

(reg-event-fx
 ::set-selected-midi-channel
 (fn [{:keys [db]} [_ selected-midi-channel]]
   {:db (assoc db :selected-midi-channel selected-midi-channel)}))

