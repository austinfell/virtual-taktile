(ns vtakt-client.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.components.keyboard :as kb]
   [vtakt-client.styles :as styles]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.utils :as utils]
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

(defn keyboard-mode-selector []
  (let [current-mode (re-frame/subscribe [::subs/keyboard-mode])]
    [re-com/selection-list
     {:choices [{:id :chromatic :label "Chromatic"}
                {:id :folding :label "Folding"}]
      :model #{@current-mode}
      :style {:color "black"}
      :multi-select? false
      :on-change #(re-frame/dispatch [::events/set-keyboard-mode (first %)])}]))

(defn seq-btn [n note chord chords scale scales keyboard-root keyboard-transpose]
  [:div
  [re-com/button
     :attr {
            :on-mouse-down #(println
                             (str "on " (if (not= chord :off)
                                          (if (= scale :chromatic)
                                            (str (kb/build-chord (get-in chords [chord (:name note)]) (:octave note)))
                                            (str (kb/build-scale-chord (get-in scales [scale (:name (kb/transpose-note keyboard-root keyboard-transpose))]) note)))
                                          (str [note]))))
            :on-mouse-up #(println
                           (str "off " (if (not= chord :off)
                                        (if (= scale :chromatic)
                                          (str (kb/build-chord (get-in chords [chord (:name note)]) (:octave note)))
                                          (str (kb/build-scale-chord (get-in scales [scale (:name (kb/transpose-note keyboard-root keyboard-transpose))]) note)))
                                        (str [note]))))
            :on-mouse-leave #(println
                              (str "off " (if (not= chord :off)
                                            (if (= scale :chromatic)
                                              (str (kb/build-chord (get-in chords [chord (:name note)]) (:octave note)))
                                              (str (kb/build-scale-chord (get-in scales [scale (:name (kb/transpose-note keyboard-root keyboard-transpose))]) note)))
                                            (str [note]))))
            }
     :style {:width "30px"
             :display "flex"
             :align-items "center"
             :padding 0
             :justify-content "center"
             :color (if (nil? note) :black :blue)
             :text-decoration "underline solid black 1px"
             :height "40px"}
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:style {:display "flex" :height "90%" :border-radius "3px" :justify-content "center" :align-items "center" :width "20px" :border "1px solid black"}} [:p {:style {:margin-bottom "0px"}} (str n)]])]])

(defn scale-selector []
  (let [options (re-frame/subscribe [::subs/scales])
        selected (re-frame/subscribe [::subs/selected-scale])]
    (fn []
      [re-com/single-dropdown
       :src (at)
       :choices (mapv (fn [v] {:id (first v)}) (into [] @options))
       :model @selected
       :width "125px"
       :label-fn #(utils/format-keyword (:id %))
       :on-change #(re-frame/dispatch [::events/set-scale %])
                     ])))

(defn chord-selector []
  (let [options (re-frame/subscribe [::subs/chords])
        selected (re-frame/subscribe [::subs/selected-chord])]
    (fn []
      [re-com/single-dropdown
       :src (at)
       :choices (mapv (fn [v] {:id (first v)}) (into [] @options))
       :model @selected
       :width "125px"
       :label-fn #(utils/format-keyword (:id %))
       :on-change #(re-frame/dispatch [::events/set-chord %])
       ])))


(defn sequencer []
  (let [ck (re-frame/subscribe [::subs/keyboard])
        transpose (re-frame/subscribe [::subs/keyboard-transpose])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        available-chords (re-frame/subscribe [::subs/chords])
        selected-scale (re-frame/subscribe [::subs/selected-scale])
        available-scales (re-frame/subscribe [::subs/scales])
        scale-root (re-frame/subscribe [::subs/keyboard-root])
        keyboard-transpose (re-frame/subscribe [::subs/keyboard-transpose])]
  [re-com/v-box
   :justify :center
   :children [[re-com/h-box
               :children
               [
                [scale-selector]
                [chord-selector]
                [re-com/button :label "<-" :on-click #(re-frame/dispatch [::events/dec-keyboard-root])]
                [re-com/button :label "->" :on-click #(re-frame/dispatch [::events/inc-keyboard-root])]
                [re-com/label
                 :style {:color :black :margin-top "5px" :margin-left "5px"}
                 :label
                 (str
                  "Root: "
                  (utils/format-keyword (get-in @ck [:root-note :name]))
                  (get-in @ck [:root-note :octave])
                  )]
                [keyboard-mode-selector]
                [re-com/button :label "<-" :on-click #(when (> @transpose -36)
                                                        (re-frame/dispatch [::events/dec-keyboard-transpose]))]
                [re-com/button :label "->" :on-click #(when (< @transpose 36)
                                                        (re-frame/dispatch [::events/inc-keyboard-transpose]))]
                [re-com/label
                 :style {:color :black :margin-top "5px" :margin-left "5px"}
                 :label
                 (str "Transpose: " @transpose)]
               ]
               ]
              [re-com/h-box
               :children [(map seq-btn
                               (range 1 9)
                               (:top (kb/rows @ck))
                               (repeat @selected-chord)
                               (repeat @available-chords)
                               (repeat @selected-scale)
                               (repeat @available-scales)
                               (repeat @scale-root)
                               (repeat @keyboard-transpose))]
               ]
              [re-com/h-box
               :children [(map seq-btn
                               (range 9 17)
                               (:bottom (kb/rows @ck))
                               (repeat @selected-chord)
                               (repeat @available-chords)
                               (repeat @selected-scale)
                               (repeat @available-scales)
                               (repeat @scale-root)
                               (repeat @keyboard-transpose))]]
              ]]))

(defn sequencer-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[seq-title]
              [sequencer]]])

(defmethod routes/panels :sequencer-panel [] [sequencer-panel])
