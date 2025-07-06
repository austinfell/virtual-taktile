(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [vtakt-client.styles :as general-styles]
   [vtakt-client.midi.events :as midi-events]
   [vtakt-client.midi.subs :as subs]
   [vtakt-client.midi.styles :as styles]))

(defn midi-selector
  [{:keys [outputs selected-output on-output-change]}]
  [:div
   [:div
    {:class (styles/midi-row)}
    [:p {:class (styles/midi-key-name)} "Device:"]
    [re-com/single-dropdown
     :choices (->> outputs
                   (into [])
                   (mapv (fn [[id device]] {:id id :label (:name device)})))
     :width "200px"
     :model selected-output
     :filter-box? true
     :on-change on-output-change]]])

(defn midi-configurator []
  (let [midi-outputs @(re-frame/subscribe [::subs/midi-outputs])
        selected-output @(re-frame/subscribe [::subs/selected-midi-output])]
    [re-com/v-box
     :class (general-styles/configurator-container)
     :gap "15px"
     :children
     [[re-com/title
       :label "MIDI Configuration"
       :level :level2]
      (if (empty? midi-outputs)
        [re-com/alert-box
         :alert-type :danger
         :class (styles/status-notification)
         :style {:margin-bottom 0}
         :body "Host connection is not functioning: there may be an issue with your browser permissions or you may have no MIDI devices available."
         :heading "MIDI Not Connected"])
      (when (seq midi-outputs)
        [:<>
         [midi-selector
          {:outputs midi-outputs
           :selected-output selected-output
           :on-output-change #(re-frame/dispatch [::midi-events/set-selected-midi-output %])}]])]]))
