(ns vtakt-client.keyboard.events
  (:require
   [re-frame.core :as re-frame]
   [clojure.set :as set]
   [vtakt-client.keyboard.chord :as chord]
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
     {:fx [[:dispatch [::clear-pressed-notes]]]
      :db (-> db
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
     {:fx [[:dispatch [::clear-pressed-notes]]]
      :db (-> db
              (assoc :selected-diatonic-chord chord)
              (assoc :selected-chromatic-chord new-chromatic-chord))})))

(re-frame/reg-event-fx
 ::set-scale
 (fn [{:keys [db]} [_ selected-scale]]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (assoc db :selected-scale selected-scale)}))

(re-frame/reg-event-fx
 ::set-keyboard-mode
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:fx [[:dispatch [::clear-pressed-notes]]]
    :db (assoc db :keyboard-mode keyboard-mode)}))

(defn calculate-midi-messages [db new-pressed-notes]
  (let [previous-sounded-notes (:sounded-notes db)
        {:keys [selected-chromatic-chord selected-diatonic-chord selected-scale
                diatonic-chords chromatic-chords scales
                keyboard-root keyboard-transpose]} db
        transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))
        scale (-> scales selected-scale transposed-root-name)
        chords (if (= selected-scale :chromatic)
                 (chromatic-chords selected-chromatic-chord)
                 (diatonic-chords selected-diatonic-chord))
        chord-mode? (not= selected-chromatic-chord :single-note)
        new-triggered-notes (cond
                              (empty? new-pressed-notes) []
                              chord-mode? [(last new-pressed-notes)]
                              :else new-pressed-notes)
        new-sounded-notes (into #{} (mapcat #(chord/build-scale-chord scale % chords) new-triggered-notes))
        added-notes (if chord-mode? new-sounded-notes (set/difference new-sounded-notes previous-sounded-notes))
        removed-notes (if chord-mode? previous-sounded-notes (set/difference previous-sounded-notes new-sounded-notes))
        note-on-messages (map (fn [note]
                                {:type :note-on
                                 :channel (get-in db [:per-track-midi-data (:selected-track db) :midi-channel])
                                 :device (get-in db [:per-track-midi-data (:selected-track db) :midi-output])
                                 :data {:note (kb/note->midi-number note) :velocity 80}})
                              added-notes)
        note-off-messages (map (fn [note]
                                 {:type :note-off
                                  :channel (get-in db [:per-track-midi-data (:selected-track db) :midi-channel])
                                  :device (get-in db [:per-track-midi-data (:selected-track db) :midi-output])
                                  :data {:note (kb/note->midi-number note) :velocity 0}})
                               removed-notes)
        midi-messages (concat note-off-messages note-on-messages)]
    (println midi-messages)
    {:midi-messages midi-messages
     :triggered-notes new-triggered-notes
     :sounded-notes new-sounded-notes}))

(re-frame/reg-event-fx
 ::add-pressed-note
 (fn [{:keys [db]} [_ note]]
   (let [new-pressed-notes (conj (:pressed-notes db) note)
         {:keys [midi-messages triggered-notes sounded-notes]} (calculate-midi-messages db new-pressed-notes)]
     {:midi midi-messages
      :db (-> db
              (assoc :pressed-notes new-pressed-notes)
              (assoc :triggered-notes triggered-notes)
              (assoc :sounded-notes sounded-notes))})))

(re-frame/reg-event-fx
 ::remove-pressed-note
 (fn [{:keys [db]} [_ note]]
   (let [new-pressed-notes (filterv (fn [n] (not= n note)) (:pressed-notes db))
         old-triggered-notes (:triggered-notes db)
         {:keys [midi-messages triggered-notes sounded-notes]} (calculate-midi-messages db new-pressed-notes)
         ;; Only include MIDI effects if triggered notes have changed
         fx (if (not= old-triggered-notes triggered-notes)
              {:midi midi-messages
               :db (-> db
                       (assoc :pressed-notes new-pressed-notes)
                       (assoc :triggered-notes triggered-notes)
                       (assoc :sounded-notes sounded-notes))}
              {:db (-> db
                       (assoc :pressed-notes new-pressed-notes)
                       (assoc :triggered-notes triggered-notes)
                       (assoc :sounded-notes sounded-notes))})]
     fx)))

(re-frame/reg-event-fx
 ::clear-pressed-notes
 (fn [{:keys [db]} _]
   (let [new-pressed-notes []
         {:keys [midi-messages triggered-notes sounded-notes]} (calculate-midi-messages db new-pressed-notes)]
     {:midi midi-messages
      :db (-> db
              (assoc :pressed-notes new-pressed-notes)
              (assoc :triggered-notes triggered-notes)
              (assoc :sounded-notes sounded-notes))})))
