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
             :color (if (or (nil? note) (= n 1)) :black :blue)
             :text-decoration "underline solid black 1px"
             :height "40px"}
     :label (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
              (str n)
              [:div {:style {:display "flex" :height "90%" :border-radius "3px" :justify-content "center" :align-items "center" :width "20px" :border "1px solid black"}} [:p {:style {:margin-bottom "0px"}} (str n)]])])


;; Representation of sequential scales as they appear on the DT and associated flat/sharp mappings.
(def sharp-notes [nil :csdf :dsef nil :fsgf :gsaf :asbf])
(def natural-notes [:c :d :e :f :g :a :b ])
(def chromatic-notes [:a :asbf :b :c :csdf :d :dsef :e :f :fsgf :g :gsaf])
(def chromatic-breakpoint-natural :c)
(def chromatic-breakpoint-accidental :csdf)

(defn scale-filter-generator [notes scale-degrees]
  (fn [scale]
    (let [offset (.indexOf notes scale)
          buff-size (+ 1 (apply max scale-degrees))
          buff (into [] (take buff-size (drop offset (cycle notes))))]
      (set (mapv buff scale-degrees)))))

(def chromatic-scales (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [1 2 3 4 5 6 7 8 9 10 11 12]) n)]) chromatic-notes)))
(def major-scales (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [0 2 4 5 7 9 11]) n)]) chromatic-notes)))
(def minor-scales (into {} (map (fn [n] [n ((scale-filter-generator chromatic-notes [0 2 3 5 7 8 10]) n)]) chromatic-notes)))
;; TODO - Generate all scales.
;; ...

(defn generate-octaves [notes octave inc-kw]
  (let [h (first notes)
        r (rest notes)
        new-octave (if (= h inc-kw) (inc octave) octave)]
    (if (empty? r)
      (list {:octave new-octave :note h})
      (lazy-seq (cons {:octave new-octave :note h} (generate-octaves r new-octave inc-kw))))))

(defn chromatic-keyboard [offset scale-filter]
  (let [sharps (map #(if (scale-filter (:note %)) % nil) (take 8 (drop offset (generate-octaves (cycle sharp-notes) 0 :csdf))))
        naturals (map #(if (scale-filter (:note %)) % nil) (take 8 (drop offset (generate-octaves (cycle natural-notes) 0 :c))))]
    [sharps naturals]))

;; TODO - Generate folding keyboard generation algorithm.
;; TODO - Generate chord mode.

(defn sequencer []
  (let [ck (chromatic-keyboard 25 (chromatic-scales :c))]
  [re-com/v-box
   :children [
              ;; TODO - We should really just use CSS to do the wrapping of 8 8 instead of defining it structurally.
              [re-com/h-box
               :children [(map seq-btn (range 1 9) (first ck))]
               ]
              [re-com/h-box
               :children [(map seq-btn (range 9 17) (second ck))]
               ]
              ]]))

(defn sequencer-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[seq-title]
              [sequencer]]])

(defmethod routes/panels :sequencer-panel [] [sequencer-panel])
