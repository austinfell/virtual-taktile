(ns vtakt-client.step-input.views
  (:require
   [vtakt-client.step-input.styles :as styles]
   [re-com.core :as re-com]
   [reagent.core :as reagent]))

(defn- step-trigger
  [position entity {:keys [on-step-press-handler on-step-release-handler on-mouse-leave-handler]
                    :or {on-step-press-handler #()
                         on-step-release-handler #()
                         on-mouse-leave-handler #()}}]
  (let [is-measure-start? (contains? #{1 5 9 13} position)]
    [:div {:key (str "step-trigger-" position "-" (when entity (str (:name entity) (:octave entity))))}
     [re-com/button
      :attr {:on-mouse-down #(on-step-press-handler entity position)
             :on-mouse-up #(on-step-release-handler entity position)
             :on-mouse-leave #(on-mouse-leave-handler entity position)}
      :class (str (styles/note-trigger-button) " "
                  (if active?
                    (styles/note-trigger-active)
                    (styles/note-trigger-inactive)))
      :label (if is-measure-start?
               [:div {:class (styles/seq-number-container)}
                [:p {:class (styles/seq-number)} (str position)]]
               (str position))]]))

(defn step-input
  [handlers
   step-to-entity-map]
  (reagent/create-class
   {:component-did-mount
    (fn [_] nil)

    :component-will-unmount
    (fn [_] nil)

    :reagent-render
    (fn []
      [re-com/v-box
       :justify :center
       :children [[re-com/h-box
                   :children (mapv
                              (fn [idx]
                                [step-trigger idx (step-to-entity-map idx) handlers])
                              (range 1 9))]

                  [re-com/h-box
                   :children (mapv
                              (fn [idx]
                                [step-trigger idx (step-to-entity-map idx) handlers])
                              (range 9 17))]]])}))

;; TODO - What we need:
;;    - We need a well defined interface for click-down, click-up, mouse exit events. This is for dispatching events lik
;;    note on, note off, pattern selections, banks, etc...
;;    - Click down and click up events should generically be applied to the keyboard
;;    - Color selection interface: We need to ability to dynamically define a underlying step button as
;;    nil, :red, :blue.
;;    - We need to do the above via an easy to use interface... Preferably, we can use the underlying data that
;;    we feed in as a key and a configuration for the underling entity as the value.
