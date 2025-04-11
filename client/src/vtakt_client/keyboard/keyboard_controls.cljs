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

(defonce active-notes (atom []))

;; Convert keyboard position to the corresponding note
(defn- position-to-note [position keyboard]
  (let [keyboard-rows (kb/rows keyboard)
        row (if (<= position 8) :top :bottom)
        index (if (<= position 8)
                (dec position)  ;; 1-based to 0-based for first row
                (- position 9)) ;; Adjust for second row
        notes (get keyboard-rows row)]
    (get notes index)))

(defn handle-key-up [event keyboard legato?]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when position
      (let [note (position-to-note position keyboard)]
        (when note
          (let [current-notes (swap! active-notes #(filterv (fn [n] (not= n note)) %))]
            (re-frame/dispatch-sync [::events/set-physical-notes
                                     (if legato? [(last current-notes)] current-notes)])))))))

(defn handle-key-down [event keyboard legato?]
  (let [key (.-key event)
        position (get key-to-position-map key)]
    (when position
      (let [note (position-to-note position keyboard)]
        (when note
          (let [current-notes (swap! active-notes conj note)]
            (re-frame/dispatch-sync [::events/set-physical-notes (if legato? [(last current-notes)] current-notes)])))))))


(defonce keyboard-event-handlers (atom {}))

(defn init-keyboard-listeners [keyboard legato]
  ;; First clean up any existing listeners
  ;; Create the bound handler functions with the current keyboard
  (let [key-down-handler #(handle-key-down % keyboard legato)
        key-up-handler #(handle-key-up % keyboard legato)]

    ;; Store references to these handlers
    (reset! keyboard-event-handlers
            {:key-down key-down-handler
             :key-up key-up-handler})

    ;; Add event listeners with these specific handlers
    (.addEventListener js/window "keydown" key-down-handler)
    (.addEventListener js/window "keyup" key-up-handler)))

(defn cleanup-keyboard-listeners []
  (when-let [handlers @keyboard-event-handlers]
    (when-let [key-down-handler (:key-down handlers)]
      (.removeEventListener js/window "keydown" key-down-handler))
    (when-let [key-up-handler (:key-up handlers)]
      (.removeEventListener js/window "keyup" key-up-handler))
    (reset! keyboard-event-handlers nil)))
