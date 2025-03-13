(ns vtakt-client.subs
  (:require
   [vtakt-client.components.keyboard :as kb]
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::selected-scale
 (fn [db _]
   (:selected-scale db)))

(re-frame/reg-sub
 ::keyboard-root
 (fn [db _]
   (:keyboard-root db)))

(re-frame/reg-sub
 ::keyboard-transpose
 (fn [db _]
   (:keyboard-transpose db)))

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
 ::internal-chord
 (fn [db _]
   (:selected-chord db)))

(re-frame/reg-sub
 ::internal-chords
 (fn [db _]
   (:chords db)))

(re-frame/reg-sub
 ::chords
 (fn [_]
   [(re-frame/subscribe [::selected-scale])
    (re-frame/subscribe [::internal-chords])])
 (fn [[selected-scale internal-chords] _]
   (if (= selected-scale :chromatic)
     (into {:off {}} internal-chords)
     {:off {} :on {}})))

(re-frame/reg-sub
 ::selected-chord
 (fn [_]
   [(re-frame/subscribe [::selected-scale])
    (re-frame/subscribe [::internal-chord])
    (re-frame/subscribe [::chords])])
 (fn [[selected-scale internal-chord chords] _]
   (if (= selected-scale :chromatic)
     (if (contains? (set (keys chords)) internal-chord)
       internal-chord
       :major)
     (if (= :off internal-chord) :off :on))))


(re-frame/reg-sub
 ::scales
 (fn [db _]
   (:scales db)))
