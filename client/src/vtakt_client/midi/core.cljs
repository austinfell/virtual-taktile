(ns vtakt-client.midi.core
  (:require [re-frame.core :as re-frame]
            [vtakt-client.midi.events :as events]))

(defn init-midi!
  "Initialize the Web MIDI API and store available outputs."
  []
  (-> (.requestMIDIAccess js/navigator)
      (.then
       (fn [access]
         (re-frame.core/dispatch [::events/initialize-midi])
         ;; Get all available MIDI outputs
         (let [outputs (.-outputs access)]
           (.forEach outputs
                     (fn [output key]
                       (re-frame.core/dispatch
                        [::events/add-midi-output
                         [key
                         {:name (.-name output)
                          :manufacturer (.-manufacturer output)
                          :output output}]])))
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
                         (re-frame.core/dispatch
                          [::events/add-midi-output
                           [(.-id port)
                           {:name (.-name port)
                            :manufacturer (.-manufacturer port)
                            :output port}]])
                         (re-frame.core/dispatch
                          [::events/remove-midi-output
                           [(.-id port)]]))))))))
       ;; Handle errors
       (fn [err]
         (js/console.error "Failed to initialize MIDI:" err)))))
