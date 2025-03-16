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

;; ------------------------------
;; Control Components
;; ------------------------------

(defn make-increment-control
  "Higher-order function that creates increment/decrement controls with consistent styling.
   Returns a component function that accepts a complex value to display.

   Parameters:
   - label: Text label for the control
   - dec-event: Event vector prefix for decrementing
   - inc-event: Event vector prefix for incrementing
   - render-fn: Function that transforms the value into a displayable string
   - min-value: Optional minimum allowed value
   - max-value: Optional maximum allowed value
   - less-than-fn: Function that compares two values to determine if first is less than second
   - greater-than-fn: Function that compares two values to determine if first is greater than second"
  [& {:keys [label dec-event inc-event render-fn min-value max-value
             less-than-equal-fn]
      :or {render-fn str
           less-than-equal-fn (fn [a b] (< (compare a b) 0))
           min-value nil
           max-value nil}}]
  (fn [value]
    (let [dec-disabled? (and (some? min-value)
                          (less-than-equal-fn value min-value))
          greater-than-equal-fn (fn [l r] (less-than-equal-fn r l))
          inc-disabled? (and (some? max-value)
                          (greater-than-equal-fn value max-value))]
      [re-com/h-box
       :align :center
       :gap "5px"
       :children
       [[re-com/button
         :label "♭"
         :disabled? dec-disabled?
         :class (str (styles/increment-button) " "
                     (when-not dec-disabled? (styles/increment-button-active)))
         :on-click #(when-not dec-disabled?
                      (re-frame/dispatch (conj dec-event value)))]
        [re-com/box
         :class (styles/increment-value-box)
         :child [:p {:class (styles/increment-value)} (render-fn value)]]
        [re-com/button
         :label "♯"
         :disabled? inc-disabled?
         :class (str (styles/increment-button) " "
                     (when-not inc-disabled? (styles/increment-button-active)))
         :on-click #(when-not inc-disabled?
                      (re-frame/dispatch (conj inc-event value)))]
       ]])))
(def root-note-control
  (make-increment-control
   :label "Root Note"
   :dec-event [::events/dec-keyboard-root]
   :inc-event [::events/inc-keyboard-root]
   :render-fn kb/format-root-note
   :min-value kb/c0-note
   :max-value kb/g9-note
   :less-than-equal-fn kb/note-less-than-equal?))
(def transpose-control
  (make-increment-control
   :label "Transpose"
   :dec-event [::events/dec-keyboard-transpose]
   :inc-event [::events/inc-keyboard-transpose]
   :render-fn str
   :min-value -36
   :max-value 36
   :less-than-equal-fn <=))

(defn- make-dropdown-selector
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
       :class (styles/mode-toggle)
       :children
       [[re-com/box
         :class (styles/mode-option chromatic?)
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :chromatic])}
         :child "Chromatic"]
        [re-com/box
         :class (styles/mode-option (not chromatic?))
         :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode :folding])}
         :child "Folding"]]]]]))

;; ------------------------------
;; Piano Key Components
;; ------------------------------

(defn- white-key-component
  "Renders a white key with proper styling.
   - note: The note this key represents
   - pressed?: Whether this note is currently pressed"
  [note idx pressed? chord-mode?]
  [re-com/box
   :class (styles/white-key pressed? chord-mode? idx)
   :child [re-com/v-box
           :justify :end
           :align :center
           :style {:height "100%"}
           :children
           [(when (and pressed? (or (not chord-mode?) (not= idx 7)))
              [re-com/box
               :class (styles/white-key-indicator)
               :child ""])
            [re-com/box
             :class (styles/white-key-label note)
             :child (if note (kb/format-note (:name note)) "")]]]])

(defn- black-key-component
  "Renders a black key with proper styling.
   - note: The note this key represents
   - position: Horizontal offset in pixels
   - pressed?: Whether this note is currently pressed"
  [{:keys [note position pressed?]}]
    [re-com/box
     :class (styles/black-key pressed? note)
     :style {:left (str position "px")}
     :child [re-com/v-box
             :justify :start
             :align :center
             :style {:height "100%"}
             :children
             [(when pressed?
                [re-com/box
                 :class (styles/black-key-indicator)
                 :child ""])
              [re-com/box
               :class (styles/black-key-label)
               :child (if note (kb/format-note (:name note)) "")]]]])

;; ------------------------------
;; Keyboard UI Components
;; ------------------------------

