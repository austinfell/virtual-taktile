(ns vtakt-client.step-input.views
  (:require
   [vtakt-client.step-input.styles :as styles]
   [re-com.core :as re-com]
   [reagent.core :as reagent]))


;; Map keyboard keys to positions in the keyboard layout
;; First row: a s d f g h j k (positions 1-8)  <- TOP row
;; Second row: z x c v b n m , (positions 9-16) <- BOTTOM row
;; Keyboard user interface.
(def key-to-position-map
  {"a" 1, "s" 2, "d" 3, "f" 4, "g" 5, "h" 6, "j" 7, "k" 8,
   "z" 9, "x" 10, "c" 11, "v" 12, "b" 13, "n" 14, "m" 15, "," 16})

(defn handle-key-up [event handler]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when (and position (not (.-repeat event)))
      (handler (get key-to-position-map key)))))

(defn handle-key-down [event handler]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when (and position (not (.-repeat event)))
      (handler (get key-to-position-map key)))))

(defonce keyboard-down-handler (atom #()))
(defonce keyboard-up-handler (atom #()))
(.addEventListener js/window "keydown" #(@keyboard-down-handler %))
(.addEventListener js/window "keyup" #(@keyboard-up-handler %))

(defn init-keyboard-listeners [on-key-down on-key-up step-to-entity-map]
  (let [down-lambda #(on-key-down (step-to-entity-map %) %)
        up-lambda #(do
                     (println step-to-entity-map)
                     (on-key-up (step-to-entity-map %) %))
        full-up-lambda #(handle-key-up % up-lambda)
        full-down-lambda #(handle-key-down % down-lambda)]
    (reset! keyboard-up-handler full-up-lambda)
    (reset! keyboard-down-handler full-down-lambda)))

;; Components
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
   {:component-did-mount
    (fn [this]
      (let [{:keys [on-step-press-handler on-step-release-handler]
             :or {on-step-press-handler #()
                  on-step-release-handler #()}} (reagent/props this)
            step-to-entity-map (nth (reagent/argv this) 2)]
        (init-keyboard-listeners on-step-press-handler on-step-release-handler step-to-entity-map)))

    :component-did-update
    (fn [this old-argv]
      (let [old-step-to-entity-map (nth old-argv 2)
            new-step-to-entity-map (nth (reagent/argv this) 2)]
        (when (not= old-step-to-entity-map new-step-to-entity-map)
          (let [{:keys [on-step-press-handler on-step-release-handler]
                 :or {on-step-press-handler #()
                      on-step-release-handler #()}} (reagent/props this)]
            (init-keyboard-listeners on-step-press-handler on-step-release-handler new-step-to-entity-map)))))

    :reagent-render
    (fn [handlers step-to-entity-map step-active-fn]
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

;; TODO - Still need to handle mouse leave.
