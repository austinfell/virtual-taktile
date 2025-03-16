(ns vtakt-client.keyboard.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.keyboard.core :as kb]
   [vtakt-client.keyboard.events :as events]
   [vtakt-client.keyboard.subs :as subs]
   [vtakt-client.keyboard.styles :as styles]
   [vtakt-client.styles :as app-styles]
   [vtakt-client.routes :as routes]
   [vtakt-client.utils :as utils]))

;; Keyboard Configuration

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
        inc-disabled? (and (some? max-value) (>= value max-value))]
    [re-com/h-box
     :align :center
     :gap "5px"
     :children
     [[re-com/button
       :label "♭" ; Flat symbol
       :disabled? dec-disabled?
       :style styles/increment-button-style
       :on-click #(when-not dec-disabled?
                    (re-frame/dispatch dec-event))]
      [re-com/box
       :style styles/increment-value-box-style
       :child [:p {:style styles/increment-value-style} value]]

      [re-com/button
       :label "♯" ; Sharp symbol
       :disabled? inc-disabled?
       :style styles/increment-button-style
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
       :style styles/mode-toggle-style
       :children
       [[re-com/box
         :style (styles/mode-option-style chromatic?)
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :chromatic])}
         :child "Chromatic"]
        [re-com/box
         :style (styles/mode-option-style (not chromatic?))
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :folding])}
         :child "Folding"]]]]]))

;; Update white-key to indicate pressed state
(defn- white-key
  "Renders a white key with proper styling.
   - note: The note this key represents
   - pressed?: Whether this note is currently pressed"
  [note idx pressed? chord-mode?]
  [re-com/box
   :style (styles/white-key-style pressed? chord-mode? idx)
   :child [re-com/v-box
           :justify :end
           :align :center
           :style {:height "100%"}
           :children
           [(when (and pressed? (or (not chord-mode?) (not= idx 7)))
              [re-com/box
               :style styles/white-key-indicator-style
               :child ""])
            [re-com/box
             :style (styles/white-key-label-style note)
             :child (if note (kb/format-note (:name note)) "")]]]])

;; Update black-key to indicate pressed state
(defn- black-key
  "Renders a black key with proper styling.
   - note: The note this key represents
   - position: Horizontal offset in pixels
   - pressed?: Whether this note is currently pressed"
  [{:keys [note position pressed?]}]
    [re-com/box
     :style (assoc (styles/black-key-style pressed? note) :left (str position "px"))
     :child [re-com/v-box
             :justify :start
             :align :center
             :style {:height "100%"}
             :children
             [(when pressed?
                [re-com/box
                 :style styles/black-key-indicator-style
                 :child ""])
              [re-com/box
               :style styles/black-key-label-style
               :child (if note (kb/format-note (:name note)) "")]]]])

(defn- is-note-pressed?
  "Determines if a note is currently being pressed."
  [note pressed-notes chord-mode]
  (when (and note pressed-notes)
    (some #(and (= (:name note) (:name %))
                (if (false? chord-mode)
                  (= (:octave note) (:octave %))
                  true))
          pressed-notes)))

