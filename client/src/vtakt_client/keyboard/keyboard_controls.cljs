(ns vtakt-client.keyboard.keyboard-controls
  (:require
   [re-frame.core :as re-frame]
   [vtakt-client.keyboard.events :as events]
   [vtakt-client.keyboard.core :as kb]))

;; Map keyboard keys to positions in the keyboard layout
;; First row: a s d f g h j k (positions 1-8)  <- TOP row
;; Second row: z x c v b n m , (positions 9-16) <- BOTTOM row
(def key-to-position-map
  {"a" 1, "s" 2, "d" 3, "f" 4, "g" 5, "h" 6, "j" 7, "k" 8,
   "z" 9, "x" 10, "c" 11, "v" 12, "b" 13, "n" 14, "m" 15, "," 16})

;; Convert keyboard position to the corresponding note
(defn- position-to-note [position keyboard]
  (let [keyboard-rows (kb/rows keyboard)
        row (if (<= position 8) :top :bottom)
        index (if (<= position 8)
                (dec position)  ;; 1-based to 0-based for first row
                (- position 9)) ;; Adjust for second row
        notes (get keyboard-rows row)]
    (get notes index)))

(defn handle-key-up [event keyboard]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when position
      (.preventDefault event)
      (let [note (position-to-note position keyboard)]
        (when note
          (re-frame/dispatch-sync [::events/untrigger-physical-note note]))))))

(defn handle-key-down [event keyboard]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when position
      (.preventDefault event)
      (let [note (position-to-note position keyboard)]
        (when note
          (re-frame/dispatch-sync [::events/trigger-physical-note note]))))))

;; Initialize keyboard event listeners
(defn init-keyboard-listeners [keyboard]
  ;; Bug - weird polling behavior on key-down. We should render the entire chord
  ;; if the user has one pressed down and switches the transpose value or the
  ;; root value.
  (.addEventListener js/window "keydown" #(handle-key-down % keyboard))
  (.addEventListener js/window "keyup" #(handle-key-up % keyboard))
  (js/console.log "Keyboard listeners initialized"))

;; Clean up event listeners when no longer needed
(defn cleanup-keyboard-listeners []
  (.removeEventListener js/window "keydown" handle-key-down)
  (.removeEventListener js/window "keyup" handle-key-up))
