(ns vtakt-client.keyboard.events
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.db :as db]
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
 (fn [{:keys [db]} [_ keyboard-mode]]
   {:db (-> db
            (update :keyboard-transpose inc)
            (assoc :pressed-notes #{}))}))

(re-frame/reg-event-fx
 ::dec-keyboard-transpose
 (fn [{:keys [db]} [_ keyboard-mode]]
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

(re-frame/reg-event-fx
 ::trigger-physical-note
 (fn [{:keys [db]} [_ note]]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale diatonic-chords chromatic-chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (:nil? note)
       {:db (update db :pressed-physical-notes #{})}
       (= selected-scale :chromatic)
       {:db (update db :pressed-physical-notes into (kb/build-scale-chord
                                              ;; The selected scale needs transposition. The note passed in
                                              ;; is already derived from the keyboard data structure which
                                              ;; is itself transposed.
                                            (-> scales selected-scale transposed-root-name)
                                            note
                                            (chromatic-chords selected-chromatic-chord)))}

       :else
       {:db (update db :pressed-physical-notes into (kb/build-scale-chord
                                              ;; The selected scale needs transposition. The note passed in
                                              ;; is already derived from the keyboard data structure which
                                              ;; is itself transposed.
                                            (-> scales selected-scale transposed-root-name)
                                            note
                                            (diatonic-chords selected-diatonic-chord)))}))))

(re-frame/reg-event-fx
 ::untrigger-physical-note
 (fn [{:keys [db]} [_ note]]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale diatonic-chords chromatic-chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (:nil? note)
       {:db db}
       (= selected-scale :chromatic)
       (do
         (println (:pressed-physical-notes db))
         {:db (update db :pressed-physical-notes clojure.set/difference (kb/build-scale-chord
                                                               (-> scales selected-scale transposed-root-name)
                                                               note
                                                               (chromatic-chords selected-chromatic-chord)))})
       :else
       {:db (update db :pressed-physical-notes clojure.set/difference (kb/build-scale-chord
                                                               (-> scales selected-scale transposed-root-name)
                                                               note
                                                               (diatonic-chords selected-diatonic-chord)))}))))


(re-frame/reg-event-fx
 ::trigger-visual-note
 (fn [{:keys [db]} [_ note]]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale diatonic-chords chromatic-chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (:nil? note)
       {:db (update db :pressed-visual-notes #{})}
       (= selected-scale :chromatic)
       {:db (update db :pressed-visual-notes into (kb/build-scale-chord
                                              ;; The selected scale needs transposition. The note passed in
                                              ;; is already derived from the keyboard data structure which
                                              ;; is itself transposed.
                                            (-> scales selected-scale transposed-root-name)
                                            note
                                            (chromatic-chords selected-chromatic-chord)))}

       :else
       {:db (update db :pressed-visual-notes into (kb/build-scale-chord
                                              ;; The selected scale needs transposition. The note passed in
                                              ;; is already derived from the keyboard data structure which
                                              ;; is itself transposed.
                                            (-> scales selected-scale transposed-root-name)
                                            note
                                            (diatonic-chords selected-diatonic-chord)))}))))

(re-frame/reg-event-fx
 ::untrigger-visual-note
 (fn [{:keys [db]} [_ note]]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale diatonic-chords chromatic-chords scales keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))]
     (cond
       (:nil? note)
       {:db db}
       (= selected-scale :chromatic)
       (do
         (println (:pressed-visual-notes db))
         {:db (update db :pressed-visual-notes clojure.set/difference (kb/build-scale-chord
                                                               (-> scales selected-scale transposed-root-name)
                                                               note
                                                               (chromatic-chords selected-chromatic-chord)))})
       :else
       {:db (update db :pressed-visual-notes clojure.set/difference (kb/build-scale-chord
                                                               (-> scales selected-scale transposed-root-name)
                                                               note
                                                               (diatonic-chords selected-diatonic-chord)))}))))
