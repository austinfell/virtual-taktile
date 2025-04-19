(ns vtakt-client.midi.events
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::initialize-midi
 (fn [{:keys [db]} _]
   {:db (assoc db :midi-outputs {})}))

(re-frame/reg-event-fx
 ::add-midi-output
 (fn [{:keys [db]} [_ [id to-add-midi-output]]]
   (println to-add-midi-output)
   {:db (update db :midi-outputs assoc id to-add-midi-output)}))

(re-frame/reg-event-fx
 ::remove-midi-output
 (fn [{:keys [db]} [_ id]]
   {:db (update db :midi-outputs dissoc id)}))

(defn send-note-on
  "Send a note-on message to a MIDI output port.
   - midi-out: MIDIOutput object
   - note: MIDI note number (0-127)
   - velocity: note velocity (0-127)
   - channel: MIDI channel (0-15)"
  [midi-out note velocity channel]
  (let [status (bit-or 0x90 channel)  ; 0x90 is note-on command
        note-value (js/Math.min (js/Math.max 0 note) 127)
        velocity-value (js/Math.min (js/Math.max 0 velocity) 127)]
    (.send midi-out #js [status note-value velocity-value])))

(defn send-note-off
  "Send a note-off message to a MIDI output port.
   - midi-out: MIDIOutput object
   - note: MIDI note number (0-127)
   - velocity: release velocity (0-127, often 0)
   - channel: MIDI channel (0-15)"
  [midi-out note velocity channel]
  (let [status (bit-or 0x80 channel)  ; 0x80 is note-off command
        note-value (js/Math.min (js/Math.max 0 note) 127)
        velocity-value (js/Math.min (js/Math.max 0 velocity) 127)]
    (.send midi-out #js [status note-value velocity-value])))

(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])

(defn note->midi-number
  "Convert a note map with :name and :octave to a MIDI note number.
   Example: {:name :c :octave 4} -> 60 (middle C)"
  [{:keys [name octave]}]
  (let [base-octave 4      ; MIDI standard: C4 (middle C) = 60
        c-index    (.indexOf chromatic-notes :c)
        note-index (.indexOf chromatic-notes name)]
    (if (= note-index -1)
      (throw (js/Error. (str "Invalid note name: " name)))
      (let [octave-offset  (* 12 (- octave base-octave))
            ; Handle note indices properly - :c is at position 3 in your scale
            ; We need to adjust based on this since we want :c4 = 60
            c-based-offset (mod (- note-index c-index) 12)
            middle-c       60]
        (+ middle-c octave-offset c-based-offset)))))

(re-frame/reg-event-fx
 ::note-on
 (fn [{:keys [db]} [_ note]]
   (do
     (send-note-on
      (:output (first (vals (db :midi-outputs)))) (note->midi-number note) 50 0)
     {:db db})))

(re-frame/reg-event-fx
 ::note-off
 (fn [{:keys [db]} [_ note]]
   (do
     (send-note-off
      (:output (first (vals (db :midi-outputs)))) (note->midi-number note) 50 0)
     {:db db})))
