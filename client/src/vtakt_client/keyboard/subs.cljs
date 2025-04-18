(ns vtakt-client.keyboard.subs
  (:require
   [vtakt-client.keyboard.core :as kb]
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::keyboard-root
 (fn [db _]
   (:keyboard-root db)))

(re-frame/reg-sub
 ::keyboard-transpose
 (fn [db _]
   (:keyboard-transpose db)))

(re-frame/reg-sub
 ::selected-scale
 (fn [db _]
   (:selected-scale db)))

(re-frame/reg-sub
 ::keyboard-mode
 (fn [db _]
   (:keyboard-mode db)))

(re-frame/reg-sub
 ::keyboard
 (fn [_]
   [(re-frame/subscribe [::selected-scale])
    (re-frame/subscribe [::keyboard-root])
    (re-frame/subscribe [::scales])
    (re-frame/subscribe [::keyboard-mode])
    (re-frame/subscribe [::keyboard-transpose])])
 (fn [[selected-scale keyboard-root scales keyboard-mode keyboard-transpose] _]
   (kb/map-notes
    (kb/filter-notes
     (if (= :chromatic keyboard-mode)
       (kb/create-chromatic-keyboard keyboard-root)
       (kb/create-folding-keyboard keyboard-root))
     (kb/create-note-predicate-from-collection
      (get-in scales [selected-scale (:name keyboard-root)])))
    #(kb/transpose-note % keyboard-transpose))))

(re-frame/reg-sub
 ::chromatic-keyboard
 (fn [_]
   [(re-frame/subscribe [::selected-scale])
    (re-frame/subscribe [::keyboard-root])
    (re-frame/subscribe [::scales])
    (re-frame/subscribe [::keyboard-mode])
    (re-frame/subscribe [::keyboard-transpose])])
 (fn [[selected-scale keyboard-root scales keyboard-mode keyboard-transpose] _]
   (kb/map-notes
    (kb/filter-notes
     (kb/create-chromatic-keyboard keyboard-root)
     (kb/create-note-predicate-from-collection
      (get-in scales [selected-scale (:name keyboard-root)])))
    #(kb/transpose-note % keyboard-transpose))))

(re-frame/reg-sub
 ::chromatic-chords
 (fn [db _]
   (:chromatic-chords db)))

(re-frame/reg-sub
 ::diatonic-chords
 (fn [db _]
   (:diatonic-chords db)))

(re-frame/reg-sub
 ::selected-chromatic-chord
 (fn [db _]
   (:selected-chromatic-chord db)))

(re-frame/reg-sub
 ::selected-diatonic-chord
 (fn [db _]
   (:selected-diatonic-chord db)))

(re-frame/reg-sub
 ::scales
 (fn [db _]
   (:scales db)))

(re-frame/reg-sub
 ::pressed-notes
 (fn [db _]
   (let [{:keys [selected-chromatic-chord selected-diatonic-chord selected-scale
                 diatonic-chords chromatic-chords scales
                 keyboard-root keyboard-transpose]} db
         transposed-root-name (:name (kb/transpose-note keyboard-root keyboard-transpose))
         scale (-> scales selected-scale transposed-root-name)
         chords (if (= selected-scale :chromatic)
                  (chromatic-chords selected-chromatic-chord)
                  (diatonic-chords selected-diatonic-chord))]
     (into #{} (mapcat #(kb/build-scale-chord scale % chords) (:pressed-notes db))))))