(defn seq-btn
  "Button component for sequencer keys"
  [n note chord chords scale scales keyboard-root keyboard-transpose]
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
     :class (styles/seq-button)
     :style {:color (if (nil? note) "black" "blue")}
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:class (styles/seq-number-container)}
               [:p {:class (styles/seq-number)} (str n)]])]])

(defn octave-view
  "Renders a piano-like octave view showing which notes are in the current scale."
  []
  (let [keyboard (re-frame/subscribe [::subs/chromatic-keyboard])
        keyboard-root (re-frame/subscribe [::subs/keyboard-root])
        pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        octave-notes (take 12 (filter #(kb/natural-note? %)
                              (iterate #(kb/shift-note % :up)
                                       (kb/transpose-note @keyboard-root 1))))
        chord-mode? (not= @selected-chord :off)

        ;; Extract white and black notes
        white-notes (:bottom (kb/rows @keyboard))
        black-notes (:top (kb/rows @keyboard))

        ;; Create a sequence of [note position] for black keys
        black-key-positions (filter #(contains? #{:d :e :g :a :b} (:name (nth % 2)))
                            (map vector
                                (rest black-notes)
                                [21 51 82 111 141 171 202]
                                octave-notes))]
    [re-com/box
     :class (styles/octave-view)
     :child
     [re-com/v-box
      :gap "10px"
      :children
      [;; Main keys container with proper positioning
       [re-com/box
        :class (styles/keys-container)
        :child
        [re-com/h-box
         :class (styles/keys-relative-container)
         :children
         ;; First place white keys as base layer
         (concat
          (mapv (fn [idx note]
                  (let [pressed? (when (and note @pressed-notes)
                                   (if chord-mode?
                                     ;; In chord mode, only check note name
                                     (some #(= (:name note) (:name %)) @pressed-notes)
                                     ;; In normal mode, check exact note (name and octave)
                                     (some #(and (= (:name note) (:name %))
                                                (= (:octave note) (:octave %)))
                                           @pressed-notes)))]
                    [white-key-component note idx pressed? chord-mode?]))
                (range)
                white-notes)

          ;; Then place black keys as overlay
          (mapv (fn [[note position]]
                  (let [pressed? (when (and note @pressed-notes)
                                   ;; For black keys, always just check the note name
                                   (some #(= (:name note) (:name %)) @pressed-notes))]
                    [black-key-component {:note note
                                          :position position
                                          :pressed? pressed?}]))
                black-key-positions))]]]]]))

(defn pressed-notes-display []
  (let [pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        ;; Group notes into columns of max 4 items
        grouped-notes (fn [notes]
                        (partition-all 4 notes))]
    [re-com/box
     :class (styles/pressed-notes-container)
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
                  :class (styles/note-label)
                  :label (kb/format-root-note note)]))]))]
       [re-com/v-box
        :align :center
        :justify :center
        :style {:height "100%"}
        :children
        [[re-com/label
          :class (styles/empty-notes-label)
          :label "No notes"]]])]))

;; ------------------------------
;; Main Components
;; ------------------------------

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
       :class (styles/configurator-container)
       :children
       [[re-com/title
         :label "Keyboard Configuration"
         :level :level2
         :class (styles/configurator-title)]
        [re-com/h-box
         :gap "20px"
         :align :center
         :children
         [
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
             :label "Notes"]
            [pressed-notes-display]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
             :label "Octave Display"]
            [octave-view]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
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
             :class (styles/section-label)
             :label "Scale"]
            [scale-selector @available-scales @selected-scale]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
             :label "Root Note"]
            [root-note-control (:root-note @ck)]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
             :label "Chord"]
            [chord-selector @available-chords @selected-chord]]]
          [re-com/v-box
           :gap "10px"
           :children
           [[re-com/label
             :class (styles/section-label)
             :label "Transpose"]
            [transpose-control @transpose]]]
         ]]]])))

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

        ;; Event handler functions
        update-pressed-notes (fn []
          (let [all-notes (reduce (fn [notes key-notes]
                                    (concat notes (val key-notes)))
                                  []
                                  @pressed-keys)]
            ;; Only dispatch if there are notes to press
            (when (seq all-notes)
              (re-frame/dispatch [::events/set-pressed-notes all-notes]))))

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

    ;; Component lifecycle with event handlers
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
                     :children [(map-indexed
                                 (fn [idx [n note chord chords scale scales keyboard-root keyboard-transpose]]
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
                     :children [(map-indexed
                                 (fn [idx [n note chord chords scale scales keyboard-root keyboard-transpose]]
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
