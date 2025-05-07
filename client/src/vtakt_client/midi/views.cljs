(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [vtakt-client.events :as events]
   [vtakt-client.midi.midi-fx :as fx]
   [vtakt-client.midi.styles :as styles]
   [vtakt-client.midi.subs :as subs]))

(defn midi-not-configured-alert
  "Component showing MIDI connection status."
  []
  (fn []
    [re-com/box
     :child
     [re-com/alert-box
      :alert-type :danger
      :body "Host connection is not functioning: there may be an issue with your browser permissions."
      :heading "MIDI Not Connected"]]))

(defn midi-configurator
  "Root level component that allows configuration of midi"
  []
  (let [midi-outputs (re-frame.core/subscribe [::subs/midi-outputs])
        selected-midi-output (re-frame.core/subscribe [::subs/selected-midi-output])
        selected-midi-channel (re-frame.core/subscribe [::subs/selected-midi-channel])]
    [re-com/v-box
     :class (styles/configurator-container)
     :gap "15px"
     :children [[re-com/title
                 :label "MIDI Configuration"
                 :level :level2
                 :class (styles/configurator-title)]
                (if (empty? @midi-outputs)
                  [midi-not-configured-alert]
                  [:div
                   [:p "Active MIDI Output"]
                   ;; Trying to get this to work with a single dropdown is a little funky...
                   ;; Re-com has a few nice components for handling multiple entries which can be useful
                   ;; for layering in a midi context... Probably my favorite is multi-select list... But
                   ;; I want to make sure it is accessible for keyboard users.
                   [re-com/single-dropdown
                    :src (re-com/at)
                    :choices (conj (mapv (fn [v] {:id (first v) :label (:name (second v))}) (into [] @midi-outputs)) {:id nil :label "Disable"})
                    :width "200px"
                    :model @selected-midi-output
                    :filter-box? true
                    :on-change #(re-frame.core/dispatch [::fx/set-selected-midi-output %])]
                   [re-com/single-dropdown
                    :src (re-com/at)
                    :choices (mapv (fn [v] {:id v :label (inc v)}) [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15])
                    :width "200px"
                    :model @selected-midi-channel
                    :filter-box? true
                    :on-change #(re-frame.core/dispatch [::fx/set-selected-midi-channel %])]
                   [:div
                    [re-com/hyperlink
                     :src      (re-com/at)
                     :label    "go to Keyboard"
                     :on-click #(re-frame/dispatch [::events/navigate :keyboard])]]])]]))

