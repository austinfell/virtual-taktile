(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
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
        selected-midi-output (re-frame.core/subscribe [::subs/selected-midi-output])]
    (println @selected-midi-output)
    [re-com/v-box
     :class (styles/configurator-container)
     :gap "15px"
     :children [[re-com/title
                 :label "MIDI Configuration"
                 :level :level2
                 :class (styles/configurator-title)]
                (if (empty? @midi-outputs)
                  [midi-not-configured-alert]
                    ;; Structure here is:
                    ;; {id {:id id :name ... :manufacturer ... :output MidiOutput}}
                  [:div
                   [:p "Active MIDI Output"]
                   [re-com/single-dropdown
                    :src (re-com/at)
                    :choices (mapv (fn [v] {:id (first v) :label (:name (second v))}) (into [] @midi-outputs))
                    :width "200px"
                    :model @selected-midi-output
                    :filter-box? true
                    :on-change #(re-frame.core/dispatch [::fx/set-selected-midi-output %])]])]]))

