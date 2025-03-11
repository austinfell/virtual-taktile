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

(defn increment-control
  "A reusable component for increment/decrement controls using musical flat/sharp symbols.
   - label: Text to display
   - value: Current value to display
   - dec-event: Event to dispatch when decrementing
   - inc-event: Event to dispatch when incrementing
   - min-value: Optional minimum value (default nil)
   - max-value: Optional maximum value (default nil)
   - is-transpose?: Optional flag to use transpose-specific tooltips"
  [{:keys [label value dec-event inc-event min-value max-value is-transpose?]}]
  (let [dec-disabled? (and (some? min-value) (<= value min-value))
        inc-disabled? (and (some? max-value) (>= value max-value))
        dec-tooltip (cond
                      dec-disabled? (str "Minimum value reached: " min-value)
                      is-transpose? "Transpose semitone down"
                      :else (str "Decrease " (clojure.string/lower-case label)))
        inc-tooltip (cond
                      inc-disabled? (str "Maximum value reached: " max-value)
                      is-transpose? "Transpose semitone up"
                      :else (str "Increase " (clojure.string/lower-case label)))]
    [re-com/h-box
     :align :center
     :gap "5px"
     :children
     [[re-com/button
       :label "♭" ; Flat symbol
       :tooltip dec-tooltip
       :tooltip-position :left-center
       :disabled? dec-disabled?
       :class (when-not dec-disabled? "active-button")
       :style {:min-width "40px"
               :font-size "16px"
               :font-weight "bold"
               :background-color "#e2e2e2"
               :border "1px solid #bbbbbb"
               :color "black"}
       :attr {:aria-label dec-tooltip}
       :on-click #(when-not dec-disabled?
                    (re-frame/dispatch dec-event))]
      [re-com/box
       :style {:width "38px"
               :text-align "center"
               :font-weight "bold"
               :display "flex"
               :align-items "center"
               :justify-content "center"
               :color "#333333"}
       :child [:p {:style {:width "100%" :position "relative" :top "5px"}} value]]

      [re-com/button
       :label "♯" ; Sharp symbol
       :tooltip inc-tooltip
       :tooltip-position :right-center
       :disabled? inc-disabled?
       :class (when-not inc-disabled? "active-button")
       :style {:min-width "40px"
               :font-size "16px"
               :font-weight "bold"
               :background-color "#e2e2e2"
               :border "1px solid #bbbbbb"
               :color "black"}
       :attr {:aria-label inc-tooltip}
       :on-click #(when-not inc-disabled?
                    (re-frame/dispatch inc-event))]
     ]]))

(defn make-dropdown-selector
  "Higher-order function that creates dropdown selectors with consistent styling.
   Returns a component function that accepts options and selected value."
  [on-change-event]
  (fn [options selected]
    [re-com/single-dropdown
     :src (at)
     :choices (mapv (fn [v] {:id (first v)}) (into [] options))
     :model selected
     :width "125px"
     :filter-box? true
     :label-fn #(utils/format-keyword (:id %))
     :on-change #(re-frame/dispatch [on-change-event %])]))
(def scale-selector (make-dropdown-selector ::events/set-scale))
(def chord-selector (make-dropdown-selector ::events/set-chord))

(defn keyboard-mode-selector [current-mode]
  (let [chromatic? (= current-mode :chromatic)]
    [re-com/v-box
     :gap "5px"
     :children
     [[re-com/h-box
       :class "mode-toggle"
       :style {:border "1px solid #ccc"
               :border-radius "4px"
               :overflow "hidden"
               :width "200px"
               :height "36px"
               :cursor "pointer"}
       :children
       [[re-com/box
         :class (str "toggle-option" (when chromatic? " active"))
         :style {:flex "1"
                 :text-align "center"
                 :padding "8px"
                 :background-color (if chromatic? "#4a86e8" "#f0f0f0")
                 :color (if chromatic? "white" "black")
                 :transition "all 0.2s ease"}
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :chromatic])}
         :child "Chromatic"]
        [re-com/box
         :class (str "toggle-option" (when-not chromatic? " active"))
         :style {:flex "1"
                 :text-align "center"
                 :padding "8px"
                 :background-color (if chromatic? "#f0f0f0" "#4a86e8")
                 :color (if chromatic? "black" "white")
                 :transition "all 0.2s ease"}
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :folding])}
         :child "Folding"]]]]]))

(defn keyboard-configurator []
  (let [ck (re-frame/subscribe [::subs/keyboard])
        transpose (re-frame/subscribe [::subs/keyboard-transpose])
        keyboard-mode (re-frame/subscribe [::subs/keyboard-mode])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        available-chords (re-frame/subscribe [::subs/chords])
        selected-scale (re-frame/subscribe [::subs/selected-scale])
        available-scales (re-frame/subscribe [::subs/scales])]
    (fn []
      [re-com/v-box
       :gap "15px"
       :children
       [[re-com/title
         :label "Keyboard Configuration"
         :level :level2
         :style {:margin-bottom "5px"}]
        [re-com/h-box
         :gap "20px"
         :align :center
         :style {:background-color "#f5f5f5"
                 :border-radius "8px"
                 :padding "15px"}
         :children
         [[re-com/v-box
           :gap "10px"
           :children
           [[re-com/label 
             :style {:font-weight "bold" :color "black"}
             :label "Scale"]
            [scale-selector @available-scales @selected-scale]]]
          
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label 
             :style {:font-weight "bold" :color "black"}
             :label "Root Note"]
            [increment-control 
             {:label "Root"
              :value (kb/format-root-note (:root-note @ck))
              :dec-event [::events/dec-keyboard-root]
              :inc-event [::events/inc-keyboard-root]}]]]
          
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label 
             :style {:font-weight "bold" :color "black"}
             :label "Chord"]
            [chord-selector @available-chords @selected-chord]]]
          
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label 
             :style {:font-weight "bold" :color "black"}
             :label "Transpose"]
            [increment-control
             {:label "Transpose"
              :value @transpose
              :dec-event [::events/dec-keyboard-transpose]
              :inc-event [::events/inc-keyboard-transpose]
              :min-value -36
              :max-value 36
              :is-transpose? true}]]]
          
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label 
             :style {:font-weight "bold" :color "black"}
             :label "Keyboard Mode"]
            [keyboard-mode-selector @keyboard-mode]]]
         ]]]])))

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
   :children [[keyboard-configurator]
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
   :children [[sequencer]]])

(defmethod routes/panels :sequencer-panel [] [sequencer-panel])
