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
                        (re-frame/dispatch dec-event))]
          [re-com/box
           :class (styles/increment-value-box)
           :child [:p {:class (styles/increment-value)} (render-fn value)]]
          [re-com/button
           :label "♯"
           :disabled? inc-disabled?
           :class (str (styles/increment-button) " "
                       (when-not inc-disabled? (styles/increment-button-active)))
           :on-click #(when-not inc-disabled?
                        (re-frame/dispatch inc-event))]
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
      :attr {:on-mouse-down #(re-frame/dispatch [::events/trigger-note note])
             :on-mouse-up #(re-frame/dispatch [::events/trigger-note nil])
             :on-mouse-leave #(re-frame/dispatch [::events/trigger-note nil])}
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

(defn- is-note-pressed?
  "Determines if a note should be shown as pressed based on the current state.

   Parameters:
   - note: The note to check
   - idx: The position index in the keyboard
   - pressed-notes: Collection of currently pressed notes
   - chord-mode?: Whether chord mode is active"
  [note idx pressed-notes chord-mode?]
  (when (and note pressed-notes)
    (if chord-mode?
      ;; In chord mode, check note name and handle special case for idx 7
      (and (some #(= (:name note) (:name %)) pressed-notes)
           (or (not= idx 7) (not chord-mode?)))
      ;; In normal mode, check exact note (name and octave)
      (some #(and (= (:name note) (:name %))
                 (= (:octave note) (:octave %)))
            pressed-notes))))

(defn- white-keys-layer
  "Renders the white keys of the piano keyboard.

   Parameters:
   - white-notes: Collection of white notes to display
   - pressed-notes: Collection of currently pressed notes
   - chord-mode?: Whether chord mode is active"
  [white-notes pressed-notes chord-mode?]
  (mapv (fn [idx note]
          [piano-key note (is-note-pressed? note idx pressed-notes chord-mode?) :white])
        (range)
        white-notes))

(defn- black-keys-layer
  "Renders the black keys of the piano keyboard as an overlay.

   Parameters:
   - black-key-positions: Collection of [note position-index octave-note] tuples
   - pressed-notes: Collection of currently pressed notes"
  [black-key-positions pressed-notes]
  (mapv (fn [[note position-index _]]
          (let [pressed? (when (and note pressed-notes)
                           ;; For black keys, always just check the note name
                           (some #(= (:name note) (:name %)) pressed-notes))]
            [re-com/box
             :class (styles/black-key-position position-index)
             :child [piano-key note pressed? :black]]))
        black-key-positions))

(defn- prepare-black-key-positions
  "Prepares the positioning data for black keys.

   Parameters:
   - black-notes: Collection of black notes from the keyboard
   - keyboard-root: The root note of the keyboard"
  [black-notes keyboard-root]
  (let [octave-notes (take 12 (filter #(kb/natural-note? %)
                                     (iterate #(kb/shift-note % :up)
                                              (kb/transpose-note keyboard-root 1))))]
    (filter #(contains? #{:d :e :g :a :b} (:name (nth % 2)))
            (map vector
                 (rest black-notes)
                 [1 2 3 4 5 6 7]
                 octave-notes))))

(s/fdef octave-view
  :args (s/cat)
  :ret ::us/reagent-component)
(defn octave-view
  "Renders a piano-like octave view showing which notes are in the current scale."
  []
  (let [keyboard (re-frame/subscribe [::subs/chromatic-keyboard])
        keyboard-root (re-frame/subscribe [::subs/keyboard-root])
        pressed-notes (re-frame/subscribe [::subs/pressed-notes])
        selected-chord (re-frame/subscribe [::subs/selected-chord])
        chord-mode? (not= @selected-chord :off)]
    (fn []
      (let [white-notes (:bottom (kb/rows @keyboard))
            black-notes (:top (kb/rows @keyboard))
            black-key-positions (prepare-black-key-positions black-notes @keyboard-root)]
        [re-com/box
         :class (styles/octave-view)
         :child
         [re-com/v-box
          :gap "10px"
          :children
          [[re-com/box
            :class (styles/keys-container)
            :child
            [re-com/h-box
             :class (styles/keys-relative-container)
             :children
             (concat
              (white-keys-layer white-notes @pressed-notes chord-mode?)
              (black-keys-layer black-key-positions @pressed-notes))]]]]]))))

(defn- note-column
  "Renders a column of note labels.

   Parameters:
   - notes: A sequence of note maps to display in this column"
  [notes]
  [re-com/v-box
   :align :center
   :justify :center
   :gap "5px"
   :children
   (for [note notes]
     ^{:key (str (hash note))}
     [re-com/label
      :class (styles/note-label)
      :label (kb/format-root-note note)])])

(defn- empty-state
  "Renders the empty state when no notes are pressed."
  []
  [re-com/v-box
   :align :center
   :justify :center
   :height "100%"
   :children
   [[re-com/label
     :class (styles/empty-notes-label)
     :label "No notes"]]])

(s/fdef pressed-notes-display
  :args (s/cat)
  :ret ::us/reagent-component)
(defn pressed-notes-display
  "Displays currently pressed notes in columns of up to 4 notes each.
   Shows an empty state message when no notes are pressed."
  []
  (let [pressed-notes (re-frame/subscribe [::subs/pressed-notes])]
    (fn []
      [re-com/box
       :class (styles/pressed-notes-container)
       :child
       (if (seq @pressed-notes)
         [re-com/h-box
          :align :center
          :justify :center
          :gap "10px"
          :children
          (for [column (partition-all 4 @pressed-notes)]
            ^{:key (str "col-" (hash (first column)))}
            [note-column column])]
         [empty-state])])))

;; ------------------------------
;; Main Components
;; ------------------------------
(defn- control-section
  [label content]
  [re-com/v-box
   :gap "10px"
   :children
   [[re-com/label
     :class (styles/section-label)
     :label label]
    content]])

(s/fdef keyboard-configurator
  :args (s/cat)
  :ret ::us/reagent-component)
(defn keyboard-configurator
  "A comprehensive configuration panel for the musical keyboard interface.

  This component provides a complete user interface for controlling and configuring
  the keyboard's behavior, including:

  - Real-time display of currently pressed notes
  - Visual octave representation showing scale notes
  - Keyboard mode selection (chromatic or folding)
  - Scale selection with support for multiple musical scales
  - Root note control with semitone adjustment
  - Chord selection with various chord types
  - Transposition control to shift played notes

  The configurator is organized into two main rows of controls:
  1. Display controls (Notes, Octave Display, Keyboard Mode)
  2. Musical theory controls (Scale, Root, Chord, Transpose)

  Each control is rendered within a standardized section layout for visual consistency.

  This component subscribes to multiple re-frame subscriptions to maintain its state:
  - ::subs/keyboard - The current keyboard state
  - ::subs/keyboard-transpose - Current transposition value
  - ::subs/keyboard-mode - Current keyboard mode (chromatic or folding)
  - ::subs/selected-chord - Currently selected chord
  - ::subs/chords - Available chord options
  - ::subs/selected-scale - Currently selected scale
  - ::subs/scales - Available scale options

  Returns:
    A Reagent component rendering the complete configuration interface.

  Example usage:
    [keyboard-configurator]

  See also:
    control-section, pressed-notes-display, octave-view, keyboard-mode-selector,
    scale-selector, root-note-control, chord-selector, transpose-control"
  []
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
         [[control-section "Notes" [pressed-notes-display]]
          [control-section "Octave Display" [octave-view]]
          [control-section "Keyboard Mode" [keyboard-mode-selector @keyboard-mode]]]]
        [re-com/h-box
         :gap "20px"
         :align :center
         :children
         [[control-section "Scale" [scale-selector @available-scales @selected-scale]]
          [control-section "Root" [root-note-control (:root-note @ck)]]
          [control-section "Chord" [chord-selector @available-chords @selected-chord]]
          [control-section "Transpose" [transpose-control @transpose]]]]]])))

