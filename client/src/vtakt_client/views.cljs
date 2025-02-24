(ns vtakt-client.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.components.keyboard :as kb]
   [vtakt-client.styles :as styles]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.subs :as subs]))

;; home
(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Hello from " @name ". This is the Home Page." )
     :level :level1
     :class (styles/level1)]))

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

;; sequencer
;; TODO - Extract to separate file.
(defn seq-title []
  [re-com/title
   :src   (at)
   :label "VTakt Sequencer"
   :level :level1])

(defn seq-btn [n note]
  [re-com/button
     :style {:width "30px"
             :display "flex"
             :align-items "center"
             :padding 0
             :justify-content "center"
             :color (if (or (nil? note) (= n 1)) :black :blue)
             :text-decoration "underline solid black 1px"
             :height "40px"}
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:style {:display "flex" :height "90%" :border-radius "3px" :justify-content "center" :align-items "center" :width "20px" :border "1px solid black"}} [:p {:style {:margin-bottom "0px"}} (str n)]])])


(defn sequencer []
  (let [ck (kb/chromatic-keyboard 25 (kb/chromatic-scales :c))]
  [re-com/v-box
   :children [
              ;; TODO - We should really just use CSS to do the wrapping of 8 8 instead of defining it structurally.
              [re-com/h-box
               :children [(map seq-btn (range 1 9) (first ck))]
               ]
              [re-com/h-box
               :children [(map seq-btn (range 9 17) (second ck))]
               ]
              ]]))

(defn sequencer-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[seq-title]
              [sequencer]]])

(defmethod routes/panels :sequencer-panel [] [sequencer-panel])
