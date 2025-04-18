(ns vtakt-client.keyboard.events
  (:require
   [re-frame.core :as re-frame]
   [clojure.core.async :as async]
   [vtakt-client.keyboard.core :as kb]))

(re-frame/reg-event-fx
 ::inc-keyboard-root
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :keyboard-root #(kb/transpose-note % 1))
            (assoc :pressed-notes #{}))}))

(re-frame/reg-event-fx
 ::dec-keyboard-root
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :keyboard-root #(kb/transpose-note % -1))
            (assoc :pressed-notes #{}))}))

(re-frame/reg-event-fx
 ::inc-keyboard-transpose
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :keyboard-transpose inc)
            (assoc :pressed-notes #{}))}))

(re-frame/reg-event-fx
 ::dec-keyboard-transpose
 (fn [{:keys [db]}]
   {:db (-> db
            (update :keyboard-transpose dec)
            (assoc :pressed-notes #{}))}))

(re-frame/reg-event-fx
 ::set-selected-chromatic-chord
 (fn [{:keys [db]} [_ chord]]
   (let [current-chromatic-chord (:selected-chromatic-chord db)
         current-diatonic-chord (:selected-diatonic-chord db)
         new-diatonic-chord (cond
                              (= chord :single-note) :single-note
                              ;; If changing from single-note to something else, set diatonic to triad
                              (and (= current-chromatic-chord :single-note)
                                   (not= chord :single-note)) :triad
                              ;; Otherwise keep current diatonic chord
                              :else current-diatonic-chord)]
     {:db (-> db
              (assoc :selected-chromatic-chord chord)
              (assoc :selected-diatonic-chord new-diatonic-chord))})))

(re-frame/reg-event-fx
 ::set-selected-diatonic-chord
 (fn [{:keys [db]} [_ chord]]
   (let [current-chromatic-chord (:selected-chromatic-chord db)
         current-diatonic-chord (:selected-diatonic-chord db)
         new-chromatic-chord (cond
                               ;; If setting diatonic to single-note, sync chromatic to single-note too
                               (= chord :single-note) :single-note
                               ;; If changing from single-note to something else, set chromatic to major
                               (and (= current-diatonic-chord :single-note)
                                    (not= chord :single-note)) :major
                               ;; Otherwise keep current chromatic chord
                               :else current-chromatic-chord)]
     {:db (-> db
              (assoc :selected-diatonic-chord chord)
              (assoc :selected-chromatic-chord new-chromatic-chord))})))

(re-frame/reg-event-fx
 ::set-scale
 (fn [{:keys [db]} [_ selected-scale]]
   {:db (assoc db :selected-scale selected-scale)}))

(re-frame/reg-event-fx
 ::set-keyboard-mode
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:db (assoc db :keyboard-mode keyboard-mode)}))

(def input-chan (async/chan 100))
(defn handle-note-event [source note-data event-type]
  (async/put! input-chan {:source source :note note-data :event event-type}))

(defn start-aggregator []
  (async/go-loop [current-state []]
    (when-let [input-event (async/<! input-chan)]
      (let [new-state (conj current-state input-event)]
        (println new-state)
        (recur new-state)))))
(start-aggregator)

(re-frame/reg-event-fx
 ::set-pressed-notes
 (fn [{:keys [db]} [_ notes]]
   (handle-note-event :keyboard {:name :c :octave 4} :on)
   {:db (assoc db :pressed-notes notes)}))
