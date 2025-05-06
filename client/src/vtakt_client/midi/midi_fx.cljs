(ns vtakt-client.midi.midi-fx
  (:require
   [re-frame.core :refer [reg-fx reg-event-fx dispatch]]))

;; When the module is loaded, we *need* access to midi outputs.
;; I'm going to be eager about this for practical reasons related
;; to the UX - I don't want the user to click a note and then see
;; a request to be able to use MIDI browser side.
(def midi-outputs (atom nil))

(when (nil? @midi-outputs)
  (-> (.requestMIDIAccess js/navigator)
      (.then
       (fn [access]
         (let [outputs (.-outputs access)]
           ;; Initialize our MIDI map.
           (swap! midi-outputs {})
           (.forEach outputs
                     (fn [output key]
                       (swap! midi-outputs assoc key
                              {:name (.-name output)
                               :manufacturer (.-manufacturer output)
                               :output output})))

           ;; Sync Re-frame state... We also need to default to an initial selection.
           (dispatch [::set-midi-outputs @midi-outputs])

           ;; Sync subsequent changes.
           (set! (.-onstatechange access)
                 (fn [e]
                   (let [port (.-port e)
                         state (.-state port)
                         type (.-type port)]
                     ;; Only handle output devices
                     ;; TODO - Add logic to migrate to new midi device
                     ;; if active one changed.
                     (when (= type "output")
                       (if (= state "connected")
                         ;; Add new device or update existing
                         (swap! midi-outputs assoc (.-id port)
                                {:name (.-name port)
                                 :manufacturer (.-manufacturer port)
                                 :output port})
                         (swap! midi-outputs dissoc (.-id port)))
                       (dispatch [::set-midi-outputs @midi-outputs])
                       ))))))
       ;; Handle errors
       (fn [err]
         (js/console.error "Failed to initialize MIDI:" err)))))

(defn midi-effect [{:keys [type channel device data on-success on-failure]}]
  (try
    (let [device (get @midi-outputs device)
          output (:output device)]
      (when output
        (let [status-byte (case type
                            :note-on (bit-or 0x90 channel)
                            :note-off (bit-or 0x80 channel))
              data-bytes (case type
                           :note-on [(:note data) (:velocity data)]
                           :note-off [(:note data) (:velocity data)])]
          (.send output (clj->js (cons status-byte data-bytes)))
          (when on-success
            (dispatch on-success)))))
    (catch js/Error e
      (when on-failure
        (dispatch on-failure)))))

(reg-fx
 :midi
 (fn [midi-map]
   (if (map? midi-map)
     (midi-effect midi-map)
     (doseq [msg midi-map]
       (midi-effect msg)))))

(reg-event-fx
 ::set-midi-outputs
 (fn [{:keys [db]} [_ midi-outputs]]
   (let [selected-midi-output (:selected-midi-output db)
         calculated-midi-output (if (and (not (nil? selected-midi-output)) (get selected-midi-output midi-outputs))
                                  selected-midi-output
                                  (first (keys midi-outputs)))]
     {:db (assoc (assoc db :midi-outputs midi-outputs) :selected-midi-output calculated-midi-output)})))

(reg-event-fx
 ::set-selected-midi-output
 (fn [{:keys [db]} [_ selected-midi-output]]
   {:db (assoc db :selected-midi-output selected-midi-output)}))
