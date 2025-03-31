(ns vtakt-client.keyboard.keyboard-controls
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.keyboard.events :as events]
   [vtakt-client.keyboard.subs :as subs]
   [vtakt-client.keyboard.core :as kb]))

;; Map keyboard keys to positions in the keyboard layout
;; First row: a s d f g h j k (positions 1-8)  <- TOP row
;; Second row: z x c v b n m , (positions 9-16) <- BOTTOM row
(def key-to-position-map
  {"a" 1, "s" 2, "d" 3, "f" 4, "g" 5, "h" 6, "j" 7, "k" 8,
   "z" 9, "x" 10, "c" 11, "v" 12, "b" 13, "n" 14, "m" 15, "," 16})

;; Track which keys are currently pressed
(def pressed-keys (atom #{}))

;; Store the currently active note for chord mode
(def active-chord-note (atom nil))

;; Convert keyboard position to the corresponding note
(defn- position-to-note [position keyboard]
  (let [keyboard-rows (kb/rows keyboard)
        row (if (<= position 8) :top :bottom)
        index (if (<= position 8)
                (dec position)  ;; 1-based to 0-based for first row
                (- position 9)) ;; Adjust for second row
        notes (get keyboard-rows row)]
    (get notes index)))

;; Handle key press events
(defn handle-key-down [event]
  (let [key (.-key event)
        position (get key-to-position-map key)]

    (when (and position (not (contains? @pressed-keys key)))
      ;; Prevent default browser behavior
      (.preventDefault event)

      ;; Add to pressed keys
      (swap! pressed-keys conj key)

      (let [keyboard @(re-frame/subscribe [::subs/keyboard])
            note (position-to-note position keyboard)
            chord-mode? (not= @(re-frame/subscribe [::subs/selected-chromatic-chord]) :single-note)]

        (when note
          (if chord-mode?
            ;; Chord mode (monophonic/legato)
            (do
              ;; First clear all notes
              (re-frame/dispatch [::events/trigger-note nil])
              ;; Store the active chord note
              (reset! active-chord-note note)
              ;; Then trigger the new note
              (re-frame/dispatch [::events/trigger-note note]))

            ;; Single note mode (polyphonic)
            ;; In single note mode, we need to set pressed-notes directly
            ;; to avoid duplicating notes
            (let [current-pressed (set @(re-frame/subscribe [::subs/pressed-notes]))]
              (when-not (contains? current-pressed note)
                (re-frame/dispatch [::events/trigger-note note])))))))))

;; Handle key release events
(defn handle-key-up [event]
  (let [key (.-key event)
        position (get key-to-position-map key)]

    (when (and position (contains? @pressed-keys key))
      (.preventDefault event)

      ;; Remove from pressed keys
      (swap! pressed-keys disj key)

      (let [keyboard @(re-frame/subscribe [::subs/keyboard])
            note (position-to-note position keyboard)
            chord-mode? (not= @(re-frame/subscribe [::subs/selected-chromatic-chord]) :single-note)]

        (when note
          (if chord-mode?
            ;; In chord mode:
            (if (empty? @pressed-keys)
              ;; If all keys released, release all notes
              (do
                (reset! active-chord-note nil)
                (re-frame/dispatch [::events/trigger-note nil]))
              ;; If other keys still pressed, find the last pressed key and play its note
              (let [last-key (last @pressed-keys)
                    last-position (get key-to-position-map last-key)
                    last-note (position-to-note last-position keyboard)]
                (when (and last-note (not= @active-chord-note last-note))
                  (re-frame/dispatch [::events/trigger-note nil])
                  (reset! active-chord-note last-note)
                  (re-frame/dispatch [::events/trigger-note last-note]))))

            ;; In single note mode:
            ;; We need to modify the events.cljs file to handle proper note removal
            ;; For now, just send nil to clear all notes
            (re-frame/dispatch [::events/trigger-note nil])))))))

;; Initialize keyboard event listeners
(defn init-keyboard-listeners []
  (.addEventListener js/window "keydown" handle-key-down)
  (.addEventListener js/window "keyup" handle-key-up)
  (js/console.log "Keyboard listeners initialized"))

;; Clean up event listeners when no longer needed
(defn cleanup-keyboard-listeners []
  (.removeEventListener js/window "keydown" handle-key-down)
  (.removeEventListener js/window "keyup" handle-key-up))
