(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com]
   [re-frame.core :as re-frame]
   [vtakt-client.midi.styles :as styles]
   [vtakt-client.midi.subs :as subs]
   [reagent.core :as r]))

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
  (let [midi-outputs (re-frame.core/subscribe [::subs/midi-outputs])]
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
                  [:p (str (:output (first (vals @midi-outputs))))])]]))

