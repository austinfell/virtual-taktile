(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as re-frame]
   [vtakt-client.midi.styles :as styles]
   [reagent.core :as r]))

(defn midi-status-indicator
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
  (fn []
    [re-com/v-box
     :class (styles/configurator-container)
     :gap "15px"
     :children [[re-com/title
                 :label "MIDI Configuration"
                 :level :level2
                 :class (styles/configurator-title)]
                [midi-status-indicator]]]))
