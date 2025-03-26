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
   {:db (assoc db :selected-chromatic-chord
               (cond
                 ;; If the chord sent to the event is in the list of chords we know about,
                 ;; then the current selected chromatic chord to that value.
                 ((db :chromatic-chords) chord) chord
                 ;; Otherwise, if the chord is a triad, we just map it to the chromatic
                 ;; chord of a major. Hardware doesn't have any sophisticated state management
                 ;; that will remember that last chord that was selected.
                 (= chord :triad) :major
                 ;; This really shouldn't happen, but if it does, we will throw up our hands
                 ;; and set the chord to be a major.
                 :else :major))}))

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

