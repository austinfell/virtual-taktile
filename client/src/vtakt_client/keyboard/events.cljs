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

(re-frame/reg-event-fx
 ::trigger-note
 (fn [{:keys [db]} [_ {:keys [name octave] :as note}]]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale diatonic-chords chromatic-chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (nil? note)
       {:db (assoc db :pressed-notes [])}

       (= selected-scale :chromatic)
       {:db (update db :pressed-notes concat (kb/build-scale-chord
                                              (-> scales selected-scale transposed-root-name)
                                              note
                                              (chromatic-chords selected-chromatic-chord)))}

       :else
       {:db (update db :pressed-notes concat (kb/build-scale-chord
                                              (-> scales selected-scale transposed-root-name)
                                              note
                                              (diatonic-chords selected-diatonic-chord)))}))))

