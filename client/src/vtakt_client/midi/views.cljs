(ns vtakt-client.midi.views
  (:require
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as re-frame]
   [vtakt-client.midi.styles :as styles]
   [reagent.core :as r]))

(defonce midi-outputs (r/atom {}))

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
                         (swap! midi-outputs dissoc (.-id port)))))))))

       ;; Handle errors
       (fn [err]
         (js/console.error "Failed to initialize MIDI:" err))))
  nil)

(defn midi-not-configured-alert
  "Component showing MIDI connection status."
  []
  (fn []
    [re-com/box
     :child
     [re-com/alert-box
      :alert-type :danger
      :body "Host connection is not functioning: there may be an issue with your browser permissions."
      :heading "MIDI Not Connected"]]))

(defn midi-configurator
  "Root level component that allows configuration of midi"
  []
  (r/create-class
   {:component-did-mount
    (fn [this]
      (init-midi!)
      (println @midi-outputs)
      (println "MIDI configurator mounted"))

    :reagent-render
    (fn []
      [re-com/v-box
       :class (styles/configurator-container)
       :gap "15px"
       :children [[re-com/title
                   :label "MIDI Configuration"
                   :level :level2
                   :class (styles/configurator-title)]
                  (when (empty? @midi-outputs)
                    [midi-not-configured-alert])]])}))
