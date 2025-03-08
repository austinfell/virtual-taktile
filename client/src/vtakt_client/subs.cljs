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
 ::keyboard-root
 (fn [db _]
   (:keyboard-root db)))

(re-frame/reg-sub
 ::keyboard
 (fn [_]
   [(re-frame/subscribe [::selected-scale]) (re-frame/subscribe [::keyboard-root]) (re-frame/subscribe [::scales])])
 (fn [[selected-scale keyboard-root scales] _]
   (kb/filter-notes
    (kb/create-chromatic-keyboard keyboard-root)
    (kb/create-note-predicate-from-collection (get-in scales [selected-scale (:name keyboard-root)])))))

(re-frame/reg-sub
 ::selected-chord
 (fn [db _]
   (:selected-chord db)))

(re-frame/reg-sub
 ::selected-scale
 (fn [db _]
   (:selected-scale db)))

(re-frame/reg-sub
 ::chords
 (fn [db _]
   (:chords db)))

(re-frame/reg-sub
 ::scales
 (fn [db _]
   (:scales db)))
