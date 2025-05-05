;; Sound Generator Module (vtakt-client/sound.cljs)
(ns vtakt-client.sound
  (:require [clojure.string :as str]))

;; Audio context instance - created on demand
(def audio-context (atom nil))

;; Map of currently active oscillators
(def active-oscillators (atom {}))

;; Initialize the audio context if not already initialized
(defn ensure-audio-context []
  (when (nil? @audio-context)
    (reset! audio-context (new js/AudioContext))))

;; Function to get frequency for a note
(defn note-to-frequency [note]
  (let [note-map {:c 0, :c# 1, :d 1, :d# 2, :e 2, 
                  :f 3, :f# 4, :g 4, :g# 5, :a 5, :a# 6, :b 6}
        semitone (get note-map (:name note) 0)
        octave (:octave note)
        ;; A4 (440Hz) is our reference note
        a4-midi-note 69 
        a4-freq 440
        ;; Calculate MIDI note nuber 
        midi-note (+ (- (* 12 (+ octave 1)) 12) semitone)
        ;; Calculate the frequency based on the equal temperament formula
        freq (* a4-freq (js/Math.pow 2 (/ (- midi-note a4-midi-note) 12)))]
    freq))

;; Play a note - creates oscillator and adds to active oscillators map
(defn play-note [note]
  (ensure-audio-context)
  (let [id (str (hash note))
        ctx @audio-context
        freq (note-to-frequency note)
        oscillator (.createOscillator ctx)
        gain-node (.createGain ctx)]
    
    ;; Configure oscillator
    (set! (.-type oscillator) "sine")
    (set! (.. oscillator -frequency -value) freq)
    
    ;; Apply a simple envelope for better sound
    (set! (.. gain-node -gain -value) 0)
    (.setValueAtTime (.-gain gain-node) 0 (.-currentTime ctx))
    (.linearRampToValueAtTime (.-gain gain-node) 0.2 (+ (.-currentTime ctx) 0.01))
    
    ;; Connect nodes
    (.connect oscillator gain-node)
    (.connect gain-node (.-destination ctx))
    
    ;; Start the oscillator
    (.start oscillator)
    
    ;; Store the oscillator and gain node for later stopping
    (swap! active-oscillators assoc id {:oscillator oscillator 
                                        :gain gain-node 
                                        :note note})))

;; Stop a specific note
(defn stop-note [note]
  (let [id (str (hash note))]
    (when-let [{:keys [oscillator gain-node]} (get @active-oscillators id)]
      ;; Apply release envelope for smooth fade out
      (let [ctx @audio-context
            release-time (+ (.-currentTime ctx) 0.1)]
        (.cancelScheduledValues (.-gain gain-node) (.-currentTime ctx))
        (.setValueAtTime (.-gain gain-node) (.. gain-node -gain -value) (.-currentTime ctx))
        (.linearRampToValueAtTime (.-gain gain-node) 0 release-time)
        
        ;; Schedule stopping and cleanup after release
        (.setTimeout js/window 
                    (fn []
                      (.stop oscillator)
                      (.disconnect oscillator)
                      (.disconnect gain-node)
                      (swap! active-oscillators dissoc id))
                    (* 1000 0.11))))))

;; Update the currently sounding notes
(defn update-notes [old-notes new-notes]
  (println old-notes)
  (println new-notes)
  (let [old-set (set (map #(str (hash %)) old-notes))
        new-set (set (map #(str (hash %)) new-notes))
        
        to-stop (clojure.set/difference old-set new-set)
        to-play (clojure.set/difference new-set old-set)]
    
    ;; Stop notes that are no longer played
    (doseq [note-id to-stop]
      (when-let [{:keys [note]} (get @active-oscillators note-id)]
        (stop-note note)))
    
    ;; Start new notes
    (doseq [note new-notes]
      (when (contains? to-play (str (hash note)))
        (play-note note)))))

;; Stop all currently playing notes
(defn stop-all-notes []
  (doseq [[_ {:keys [note]}] @active-oscillators]
    (stop-note note)))
