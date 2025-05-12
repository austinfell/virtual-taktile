(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [vtakt-client.events :as events]
   [vtakt-client.styles :as styles]
   [vtakt-client.midi.events :as midi-events]
   [vtakt-client.midi.subs :as subs]))

;; TODO Still want to work with a more cohesive MIDI tuple data structure here.
(defn midi-status
  "Show the current status of MIDI based on if the browser has access to any devices."
  [outputs]
  (if (empty? outputs)
    [re-com/alert-box
     :alert-type :danger
     :style {:margin-bottom 0}
     :body "Host connection is not functioning: there may be an issue with your browser permissions or you may have no MIDI devices available."
     :heading "MIDI Not Connected"]
    [re-com/alert-box
     :alert-type :info
     :style {:margin-bottom 0}
     :body "Able to transmit MIDI data to host operating system!"
     :heading "MIDI Connected"]))

(defn midi-selector
  [{:keys [outputs selected-output selected-channel on-output-change on-channel-change]}]
  [:div
   [:div
    {:style {:display "flex" :align-items "center"}}
    [:p {:style {:color "black" :margin 0  :margin-right "5px"}} "Device:"]
    [re-com/single-dropdown
     :choices (->> outputs
                   (into [])
                   (mapv (fn [[id device]] {:id id :label (:name device)})))
     :width "200px"
     :model selected-output
     :filter-box? true
     :on-change on-output-change]]
   [:div
    {:style {:margin-top "10px"}}
    [:div
     {:style {:display "flex" :align-items "center"}}
     [:p {:style {:color "black" :margin 0 :margin-right "5px"}} "Channel:"]
     [re-com/single-dropdown
      :choices (->> (range 16)
                    (mapv (fn [v] {:id v :label (inc v)})))
      :width "60px"
      :model selected-channel
      :filter-box? true
      :on-change on-channel-change]]]])

(defn midi-configurator []
  (let [midi-outputs @(re-frame/subscribe [::subs/midi-outputs])
        selected-output @(re-frame/subscribe [::subs/selected-midi-output])
        selected-channel @(re-frame/subscribe [::subs/selected-midi-channel])]
    [re-com/v-box
     :class (styles/configurator-container)
     :gap "15px"
     :children
     [[re-com/title
       :label "MIDI Configuration"
       :level :level2]
      [midi-status midi-outputs]
      (when (seq midi-outputs)
        [:<>
         [midi-selector
          {:outputs midi-outputs
           :selected-output selected-output
           :selected-channel selected-channel
           :on-output-change #(re-frame/dispatch [::midi-events/set-selected-midi-output %])
           :on-channel-change #(re-frame/dispatch [::midi-events/set-selected-midi-channel %])}]
         [re-com/hyperlink
          :label "go to Keyboard"
          :style {:margin-top "10px"}
          :on-click #(re-frame/dispatch [::events/navigate :keyboard])]])]]))