(s/fdef keyboard
  :args (s/cat)
  :ret ::us/reagent-component)
(defn keyboard
  "Renders an interactive musical keyboard interface with trigger buttons arranged in two rows.

  This component creates a virtual keyboard consisting of two rows of notes that users
  can interact with to trigger musical notes and chords. The keyboard layout is determined
  by the current keyboard state (either chromatic or folding), which includes information
  about note arrangement, active scales, and other musical parameters.

  The keyboard uses the following structure:
  - A top row of buttons representing the first 8 positions (1-8)
  - A bottom row of buttons representing the next 8 positions (9-16)

  Each position renders a note-trigger component that:
  - Displays the position number
  - Shows special styling for measure starts (positions 1, 5, 9, 13)
  - Enables playing notes when clicked
  - Indicates when a position has no associated note

  The component subscribes to:
  - ::subs/keyboard - The current keyboard state with full note arrangement

  The actual notes played depend on several factors:
  - Current keyboard mode (chromatic or folding)
  - Selected scale
  - Root note
  - Transposition value
  - Chord settings

  Notes are displayed differently in each keyboard mode:
  - Chromatic: Natural notes on bottom row, accidentals (sharps/flats) on top row
  - Folding: Sequential notes arranged to optimize playability

  Returns:
    A Reagent component rendering the interactive keyboard interface.

  Example usage:
    [keyboard]

  See also:
    note-trigger, keyboard-configurator, kb/rows"
  []
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
