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
   [vtakt-client.utils.core :as uc]
   [vtakt-client.utils.specs :as us]
   [clojure.spec.alpha :as s]))

;; ------------------------------
;; Control Components
;; ------------------------------

(defn- make-increment-control
  "Higher-order function that creates increment/decrement controls with consistent styling.
   Returns a memoized component function that accepts a complex value to display.
   Parameters:
   - label: Text label for the control
   - dec-event: Event vector prefix for decrementing
   - inc-event: Event vector prefix for incrementing
   - render-fn: Function that transforms the value into a displayable string
   - min-value: Optional minimum allowed value (inclusive boundary)
   - max-value: Optional maximum allowed value (inclusive boundary)
   - at-or-below-fn: Function that compares if first value is at or below second value
                     (returns true when first <= second in conventional terms)
                     This determines when decrement button should be disabled
   Returns:
   A memoized function component that takes a value and renders an increment control

   Sufficiently generic that I do not think this is worth spec-ing out."
  [& {:keys [label dec-event inc-event render-fn min-value max-value
             at-or-below-fn]
      :or {render-fn str
           at-or-below-fn (fn [a b] (< (compare a b) 0))
           min-value nil
           max-value nil}}]

  (memoize
    (fn [value]
      (let [dec-disabled? (and (some? min-value)
                            (at-or-below-fn value min-value))
            at-or-above-fn (fn [l r] (at-or-below-fn r l))
            inc-disabled? (and (some? max-value)
                            (at-or-above-fn value max-value))]
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
         ]]))))

;; Root note control
(s/def ::root-note-in-range
  (s/and ::kb/note
         (fn [note]
           (and (kb/note-at-or-below? kb/c0-note note)
                (kb/note-at-or-below? note kb/g9-note)))))
(s/fdef root-note-control
  :args (s/cat :value ::root-note-in-range)
  :ret ::us/reagent-component)
(def root-note-control
  (make-increment-control
   :label "Root"
   :dec-event [::events/dec-keyboard-root]
   :inc-event [::events/inc-keyboard-root]
   :render-fn kb/format-root-note
   :min-value kb/c0-note
   :max-value kb/g9-note
   :at-or-below-fn kb/note-at-or-below?))

;; Transpose control
(s/def ::transpose-value (s/int-in -36 36))
(s/fdef transpose-control
  :args (s/cat :value ::transpose-value)
  :ret ::us/reagent-component)
(def transpose-control
  (make-increment-control
   :label "Transpose"
   :dec-event [::events/dec-keyboard-transpose]
   :inc-event [::events/inc-keyboard-transpose]
   :render-fn str
   :min-value -36
   :max-value 36
   :at-or-below-fn <=))

