(ns vtakt-client.events
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.db :as db]
   [vtakt-client.keyboard.core :as kb]
   ))

(re-frame/reg-event-fx
 ::navigate
 (fn [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

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

(re-frame/reg-event-fx
 ::inc-keyboard-transpose
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:db (update db :keyboard-transpose inc)}))

(re-frame/reg-event-fx
 ::dec-keyboard-transpose
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:db (update db :keyboard-transpose dec)}))

(re-frame/reg-event-fx
 ::set-chord
 (fn [{:keys [db]} [_ chord]]
   {:db (assoc db :selected-chord chord)}))

(re-frame/reg-event-fx
 ::set-scale
 (fn [{:keys [db]} [_ selected-scale]]
   {:db (assoc db :selected-scale selected-scale)}))

(re-frame/reg-event-fx
 ::set-keyboard-mode
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:db (assoc db :keyboard-mode keyboard-mode)}))

(re-frame/reg-event-fx
 ::set-pressed-notes
 (fn [{:keys [db]} [_ notes]]
   {:db (assoc db :pressed-notes notes)}))

(re-frame/reg-event-fx
 ::clear-pressed-notes
 (fn [{:keys [db]} _]
   {:db (assoc db :pressed-notes [])}))
