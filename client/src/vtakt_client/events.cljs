(ns vtakt-client.events
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-fx
  ::navigate
  (fn [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-fx
 ::inc-keyboard-shift
 (fn [{:keys [db]} [_ keyboard-shift]]
   {:db (update db :keyboard-shift inc)}))

(re-frame/reg-event-fx
 ::dec-keyboard-shift
 (fn [{:keys [db]} [_ keyboard-shift]]
   {:db (update db :keyboard-shift dec)}))
