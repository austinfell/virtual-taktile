(ns vtakt-client.keyboard.events
  (:require
   [re-frame.core :as re-frame]
   [clojure.set :as set]
   [vtakt-client.keyboard.core :as kb]))

(re-frame/reg-event-fx
 ::inc-keyboard-root
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (-> db
            (update :keyboard-root #(kb/transpose-note % 1)))}))

(re-frame/reg-event-fx
 ::dec-keyboard-root
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (-> db
            (update :keyboard-root #(kb/transpose-note % -1)))}))

(re-frame/reg-event-fx
 ::inc-keyboard-transpose
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (-> db
            (update :keyboard-transpose inc))}))

(re-frame/reg-event-fx
 ::dec-keyboard-transpose
 (fn [{:keys [db]}]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (-> db
            (update :keyboard-transpose dec))}))

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
 ::clear-pressed-notes
 (fn [{:keys [db]} [_ note]]
   {:fx [[:dispatch [::update-triggered-notes]]]
    :db (assoc db :pressed-notes [])}))

(re-frame/reg-event-fx
 ::add-pressed-note
 (fn [{:keys [db]} [_ note]]
   {:fx [[:dispatch [::update-triggered-notes]]]
    :db (update db :pressed-notes conj note)}))

(re-frame/reg-event-fx
 ::remove-pressed-note
 (fn [{:keys [db]} [_ note]]
   {:fx [[:dispatch [::update-triggered-notes]]]
    :db (update db :pressed-notes #(filterv (fn [n] (not= n note)) %))}))

(re-frame/reg-event-fx
 ::update-triggered-notes
 (fn [{:keys [db]} _]
   (let [previous-sounded-notes (db :sounded-notes)
         current-pressed-notes (db :pressed-notes)
         {:keys [selected-chromatic-chord selected-diatonic-chord selected-scale
                 diatonic-chords chromatic-chords scales
                 keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))
         scale (-> scales selected-scale transposed-root-name)
         chords (if (= selected-scale :chromatic)
                  (chromatic-chords selected-chromatic-chord)
                  (diatonic-chords selected-diatonic-chord))
         new-triggered-notes (if (not= selected-chromatic-chord :single-note) [(last current-pressed-notes)] current-pressed-notes)
         new-sounded-notes (into #{} (mapcat #(kb/build-scale-chord scale % chords) new-triggered-notes))
         added-notes (set/difference new-sounded-notes previous-sounded-notes)
         removed-notes (set/difference previous-sounded-notes new-sounded-notes)
         note-on-messages (map (fn [note]
                                 {:type :note-on
                                  :channel 0
                                  :device "9RBYXR1hOVRyNrnlJra/Wvl53WPge8813quvp4JbZNo="
                                  :data {:note (kb/note->midi-number note) :velocity 80}})
                               added-notes)

         note-off-messages (map (fn [note]
                                  {:type :note-off
                                   :channel 0
                                   :device "9RBYXR1hOVRyNrnlJra/Wvl53WPge8813quvp4JbZNo="
                                   :data {:note (kb/note->midi-number note) :velocity 0}})
                                removed-notes)
         midi-messages (concat note-on-messages note-off-messages)]
     {:midi midi-messages
      :db (-> db
              (assoc :triggered-notes new-triggered-notes)
              (assoc :sounded-notes new-sounded-notes))})))
