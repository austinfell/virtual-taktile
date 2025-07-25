(ns vtakt-client.step-input.views
  (:require
   [vtakt-client.step-input.styles :as styles]
   [re-com.core :as re-com]
   [reagent.core :as reagent]))

(defn- step-trigger
  [position
   entity
   {:keys [on-step-press-handler on-step-release-handler on-mouse-leave-handler]
    :or {on-step-press-handler #()
         on-step-release-handler #()
         on-mouse-leave-handler #()}}
   step-active-fn]
  (let [is-measure-start? (contains? #{1 5 9 13} position)]
    [:div {:key (str "step-trigger-" position "-" (when entity (str (:name entity) (:octave entity))))}
     [re-com/button
      :attr {:on-mouse-down #(on-step-press-handler entity position)
             :on-mouse-up #(on-step-release-handler entity position)
             :on-mouse-leave #(on-mouse-leave-handler entity position)}
      :class (str (styles/step-trigger-button) " "
                  (case (step-active-fn entity)
                    :red (styles/step-trigger-red)
                    :green (styles/step-trigger-green)
                    :white (styles/step-trigger-white)
                    :off (styles/step-trigger-off)
                    (styles/step-trigger-off)))
      :label (if is-measure-start?
               [:div {:class (styles/step-number-container)}
                [:p {:class (styles/step-number)} (str position)]]
               (str position))]]))

(defn step-input []
  (reagent/create-class
   {:reagent-render
    (fn [handlers step-to-entity-map step-active-fn]  ; <- just re-declare the args
      [re-com/v-box
       :justify :center
       :children [[re-com/h-box
                   :children (mapv
                              (fn [idx]
                                [step-trigger idx (step-to-entity-map idx) handlers step-active-fn])
                              (range 1 9))]
                  [re-com/h-box
                   :children (mapv
                              (fn [idx]
                                [step-trigger idx (step-to-entity-map idx) handlers step-active-fn])
                              (range 9 17))]]])}))

;; TODO - What we need:
;;    - We need a well defined interface for click-down, click-up, mouse exit events. This is for dispatching events lik
;;    note on, note off, pattern selections, banks, etc...
;;    - Click down and click up events should generically be applied to the keyboard
;;    - Color selection interface: We need to ability to dynamically define a underlying step button as
;;    nil, :red, :blue.
;;    - We need to do the above via an easy to use interface... Preferably, we can use the underlying data that
;;    we feed in as a key and a configuration for the underling entity as the value.
