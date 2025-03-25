(ns vtakt-client.keyboard.events
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.db :as db]
   [vtakt-client.keyboard.core :as kb]))

(re-frame/reg-event-fx
 ::inc-keyboard-root
 (fn [{:keys [db]} _]
   {:db (update db :keyboard-root #(kb/transpose-note % 1))}))

(re-frame/reg-event-fx
 ::dec-keyboard-root
 (fn [{:keys [db]} _]
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
 ::trigger-note
 (fn [{:keys [db]} [_ {:keys [name octave] :as note}]]
   (let [{:keys [selected-chord selected-scale chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (nil? note) {:db (assoc db :pressed-notes [])}
       (= selected-chord :off) {:db (update db :pressed-notes conj note)}
       (= selected-scale :chromatic) {:db (update db :pressed-notes concat (kb/build-chord (-> chords selected-chord name) octave))}
       :else {:db (update db :pressed-notes concat (kb/build-scale-chord (-> scales selected-scale transposed-root-name) note))}))))

