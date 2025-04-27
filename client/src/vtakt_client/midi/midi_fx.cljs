(ns vtakt-client.midi.midi-fx
  (:require
   [re-frame.core :refer [reg-fx dispatch]]))

;; When the module is loaded, we *need* access to midi outputs.
(def midi-outputs (atom nil))
(when (nil? @midi-outputs)
    (-> (.requestMIDIAccess js/navigator)
        (.then
         (fn [access]
           (let [outputs (.-outputs access)]
             (swap! midi-outputs {})
             (.forEach outputs
                       (fn [output key]
                         (swap! midi-outputs assoc key
                                {:name (.-name output)
                                 :manufacturer (.-manufacturer output)
                                 :output output})))

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
                                  {:name (.-name port)
                                   :manufacturer (.-manufacturer port)
                                   :output port})
                           (swap! midi-outputs dissoc (.-id port)))))))))
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
      (println e)
      (when on-failure
        (dispatch on-failure)))))

(reg-fx
 :midi
 (fn [midi-map]
   (if (map? midi-map)
     (midi-effect midi-map)
     (doseq [msg midi-map]
       (midi-effect msg)))))
