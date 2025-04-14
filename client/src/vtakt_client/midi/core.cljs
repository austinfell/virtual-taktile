(ns vtakt-client.midi.core
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [vtakt-client.keyboard.core :as kb]
   [vtakt-client.keyboard.subs :as kb-subs]))

;; MIDI state atoms
(defonce midi-outputs (atom {}))
(defonce selected-output-id (atom nil))
(defonce midi-channel (atom 1))
(defonce velocity (atom 100))
(defonce previous-notes (atom #{}))

;; Initialize Web MIDI API
(defn init-midi!
  "Initialize the Web MIDI API and store available outputs."
  []
  (-> (.requestMIDIAccess js/navigator)
      (.then
       (fn [access]
         (reset! midi-outputs {})
         
         ;; Get all available MIDI outputs
         (let [outputs (.-outputs access)]
           (.forEach outputs
                     (fn [output key]
                       (swap! midi-outputs assoc key 
                              {:id key
                               :name (.-name output)
                               :manufacturer (.-manufacturer output)
                               :output output})))
           
           ;; Set up listener for device changes
           (set! (.-onstatechange access)
                 (fn [e]
                   (let [port (.-port e)
                         state (.-state port)
                         type (.-type port)]
                     ;; Only handle output devices
                     (when (= type "output")
                       (if (= state "connected")
                         ;; Add new device or update existing
                         (swap! midi-outputs assoc (.-id port) 
                                {:id (.-id port)
                                 :name (.-name port)
                                 :manufacturer (.-manufacturer port)
                                 :output port})
                         ;; Remove disconnected device
                         (swap! midi-outputs dissoc (.-id port)))))))
           
           ;; Auto-select first output if available and none selected
           (when (and (nil? @selected-output-id) (seq @midi-outputs))
             (reset! selected-output-id (-> @midi-outputs first first)))))
       
       ;; Handle errors
       (fn [err]
         (js/console.error "Failed to initialize MIDI:" err))))
  nil)

;; Convert note to MIDI note number
(defn note->midi-number
  "Convert a note map {:name :c, :octave 4} to MIDI note number."
  [note]
  (when note
    (let [note-values {:c 0, :csdf 1, :d 2, :dsef 3,
                       :e 4, :f 5, :fsgf 6, :g 7,
                       :gsaf 8, :a 9, :asbf 10, :b 11}
          base-value (get note-values (:name note) 0)
          octave-value (* 12 (+ (:octave note) 1))]
      (+ base-value octave-value))))

;; Get the currently selected MIDI output device
(defn get-selected-output []
  (when @selected-output-id
    (get-in @midi-outputs [@selected-output-id :output])))

;; Send MIDI note on message
(defn send-note-on!
  "Send a MIDI note on message for the given note."
  [note]
  (when (and (get-selected-output))
    (let [output (get-selected-output)
          note-num (note->midi-number note)
          vel @velocity
          channel (dec @midi-channel)] ;; MIDI channels are 0-15 internally
      (when (and note-num (>= note-num 0) (<= note-num 127))
        (.send output (clj->js [(bit-or 0x90 channel) ;; Note on command
                                note-num
                                vel]))))))

;; Send MIDI note off message
(defn send-note-off!
  "Send a MIDI note off message for the given note."
  [note]
  (when (and (get-selected-output))
    (let [output (get-selected-output)
          note-num (note->midi-number note)
          channel (dec @midi-channel)] ;; MIDI channels are 0-15 internally
      (when (and note-num (>= note-num 0) (<= note-num 127))
        (.send output (clj->js [(bit-or 0x80 channel) ;; Note off command
                                note-num
                                0]))))))

;; Handle note changes
(defn handle-note-changes!
  "Compare previous and current notes, sending appropriate MIDI messages."
  [current-notes]
  (let [prev @previous-notes
        notes-to-turn-off (clojure.set/difference prev current-notes)
        notes-to-turn-on (clojure.set/difference current-notes prev)]
    
    ;; Turn off notes that are no longer pressed
    (doseq [note notes-to-turn-off]
      (send-note-off! note))
    
    ;; Turn on newly pressed notes
    (doseq [note notes-to-turn-on]
      (send-note-on! note))
    
    ;; Update previous notes
    (reset! previous-notes current-notes)))

;; Re-frame subscription to monitor pressed notes
(defn start-midi-listener! []
  (re-frame/reg-sub-raw
   ::midi-notes-listener
   (fn [_ _]
     (let [pressed-notes (re-frame/subscribe [::kb-subs/pressed-notes])]
       (r/track!
        (fn []
          (handle-note-changes! @pressed-notes)
          @pressed-notes))))))

;; Initialize the entire MIDI system
(defn init! []
  (init-midi!)
  (start-midi-listener!))

(re-frame/reg-event-db
 ::set-midi-channel
 (fn [db [_ channel]]
   (reset! midi-channel channel)
   db))

(re-frame/reg-event-db
 ::set-midi-velocity
 (fn [db [_ vel]]
   (reset! velocity vel)
   db))

(re-frame/reg-event-db
 ::set-selected-output
 (fn [db [_ output-id]]
   (reset! selected-output-id output-id)
   db))

;; Re-frame subscriptions
(re-frame/reg-sub
 ::midi-outputs
 (fn [_ _]
   @midi-outputs))

(re-frame/reg-sub
 ::selected-output-id
 (fn [_ _]
   @selected-output-id))

(re-frame/reg-sub
 ::midi-channel
 (fn [_ _]
   @midi-channel))

(re-frame/reg-sub
 ::velocity
 (fn [_ _]
   @velocity))
