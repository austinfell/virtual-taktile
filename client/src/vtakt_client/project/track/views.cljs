(ns vtakt-client.project.track.views
  (:require
   [vtakt-client.styles :as general-styles]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [vtakt-client.project.track.events :as events]
   [vtakt-client.project.track.subs :as subs]))

(defn track-selector []
  (let [selected-track (re-frame/subscribe [::subs/selected-track])
        available-tracks (re-frame/subscribe [::subs/available-tracks])]
    [re-com/v-box
     :class (general-styles/configurator-container)
     :gap "15px"
     :children
     [[re-com/horizontal-tabs
      :model @selected-track
      :tabs (mapv (fn [track] {:id track :label (str track)}) @available-tracks)
      :on-change #(re-frame/dispatch [::events/set-selected-track %])]]]))

