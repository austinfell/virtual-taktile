(ns vtakt-client.keyboard.styles
  (:require
   [vtakt-client.styles :as app-styles]
   [spade.core :refer [defclass defglobal]]
   [garden.units :refer [px]]
   [garden.color :as color]))

;; Color constants
(def colors
  {:white-key-default "#FFF6A3"
   :white-key-pressed "#FFD700"
   :black-key-default "#333"
   :black-key-pressed "#8c44ad"
   :indicator-white "#d35400"
   :indicator-black "#f39c12"
   :chromatic-active "#4a86e8"
   :inactive "#f0f0f0"
   :border-light "#ccc"
   :border-dark "#bbbbbb"
   :text-dark "#333333"
   :text-black "black"
   :text-white "white"
   :text-light "#999"
   :bg-light "#f5f5f5"
   :bg-dark "#222"})

;; Button styles
(defclass seq-button []
  {:width (px 30)
   :display "flex"
   :align-items "center"
   :padding 0
   :justify-content "center"
   :text-decoration "underline solid black 1px"
   :height (px 40)})

(defclass seq-button-number [n]
  (when (and (not= n 1) (not= n 5) (not= n 9) (not= n 13))
    {:text-decoration "none"}))

(defclass seq-number-container []
  {:display "flex"
   :height "90%"
   :border-radius (px 3)
   :justify-content "center"
   :align-items "center"
   :width (px 20)
   :border "1px solid black"})

(defclass seq-number []
  {:margin-bottom (px 0)})

;; Increment control styles
(defclass increment-button []
  {:min-width (px 40)
   :font-size (px 16)
   :font-weight "bold"
   :background-color (:bg-light colors)
   :border (str "1px solid " (:border-dark colors))
   :color (:text-black colors)})

(defclass increment-button-active []
  {:cursor "pointer"
   :transition "background-color 0.2s ease"
   "&:hover" {:background-color (color/lighten (:bg-light colors) 5)}})

(defclass increment-value-box []
  {:width (px 38)
   :text-align "center"
   :font-weight "bold"
   :display "flex"
   :align-items "center"
   :justify-content "center"
   :color (:text-dark colors)})

(defclass increment-value []
  {:width "100%"
   :position "relative"
   :top (px 5)})

;; Keyboard mode selector styles
(defclass mode-toggle []
  {:border (str "1px solid " (:border-light colors))
   :border-radius (px 4)
   :overflow "hidden"
   :width (px 200)
   :height (px 36)
   :cursor "pointer"})

(defclass mode-option [active?]
  {:flex 1
   :text-align "center"
   :padding (px 8)
   :background-color (if active? (:chromatic-active colors) (:inactive colors))
   :color (if active? (:text-white colors) (:text-black colors))
   :transition "all 0.2s ease"})

;; Piano key styles
(defclass white-key [pressed? chord-mode? idx]
  {:width (px 28)
   :height (px 60)
   :background (if (and pressed? (or (not chord-mode?) (not= idx 7))) 
                 (:white-key-pressed colors) 
                 (:white-key-default colors))
   :margin (str "0 " (px 1))
   :position "relative"
   :z-index 1
   :transition "background-color 0.2s ease"})

(defclass white-key-indicator []
  {:width (px 12)
   :height (px 12)
   :border-radius "50%"
   :background-color (:indicator-white colors)
   :margin-bottom (px 15)
   :position "relative"
   :left (px 7)})

(defclass white-key-label [note]
  {:position "absolute"
   :bottom 0
   :left (px 1)
   :right 0
   :text-align "center"
   :color (if (not (nil? note)) (:text-dark colors) "transparent")
   :font-size (px 10)
   :font-weight "bold"})

(defclass black-key [pressed? note]
  {:width (px 16)
   :height (px 40)
   :background (if pressed? (:black-key-pressed colors) (:black-key-default colors))
   :border-left (if (not (nil? note)) "none" "2px solid black")
   :border-bottom (if (not (nil? note)) "none" "2px solid black")
   :border-right (if (not (nil? note)) "none" "2px solid black")
   :position "absolute"
   :z-index 2
   :transition "background-color 0.2s ease"})

(defclass black-key-indicator []
  {:width (px 10)
   :height (px 10)
   :border-radius "50%"
   :background-color (:indicator-black colors)
   :margin-top (px 5)
   :position "relative"
   :left (px 3)})

(defclass black-key-label []
  {:position "absolute"
   :bottom (px 1)
   :left (px 2)
   :right 0
   :text-align "center"
   :color (:white-key-default colors)
   :font-size (px 8)
   :font-weight "bold"})

;; Octave view styles
(defclass octave-view []
  {:background-color (:bg-dark colors)
   :border-radius (px 5)
   :padding (px 10)
   :width (px 260)})

(defclass keys-container []
  {:position "relative"
   :height (px 70)
   :margin-top (px 10)})

(defclass keys-relative-container []
  {:position "relative"})

;; Pressed notes display styles
(defclass pressed-notes-container []
  {:width "auto"
   :min-width (px 130)
   :min-height (px 115)
   :display "flex"
   :justify-content "center"
   :font-size (px 12)
   :align-items "center"
   :background-color (:bg-light colors)
   :border (str "1px solid " (:border-light colors))
   :border-radius (px 5)
   :padding (px 5)})

(defclass note-label []
  {:font-weight "bold"
   :color (:text-black colors)
   :margin (str (px 2) " 0")
   :white-space "nowrap"})

(defclass empty-notes-label []
  {:color (:text-light colors)})

;; Keyboard configurator styles
(defclass configurator-container []
  {:background-color (:bg-light colors)
   :border-radius (px 8)
   :padding (px 15)})

(defclass configurator-title []
  {:margin-bottom (px 5)})

(defclass section-label []
  {:font-weight "bold"
   :color (:text-black colors)})
