(ns vtakt-client.keyboard.styles
  (:require
   [vtakt-client.styles :as app-styles]))

;; Button styles
(def seq-button-style
  {:width "30px"
   :display "flex"
   :align-items "center"
   :padding 0
   :justify-content "center"
   :text-decoration "underline solid black 1px"
   :height "40px"})

(defn seq-button-number-style [n]
  (if (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
    {:style seq-button-style}
    {:style (assoc seq-button-style :text-decoration "none")}))

(def seq-number-container-style
  {:display "flex"
   :height "90%"
   :border-radius "3px"
   :justify-content "center"
   :align-items "center"
   :width "20px"
   :border "1px solid black"})

(def seq-number-style
  {:margin-bottom "0px"})

;; Increment control styles
(def increment-button-style
  {:min-width "40px"
   :font-size "16px"
   :font-weight "bold"
   :background-color "#e2e2e2"
   :border "1px solid #bbbbbb"
   :color "black"})

(def increment-value-box-style
  {:width "38px"
   :text-align "center"
   :font-weight "bold"
   :display "flex"
   :align-items "center"
   :justify-content "center"
   :color "#333333"})

(def increment-value-style
  {:width "100%"
   :position "relative"
   :top "5px"})

;; Keyboard mode selector styles
(def mode-toggle-style
  {:border "1px solid #ccc"
   :border-radius "4px"
   :overflow "hidden"
   :width "200px"
   :height "36px"
   :cursor "pointer"})

(defn mode-option-style [active?]
  {:flex "1"
   :text-align "center"
   :padding "8px"
   :background-color (if active? "#4a86e8" "#f0f0f0")
   :color (if active? "white" "black")
   :transition "all 0.2s ease"})

;; Piano key styles
(defn white-key-style [pressed? chord-mode? idx]
  {:width "28px"
   :height "60px"
   :background (if (and pressed? (or (not chord-mode?) (not= idx 7))) "#FFD700" "#FFF6A3")
   :margin "0 1px"
   :position "relative"
   :z-index 1
   :transition "background-color 0.2s ease"})

(def white-key-indicator-style
  {:width "12px"
   :height "12px"
   :border-radius "50%"
   :background-color "#d35400"
   :margin-bottom "15px"
   :position "relative"
   :left "7px"})

(defn white-key-label-style [note]
  {:position "absolute"
   :bottom 0
   :left "1px"
   :right 0
   :text-align "center"
   :color (if (not (nil? note)) "#333" "transparent")
   :font-size "10px"
   :font-weight "bold"})

(defn black-key-style [pressed? note]
  {:width "16px"
   :height "40px"
   :background (if pressed? "#8c44ad" "#333") ; Purple for pressed, dark gray for normal
   :border-left (if (not (nil? note)) "none" "2px solid black")
   :border-bottom (if (not (nil? note)) "none" "2px solid black")
   :border-right (if (not (nil? note)) "none" "2px solid black")
   :position "absolute"
   :z-index 2
   :transition "background-color 0.2s ease"})

(def black-key-indicator-style
  {:width "10px"
   :height "10px"
   :border-radius "50%"
   :background-color "#f39c12"
   :margin-top "5px"
   :position "relative"
   :left "3px"})

(def black-key-label-style
  {:position "absolute"
   :bottom "1px"
   :left "2px"
   :right 0
   :text-align "center"
   :color "#FFF6A3"
   :font-size "8px"
   :font-weight "bold"})

;; Octave view styles
(def octave-view-style
  {:background-color "#222"
   :border-radius "5px"
   :padding "10px"
   :width "260px"})

(def keys-container-style
  {:position "relative"
   :height "70px"
   :margin-top "10px"})

;; Pressed notes display styles
(def pressed-notes-container-style
  {:width "auto"
   :min-width "130px"
   :min-height "115px"
   :display "flex"
   :justify-content "center"
   :font-size "12px"
   :align-items "center"
   :background-color "#f5f5f5"
   :border "1px solid #ccc"
   :border-radius "5px"
   :padding "5px"})

(def note-label-style
  {:font-weight "bold"
   :color "black"
   :margin "2px 0"
   :white-space "nowrap"})

(def empty-notes-label-style
  {:color "#999"})

;; Keyboard configurator styles
(def configurator-container-style
  {:background-color "#f5f5f5"
   :border-radius "8px"
   :padding "15px"})

(def configurator-title-style
  {:margin-bottom "5px"})

(def section-label-style
  {:font-weight "bold"
   :color "black"})
