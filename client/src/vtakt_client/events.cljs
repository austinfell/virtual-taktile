(ns vtakt-client.events
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.db :as db]
   [vtakt-client.components.keyboard :as kb]
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
 ::inc-keyboard-root
 (fn [{:keys [db]} [_ keyboard-shift]]
   {:db (update db :keyboard-root #(kb/transpose-note % 1))}))

(re-frame/reg-event-fx
 ::dec-keyboard-root
 (fn [{:keys [db]} [_ keyboard-shift]]
   {:db (update db :keyboard-root #(kb/transpose-note % -1))}))