(defn- is-note-in-scale?
  "Determines if a note is in the current scale."
  [note scale-notes]
  (when (and note scale-notes)
    (some #(= (:name note) %) scale-notes)))

(defn octave-view
  "Renders a piano-like octave view showing which notes are in the current scale.
   - Selected scale notes will be highlighted"
  []
  (let [keyboard (re-frame/subscribe [::subs/chromatic-keyboard])
        keyboard-root (re-frame/subscribe [::subs/keyboard-root])
        pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        octave-notes (take 12 (filter #(kb/natural-note? %) (iterate #(kb/shift-note % :up) (kb/transpose-note @keyboard-root 1))))

        ;; Extract white and black notes
        white-notes (:bottom (kb/rows @keyboard))
        black-notes (:top (kb/rows @keyboard))

        ;; Create a sequence of [note position] for black keys
        black-key-positions (filter #(contains? #{:d :e :g :a :b} (:name (nth % 2)))
                             (map vector
                              (rest black-notes)
                              [21 51 82 111 141 171 202]
                              octave-notes
                              ))]
    [re-com/box
     :style styles/octave-view-style
     :child
     [re-com/v-box
      :gap "10px"
      :children
      [
       ;; Main keys container with proper positioning
       [re-com/box
        :style styles/keys-container-style
        :child
        [re-com/h-box
         :style {:position "relative"}
         :children
         ;; First place white keys as base layer
         (concat
          (mapv (fn [idx note]
                  [white-key note idx (is-note-pressed? note @pressed-notes (not= @selected-chord :off)) (not= @selected-chord :off)])
                (range)
                white-notes)

          ;; Then place black keys as overlay
          (mapv (fn [[note position]]
                  [black-key {:note note
                              :position position
                              :pressed? (is-note-pressed? note @pressed-notes true)}])
                black-key-positions))]]]]]))

(defn pressed-notes-display []
  (let [pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        ;; Group notes into columns of max 4 items
        grouped-notes (fn [notes]
                        (partition-all 4 notes))]
    [re-com/box
     :style styles/pressed-notes-container-style
     :child
     (if (seq @pressed-notes)
       [re-com/h-box
        :align :center
        :justify :center
        :gap "10px"
        :children
        (doall
          (for [column (grouped-notes @pressed-notes)]
            ^{:key (str "col-" (hash (first column)))}
            [re-com/v-box
             :align :center
             :justify :center
             :gap "5px"
             :children
             (doall
               (for [note column]
                 ^{:key (str (hash note))}
                 [re-com/label
                  :style styles/note-label-style
                  :label (kb/format-root-note note)]))]))]
       [re-com/v-box
        :align :center
        :justify :center
        :style {:height "100%"}
        :children
        [[re-com/label
          :style styles/empty-notes-label-style
          :label "No notes"]]])]))

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
       :style styles/configurator-container-style
       :children
       [[re-com/title
         :label "Keyboard Configuration"
         :level :level2
         :style styles/configurator-title-style]
        [re-com/h-box
         :gap "20px"
         :align :center
         :children
         [
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
             :label "Notes"]
            [pressed-notes-display]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
             :label "Octave Display"]
            [octave-view]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
             :label "Keyboard Mode"]
            [keyboard-mode-selector @keyboard-mode]]]
          ]
         ]
        [re-com/h-box
         :gap "20px"
         :align :center
         :children
         [
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
             :label "Scale"]
            [scale-selector @available-scales @selected-scale]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
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
             :style styles/section-label-style
             :label "Chord"]
            [chord-selector @available-chords @selected-chord]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :style styles/section-label-style
             :label "Transpose"]
            [increment-control
             {:label "Transpose"
              :value @transpose
              :dec-event [::events/dec-keyboard-transpose]
              :inc-event [::events/inc-keyboard-transpose]
              :min-value -36
              :max-value 36
              :is-transpose? true}]]]
         ]]]])))

;; Keyboard

(defn seq-btn [n note chord chords scale scales keyboard-root keyboard-transpose]
  [:div
  [re-com/button
     :attr {
            :on-mouse-down #(let [notes-to-press
                                  (if (not= chord :off)
                                    (if (= scale :chromatic)
                                      (kb/build-chord (get-in chords [chord (:name note)]) (:octave note))
                                      (kb/build-scale-chord (get-in scales [scale (:name (kb/transpose-note keyboard-root keyboard-transpose))]) note))
                                    [note])]
                              (re-frame/dispatch [::events/set-pressed-notes notes-to-press]))
            :on-mouse-up #(re-frame/dispatch [::events/clear-pressed-notes])
            :on-mouse-leave #(re-frame/dispatch [::events/clear-pressed-notes])
            }
     :style (assoc styles/seq-button-style :color (if (nil? note) :black :blue))
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:style styles/seq-number-container-style}
               [:p {:style styles/seq-number-style} (str n)]])]])

