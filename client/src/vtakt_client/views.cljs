(ns vtakt-client.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.keyboard.views :as kb]
   [vtakt-client.project.views :as project]
   [vtakt-client.step-input.views :as step-input]
   [vtakt-client.project.pattern.views :as pattern]
   [vtakt-client.project.track.views :as track]
   [vtakt-client.project.pattern.subs :as psubs]
   [vtakt-client.midi.views :as midi]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.subs :as subs]
   [reagent.core :as reagent]))

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
  (let [trigs-mode (reagent/atom :keyboard)
        active-bank (re-frame/subscribe [::psubs/active-bank])]
    (fn []
      [re-com/v-box
       :src      (at)
       :gap      "1em"
       :children [(if (= @trigs-mode :keyboard)
                    [kb/keyboard-configurator])
                  (if (= @trigs-mode :keyboard)
                    [kb/keyboard])
                  (if (= @trigs-mode :pattern-select)
                    [pattern/pattern-select @active-bank #(reset! trigs-mode :keyboard)])
                  (if (= @trigs-mode :bank-select)
                    [pattern/pattern-change-workflow #(reset! trigs-mode :keyboard)])
                  [:div
                   [:button {:on-click #(reset! trigs-mode :keyboard)} "Keyboard"]
                   [:button {:on-click #(reset! trigs-mode :pattern-select)} "Pattern"]
                   [:button {:on-click #(reset! trigs-mode :bank-select)} "Bank"]]
                  [midi/midi-configurator]
                  [project/save-project-as]
                  [project/project-manager]
                  [track/track-select]]])))

(defmethod routes/panels :keyboard-panel []
  [keyboard-panel])

(defn midi-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[midi/midi-configurator]]])

(defmethod routes/panels :midi-panel []
  [midi-panel])
