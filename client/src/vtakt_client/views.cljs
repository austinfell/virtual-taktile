(ns vtakt-client.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.styles :as styles]
   [vtakt-client.events :as events]
   [vtakt-client.routes :as routes]
   [vtakt-client.subs :as subs]))

;; home
(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Hello from " @name ". This is the Home Page." )
     :level :level1
     :class (styles/level1)]))

(defn link-to-about-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to About Page"
   :on-click #(re-frame/dispatch [::events/navigate :about])])

(defn home-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[home-title]
              [link-to-about-page]]])


(defmethod routes/panels :home-panel [] [home-panel])

;; about

(defn about-title []
  [re-com/title
   :src   (at)
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to Home Page"
   :on-click #(re-frame/dispatch [::events/navigate :home])])

(defn about-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[about-title]
              [link-to-home-page]]])

(defmethod routes/panels :about-panel [] [about-panel])

;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(routes/panels @active-panel)]]))

;; sequencer
;; TODO - Extract to separate file.
(defn seq-title []
  [re-com/title
   :src   (at)
   :label "VTakt Sequencer"
   :level :level1])

(defn seq-btn [n note]
  [re-com/button
     :style {:width "30px"
             :display "flex"
             :align-items "center"
             :padding 0
             :justify-content "center"
             :color (if (nil? note) :black :blue)
             :text-decoration "underline solid black 1px"
             :height "40px"}
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:style {:display "flex" :height "90%" :border-radius "3px" :justify-content "center" :align-items "center" :width "20px" :border "1px solid black"}} [:p {:style {:margin-bottom "0px"}} (str n)]])])


;; Representation of sequential scales as they appear on the DT and associated flat/sharp mappings.
(def naturals [:a :b :c :d :e :f :g])
(def accidentals [:gsaf :asbf :csdf :dsef :fsgf])
(def notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])
(def flat-mappings
  {
   :a :gsaf
   :b :asbf
   :d :csdf
   :e :dsef
   :g :fsgf
   })

(defn generate-sharps [naturals]
  "Takes a list of natural notes and maps them to their associated flats...
   Except for the first element... See below.

   Implements a fun little quirk of the Digitone's midi input keyboard: basically,
   Digitone functions by mapping flats in parallel with their natural notes...
   Except the first note in the row, which when being used as a chromatic keyboard
   always is empty (nil).

   Looks funky - but this is how the physical hardware behaves."
  (into [nil] (map flat-mappings (rest naturals))))

(defn shift-list [l n]
  "Takes a given ordered collection 'l' and shifts the elements right by 'n'.
   This also has wrapping behavior such that if n > (count l), then it will
   loop back (aka use modulo arithmetic). It also accepts negative values which
   will cause the shift to occur in a leftwards direction.

   Example: (shift-right [1 2 3 4], 1) => [4 1 2 3]
   Example: (shift-right [1 2 3 4], 3) => [2 3 4 1]
   Example: (shift-right [1 2 3 4], 4) => [2 3 4 1]
   Example: (shift-right [1 2 3 4], 5) => [4 1 2 3]
  "
  (let [sl1 (take (- (count l) (mod n (count l))) l)
        sl2 (take-last (mod n (count l)) l)]
    (concat sl2 sl1)))

;; TODO - We will want the sequencer to take a root note and an octave and then
;; using that, determine how to render the keyboard. 
;;
;; It should also generate a data structure for each note on each key... maybe something like
;; {
;;   :note :gsaf
;;   :octave 5
;; },
;; ...
;; This could then trivially be converted to a websocket async message and then
;; ultimately to midi.
;;
;; ---------
;; Fold will be interesting... I think my favorite way of doing it is to
;; Create a method that can generate a scale "loop" lazily, then get rid
;; of all the weird "conj" "into" stuff in the view logic, and instead call
;; (def all-notes (take 12 (get-notes :a)))
;; (def naturals (map keep-natural (take 12 (get-notes :a))))
;; (def accidentals (map keep-accidental (take 12 (get-notes :a))))
;;
;; Then if you wanna do modes, other things....
;; (def all-notes (take 12 (get-notes :a)))
;; (def naturals (map (keep-dorian :a) (map keep-natural (take 12 (get-notes :a)))))
;; (def accidentals (map (keep-dorian :a) (map keep-accidental (take 12 (get-notes :a)))))
;; ^ This will nil out anything that isn't Dorian, just like the behavior of the hardware.
;;
;; Probably makes more sense to just make a higher order function "keep". That way we can work
;; purely in terms of boolean functions
;;
;; Back to fold - how would we do it?
;; Now we have an easy way to generate notes:
;; (def all-notes (get-notes :a))
;; ^ This is Lazy. So we can map and filter how we want:
;; First, just get a chromatic keyboard:
;; (def all-notes (take 24 (get-notes :a)))
;; Now just split the vector.
;;
;; Only problem... We really need to know the octaves... It is super hard with
;; all this filtering possibility to calculate that downstream: we can make assumptions
;; with chromatic keyboard, but not with the various scales that DT lets us use...
;;
;; So lets add an extra param to represent octave...
;; (def all-notes (take 24 (get-notes :a 5)))
;; Now, we can retain state some way so that loops into the B->C boundary trigger
;; an increment on the octave.
;;
;; Once we do all of this, implementing fold is ez pz
;; (def all-notes (take 16 (get-notes :a 5)))
;; Now just split the vector, 8 8
;;
;; Wanna make it only C Dorian notes?
;; (def all-notes (take 16 (filter (keep (dorian :c)) (get-notes :a 5))))
;; Again, split it up 8 8 again.

;; This will interact with a server running on the machine using websocket and Ring.
;; It will transmit a message, on click, to the server which will asyncronously send a
;; message back when it can to update the view state of the sequencer.
(defn sequencer []
  (let [natural-notes (conj (into [] (shift-list naturals -8)) (first (shift-list naturals -8)))]
  [re-com/v-box
   :children [
              ;; TODO - We should really just use CSS to do the wrapping of 8 8 instead of defining it structurally.
              [re-com/h-box
               :children [(map seq-btn (range 1 9) (generate-sharps natural-notes))]
               ]
              [re-com/h-box
               :children [(map seq-btn (range 9 17) natural-notes)]
               ]
              ]]))

(defn sequencer-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[seq-title]
              [sequencer]]])

(defmethod routes/panels :sequencer-panel [] [sequencer-panel])
