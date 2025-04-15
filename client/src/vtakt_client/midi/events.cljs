(ns vtakt-client.midi.events
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::initialize-midi
 (fn [{:keys [db]} _]
   {:db (assoc db :midi-outputs {})}))

(re-frame/reg-event-fx
 ::add-midi-output
 (fn [{:keys [db]} [_ [id to-add-midi-output]]]
   {:db (update db :midi-outputs assoc id to-add-midi-output)}))

(re-frame/reg-event-fx
 ::remove-midi-output
 (fn [{:keys [db]} [_ id]]
   {:db (update db :midi-outputs dissoc id)}))