(defn- make-dropdown-selector
  "Higher-order function that creates dropdown selectors with consistent styling.
   Returns a memoized component function that accepts options and selected value.

   Parameters:
   - on-change-event: Event to dispatch when selection changes
   - width: Width of the dropdown (default: '125px')
   - filter-box?: Whether to show a filter box (default: true)
   - pinned-items: Set of item keys that should appear at the top of the list
   - sort-fn: Function to sort the non-pinned options (default: alphabetical by name)"
  [on-change-event & {:keys [width filter-box? pinned-items sort-fn]
                       :or {width "125px"
                            filter-box? true
                            pinned-items #{}
                            sort-fn (fn [options]
                                     (sort-by (comp name first) options))}}]
  (memoize
    (fn [options selected]
      (let [;; Split options into pinned and regular items
            pinned (filter #(contains? pinned-items (first %)) options)
            regular (filter #(not (contains? pinned-items (first %))) options)
            ;; Sort the regular items
            sorted-regular (sort-fn regular)
            ;; Combine pinned items (in original order) with sorted regular items
            combined-options (concat pinned sorted-regular)]
        [re-com/single-dropdown
         :src (at)
         :class (styles/dropdown)
         :choices (mapv (fn [v] {:id (first v)}) (into [] combined-options))
         :model selected
         :width width
         :filter-box? filter-box?
         :label-fn #(uc/format-keyword (:id %))
         :on-change #(re-frame/dispatch [on-change-event %])]))))

;; Scale selector
(s/def ::scale-options (s/map-of keyword? (s/coll-of ::kb/chromatic-note :kind sequential?)))
(s/def ::selected-scale keyword?)
(s/fdef scale-selector
  :args (s/cat :options ::scale-options :selected ::selected-scale)
  :ret ::us/reagent-component)
(def scale-selector
  (make-dropdown-selector
   ::events/set-scale
   :width "190px"
   :pinned-items #{:chromatic}))

;; Chord selector
(s/def ::chord-options (s/map-of keyword? (s/map-of ::kb/chromatic-note (s/coll-of ::kb/chromatic-note :kind sequential?))))
(s/def ::selected-chord keyword?)
(s/fdef chord-selector
  :args (s/cat :options ::chord-options :selected ::selected-chord)
  :ret ::us/reagent-component)
(def chord-selector
  (make-dropdown-selector
   ::events/set-chord
   :pinned-items #{:off}))

;; Keyboard mode selector.
(s/def ::keyboard-mode #{:chromatic :folding})
(s/fdef keyboard-mode-selector
  :args (s/cat :current-mode ::keyboard-mode)
  :ret ::us/reagent-component)
(defn keyboard-mode-selector
  "A toggle component for switching between chromatic and folding keyboard modes."
  [current-mode]
  (let [chromatic? (= current-mode :chromatic)
        ;; TODO - we should really have a single source of truth for available keyboard modes...
        ;; who knows, maybe one day we will have a 31TET keyboard or something...
        mode-options [{:id :chromatic :label "Chromatic"}
                      {:id :folding :label "Folding"}]]
    [re-com/v-box
     :gap "5px"
     :children
     [[re-com/h-box
       :class (styles/mode-toggle)
       :children
       (for [{:keys [id label]} mode-options]
         [re-com/box
          :class (styles/mode-option (= id current-mode))
          :attr {:on-click #(re-frame/dispatch [::events/set-keyboard-mode id])
                 :key (name id)}
          :child label])]]]))

;; ------------------------------
;; Piano Key Components
;; ------------------------------

(s/def ::note-or-nil (s/nilable ::kb/note))
(s/def ::pressed? boolean?)
(s/def ::key-type #{:white :black})
(s/fdef piano-key
  :args (s/cat :note ::note-or-nil
               :pressed? ::pressed?
               :key-type ::key-type)
  :ret ::us/reagent-component)
(def piano-key
  (memoize
    (fn [note pressed? key-type]
      (let [is-white? (= key-type :white)]
        [re-com/box
         :class (if is-white?
                  (styles/white-key pressed?)
                  (styles/black-key pressed?))
         :child [re-com/v-box
                 :justify (if is-white? :end :start)
                 :align :center
                 :style {:height "100%"}
                 :children
                 [(when pressed?
                    [re-com/box
                     :class (if is-white?
                              (styles/white-key-indicator)
                              (styles/black-key-indicator))
                     :child ""])
                  [re-com/box
                   :class (if is-white?
                            (styles/white-key-label note)
                            (styles/black-key-label))
                   :child (if note (kb/format-note (:name note)) "")]]]]))))

;; ------------------------------
;; Keyboard UI Components
;; ------------------------------

(defn- note-trigger-impl
  [position note]
  (let [is-measure-start? (contains? #{1 5 9 13} position)
        has-note? (some? note)]
    [:div {:key (str "note-trigger-" position "-" (when note (str (:name note) (:octave note))))}
     [re-com/button
      :attr {:on-mouse-down #(when note (re-frame/dispatch [::events/trigger-note note]))
             :on-mouse-up #(re-frame/dispatch [::events/clear-pressed-notes])
             :on-mouse-leave #(re-frame/dispatch [::events/clear-pressed-notes])}
      :class (str (styles/note-trigger-button) " "
                  (if has-note?
                    (styles/note-trigger-active)
                    (styles/note-trigger-inactive)))
      :label (if is-measure-start?
               [:div {:class (styles/seq-number-container)}
                [:p {:class (styles/seq-number)} (str position)]]
               (str position))]]))

(s/def ::keyboard-position (s/int-in 1 17))
(s/def ::note-or-nil (s/nilable ::kb/note))
(s/fdef note-trigger
  :args (s/cat :position ::keyboard-position
               :note ::note-or-nil)
  :ret ::us/reagent-component)
(def note-trigger
  "A button component that triggers a note or chord when pressed.
   Parameters:
   - position: The position number in the keyboard layout (1-16)
   - note: The note data to be played or nil if no note at this position

   This component is memoized for performance when re-rendering with the same props."
  (memoize note-trigger-impl))

(defn octave-view
  "Renders a piano-like octave view showing which notes are in the current scale."
  []
  (let [keyboard (re-frame/subscribe [::subs/chromatic-keyboard])
        keyboard-root (re-frame/subscribe [::subs/keyboard-root])
        pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        chord-mode? (not= @selected-chord :off)

        ;; Extract white and black notes
        white-notes (:bottom (kb/rows @keyboard))
        black-notes (:top (kb/rows @keyboard))

        octave-notes (take 12 (filter #(kb/natural-note? %)
                                      (iterate #(kb/shift-note % :up)
                                               (kb/transpose-note @keyboard-root 1))))

        ;; Determine if a note is pressed, with special handling for idx 7 in chord mode
        is-note-pressed? (fn [note idx]
                           (when (and note @pressed-notes)
                             (if chord-mode?
                               ;; In chord mode, check note name and handle special case for idx 7
                               (and (some #(= (:name note) (:name %)) @pressed-notes)
                                    (or (not= idx 7) (not chord-mode?)))
                               ;; In normal mode, check exact note (name and octave)
                               (some #(and (= (:name note) (:name %))
                                          (= (:octave note) (:octave %)))
                                     @pressed-notes))))

        ;; Create a sequence of [note position-index] for black keys
        black-key-positions (filter #(contains? #{:d :e :g :a :b} (:name (nth % 2)))
                              (map vector
                                (rest black-notes)
                                [1 2 3 4 5 6 7]
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
                  [piano-key note (is-note-pressed? note idx) :white])
                (range)
                white-notes)

          ;; Then place black keys as overlay with class-based positioning
          (mapv (fn [[note position-index]]
                  (let [pressed? (when (and note @pressed-notes)
                                   ;; For black keys, always just check the note name
                                   (some #(= (:name note) (:name %)) @pressed-notes))]
                    [re-com/box
                     :class (styles/black-key-position position-index)
                     :child [piano-key note pressed? :black]]))
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
             :label "Root"]
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
  (let [ck (re-frame/subscribe [::subs/keyboard])]
    (fn []
      (let [keyboard-rows (kb/rows @ck)
            top-row (:top keyboard-rows)
            bottom-row (:bottom keyboard-rows)]
        [re-com/v-box
         :justify :center
         :children [[re-com/h-box
                     :children (map-indexed
                                (fn [idx note]
                                  [note-trigger (inc idx) note])
                                top-row)]
                    [re-com/h-box
                     :children (map-indexed
                                (fn [idx note]
                                  [note-trigger (+ 9 idx) note])
                                bottom-row)]]]))))