(defn keyboard []
  (let [ck (re-frame/subscribe [::subs/keyboard])
        transpose (re-frame/subscribe [::subs/keyboard-transpose])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        available-chords (re-frame/subscribe [::subs/chords])
        selected-scale (re-frame/subscribe [::subs/selected-scale])
        available-scales (re-frame/subscribe [::subs/scales])
        scale-root (re-frame/subscribe [::subs/keyboard-root])
        keyboard-transpose (re-frame/subscribe [::subs/keyboard-transpose])
        ;; Create a local atom to track pressed keys and their associated notes
        pressed-keys (reagent/atom {})

        ;; Define the key mappings
        top-row-keys #{"s" "d" "f" "g" "h" "j" "k" "l"}
        bottom-row-keys #{"z" "x" "c" "v" "b" "n" "m" ","}

        ;; Helper function to get notes from a key
        get-notes-for-key (fn [key]
          (let [top-keys-map {"a" 0, "s" 1, "d" 2, "f" 3, "g" 4, "h" 5, "j" 6, "k" 7}
                bottom-keys-map {"z" 0, "x" 1, "c" 2, "v" 3, "b" 4, "n" 5, "m" 6, "," 7}
                top-row-idx (get top-keys-map key -1)
                bottom-row-idx (get bottom-keys-map key -1)
                note (cond
                       (>= top-row-idx 0) (nth (:top (kb/rows @ck)) top-row-idx)
                       (>= bottom-row-idx 0) (nth (:bottom (kb/rows @ck)) bottom-row-idx)
                       :else nil)]

            (when note
              (if (not= @selected-chord :off)
                (if (= @selected-scale :chromatic)
                  (kb/build-chord (get-in @available-chords [@selected-chord (:name note)]) (:octave note))
                  (kb/build-scale-chord (get-in @available-scales [@selected-scale (:name (kb/transpose-note @scale-root @keyboard-transpose))]) note))
                [note]))))

        ;; Helper function to update pressed notes based on currently pressed keys
        update-pressed-notes (fn []
          (let [all-notes (reduce (fn [notes key-notes]
                                    (concat notes (val key-notes)))
                                  []
                                  @pressed-keys)]
            ;; Only dispatch if there are notes to press
            (when (seq all-notes)
              (re-frame/dispatch [::events/set-pressed-notes all-notes]))))

        ;; Function to handle key press events
        handle-key-press (fn [event]
          (let [key (.toLowerCase (.-key event))]
            ;; Skip handling if the key is already pressed (to avoid retriggering)
            (when (and (or (contains? top-row-keys key)
                           (contains? bottom-row-keys key))
                       (not (contains? @pressed-keys key)))
              (let [notes (get-notes-for-key key)]
                (when (seq notes)
                  ;; Store the notes associated with this key
                  (if (= @selected-chord :off)
                    (swap! pressed-keys assoc key notes)
                    (reset! pressed-keys {key notes}))
                  ;; Update all currently pressed notes
                  (update-pressed-notes))))))

        ;; Function to handle key release events
        handle-key-release (fn [event]
          (let [key (.toLowerCase (.-key event))]
            (when (contains? @pressed-keys key)
              ;; Remove this key from pressed keys
              (swap! pressed-keys dissoc key)
              ;; If no keys are pressed, clear all notes
              (if (empty? @pressed-keys)
                (re-frame/dispatch [::events/clear-pressed-notes])
                ;; Otherwise update with remaining pressed notes
                (update-pressed-notes)))))]

    ;; Add event listeners when component mounts
    (reagent/create-class
     {:component-did-mount
      (fn []
        (.addEventListener js/document "keydown" handle-key-press)
        (.addEventListener js/document "keyup" handle-key-release))

      :component-will-unmount
      (fn []
        (.removeEventListener js/document "keydown" handle-key-press)
        (.removeEventListener js/document "keyup" handle-key-release))

      :reagent-render
      (fn []
        [re-com/v-box
         :justify :center
         :children [[re-com/h-box
                     :children [(map-indexed (fn [idx [n note chord chords scale scales keyboard-root keyboard-transpose]]
                                               (seq-btn n note chord chords scale scales keyboard-root keyboard-transpose))
                                             (map vector
                                                  (range 1 9)
                                                  (:top (kb/rows @ck))
                                                  (repeat @selected-chord)
                                                  (repeat @available-chords)
                                                  (repeat @selected-scale)
                                                  (repeat @available-scales)
                                                  (repeat @scale-root)
                                                  (repeat @keyboard-transpose)))]
                     ]
                    [re-com/h-box
                     :children [(map-indexed (fn [idx [n note chord chords scale scales keyboard-root keyboard-transpose]]
                                               (seq-btn n note chord chords scale scales keyboard-root keyboard-transpose))
                                             (map vector
                                                  (range 9 17)
                                                  (:bottom (kb/rows @ck))
                                                  (repeat @selected-chord)
                                                  (repeat @available-chords)
                                                  (repeat @selected-scale)
                                                  (repeat @available-scales)
                                                  (repeat @scale-root)
                                                  (repeat @keyboard-transpose)))]]]])})))
