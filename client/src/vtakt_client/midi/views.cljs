(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [vtakt-client.midi.core :as midi]
   [vtakt-client.midi.styles :as styles]))

(defn midi-status-indicator
  "Component showing MIDI connection status."
  []
  (let [selected-output (re-frame/subscribe [::midi/selected-output-id])
        connected? @selected-output]
    (fn []
      [re-com/box
       :class (styles/status-box connected?)
       :child
       [re-com/label
        :label (if connected?
                 "MIDI Connected"
                 "MIDI Not Connected")]])))

(defn midi-output-selector
  "Dropdown for selecting MIDI output device."
  []
  (let [outputs (re-frame/subscribe [::midi/midi-outputs])
        selected-id (re-frame/subscribe [::midi/selected-output-id])]
    (fn []
      (let [choices (mapv (fn [[id info]]
                            {:id id 
                             :label (str (:name info) 
                                        (when (seq (:manufacturer info))
                                          (str " - " (:manufacturer info))))})
                          @outputs)]
        [re-com/v-box
         :gap "5px"
         :children
         [[re-com/single-dropdown
           :src (at)
           :choices choices
           :model @selected-id
           :width "100%"
           :on-change #(re-frame/dispatch [::midi/set-selected-output %])
           :class (styles/dropdown)]]]))))

(defn midi-channel-selector
  "Slider for selecting MIDI channel (1-16)."
  []
  (let [channel (re-frame/subscribe [::midi/midi-channel])]
    (fn []
      [re-com/v-box
       :gap "5px"
       :children
       [[re-com/title
         :level :level3
         :label "MIDI Channel"
         :class (styles/section-title)]
        [re-com/h-box
         :gap "10px"
         :align :center
         :children
         [[re-com/label :label "1"]
          [re-com/slider
           :src (at)
           :model channel
           :min 1
           :max 16
           :width "100%"
           :step 1
           :class (styles/slider)
           :on-change #(re-frame/dispatch [::midi/set-midi-channel %])]
          [re-com/label :label "16"]]]
        [re-com/box
         :align :center
         :child
         [re-com/label
          :class (styles/slider-value)
          :label (str "Channel: " @channel)]]]])))

(defn midi-velocity-control
  "Slider for controlling note velocity."
  []
  (let [velocity (re-frame/subscribe [::midi/velocity])]
    (fn []
      [re-com/v-box
       :gap "5px"
       :children
       [[re-com/title
         :level :level3
         :label "Note Velocity"
         :class (styles/section-title)]
        [re-com/h-box
         :gap "10px"
         :align :center
         :children
         [[re-com/label :label "1"]
          [re-com/slider
           :src (at)
           :model velocity
           :min 1
           :max 127
           :width "100%"
           :step 1
           :class (styles/slider)
           :on-change #(re-frame/dispatch [::midi/set-midi-velocity %])]
          [re-com/label :label "127"]]]
        [re-com/box
         :align :center
         :child
         [re-com/label
          :class (styles/slider-value)
          :label (str "Velocity: " @velocity)]]]])))

(defn midi-panel
  "Main MIDI configuration panel."
  []
  [re-com/v-box
   :gap "15px"
   :class (styles/midi-panel)
   :children
   [[re-com/title
     :label "MIDI Configuration"
     :level :level2
     :class (styles/panel-title)]
    [midi-status-indicator]
    [re-com/v-box
     :gap "15px"
     :class (styles/control-group)
     :children
     [[midi-output-selector]
      [midi-channel-selector]
      [midi-velocity-control]]]
    [re-com/button
     :src (at)
     :label "Refresh MIDI Devices"
     :on-click midi/init-midi!
     :class (styles/refresh-button)]]])

;; Initialize MIDI when panel mounts
(defn midi-panel-container []
  (r/create-class
   {:component-did-mount
    (fn [_]
      (midi/init!))

    :reagent-render
    (fn []
      [midi-panel])}))
