(ns vtakt-client.project.track.views
  (:require
   [re-com.core :as re-com]
   [vtakt-client.project.track.styles :as styles]
   [vtakt-client.styles :as general-styles]
   [vtakt-client.project.track.subs :as subs]
   [vtakt-client.midi.subs :as midi-subs]
   [vtakt-client.project.track.events :as events]
   [vtakt-client.midi.events :as midi-events]
   [re-frame.core :as re-frame]))

(defn midi-channel-selector []
  (let [selected-channel (re-frame/subscribe [::midi-subs/selected-midi-channel-for-track])]
    (fn []
      [:div
       {:class (styles/channel-row)}
       [:p {:class (styles/channel-key-name)} "Channel:"]
       [re-com/input-text
        :model (str (inc (or @selected-channel 0)))
        :width "60px"
        :attr {:type "number"
               :min 1
               :max 16
               :on-input #(re-frame/dispatch [::midi-events/set-selected-midi-channel-for-track
                                              (dec (js/parseInt (-> % .-target .-value) 10))])}
        :on-change #(re-frame/dispatch [::midi-events/set-selected-midi-channel-for-track (dec (js/parseInt % 10))])]])))

(defn track-select []
  (let [active-track (re-frame/subscribe [::subs/active-track])]
    (fn []
      [re-com/v-box
       :class (general-styles/configurator-container)
       :gap "15px"
       :children
       [[re-com/title
         :label "Track"
         :level :level2]
        [midi-channel-selector]
        [re-com/v-box
         :gap "8px"
         :children
         (mapv (fn [row]
                 [re-com/h-box
                  :gap "8px"
                  :children
                  (mapv (fn [col]
                          (let [track-num (+ (* row 2) col 1)]
                            [:div
                             {:key track-num
                              :on-click #(re-frame/dispatch [::events/set-active-track track-num])
                              :class [(styles/track track-num)
                                      (when (= track-num @active-track)
                                        (styles/track-active track-num))]}
                             (str "T" track-num)]))
                        (range 2))])
               (range 2))]]])))
