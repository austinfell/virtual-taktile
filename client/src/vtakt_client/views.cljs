(ns vtakt-client.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.keyboard.views :as kb]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.midi.views :as mv]
   [vtakt-client.subs :as subs]))

;; home
(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Hello from " @name ". This is the Home Page.")
     :level :level1]))

(defn link-to-about-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to About Page"
   :on-click #(re-frame/dispatch [::events/navigate :about])])

(defn home-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[home-title]
              [link-to-about-page]]])

(defn link-to-keyboard-page []
  [re-com/hyperlink
   :src      (at)
   :label    "Back to Keyboard"
   :on-click #(re-frame/dispatch [::events/navigate :keyboard])])

(defn midi-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[mv/midi-panel-container]
              [link-to-keyboard-page]]])

(defmethod routes/panels :midi-panel [] [midi-panel])

(defn link-to-midi-page []
  [re-com/hyperlink
   :src      (at)
   :label    "MIDI Configuration"
   :on-click #(re-frame/dispatch [::events/navigate :midi])])

(defmethod routes/panels :home-panel [] [home-panel])

;; about

(defn about-title []
  [re-com/title
   :src   (at)
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to Home Page"
   :on-click #(re-frame/dispatch [::events/navigate :home])])

(defn about-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[about-title]
              [link-to-home-page]]])

(defmethod routes/panels :about-panel [] [about-panel])

;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(routes/panels @active-panel)]]))

(defn keyboard-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[kb/keyboard-configurator]
              [kb/keyboard]
              [link-to-midi-page]]])

(defmethod routes/panels :keyboard-panel []
  [keyboard-panel])
