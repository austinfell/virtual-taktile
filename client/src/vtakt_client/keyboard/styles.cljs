(ns vtakt-client.keyboard.styles
  (:require
   [vtakt-client.styles :as app-styles]
   [spade.core :refer [defclass defglobal]]
   [garden.units :refer [px]]
   [garden.color :as color]))

(def colors
  {:white-key-default "#FFFFFF"
   :white-key-pressed "#E6F0FA"
   :black-key-default "#282828"
   :black-key-pressed "#404040"
   :indicator-white "#3498db"
   :indicator-black "#2980b9"
   :chromatic-active "#34495e"
   :inactive "#f8f9fa"
   :border-light "#e0e0e0"
   :border-dark "#d0d0d0"
   :text-dark "#333333"
   :text-black "#000000"
   :text-white "#ffffff"
   :text-light "#7f8c8d"
   :bg-light "#f9f9f9"
   :bg-dark "#1e1e1e"})

;; Button styles
(defclass note-trigger-button []
  {:width (px 30)
   :display "flex"
   :align-items "center"
   :padding 0
   :justify-content "center"
   :text-decoration "underline solid black 1px"
   :height (px 40)})

;; Active state style (when there's a note)
(defclass note-trigger-active []
  {:color "blue"})

;; Inactive state style (when there's no note)
(defclass note-trigger-inactive []
  {:color "black"})

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
   :transition "background-color 0.2s ease"}
  [:&:hover {:background-color (color/darken (:bg-light colors) 5)}]
  [:&:focus {:background-color (color/darken (:bg-light colors) 5)
             :outline "2px solid #3498db"}])

(defclass increment-value-box []
  {:width (px 38)
   :text-align "center"
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
   :cursor "pointer"})

(defclass mode-option [active?]
  {:flex 1
   :text-align "center"
   :padding (px 8)
   :background-color (if active? (:chromatic-active colors) (:inactive colors))
   :color (if active? (:text-white colors) (:text-black colors))
   :transition "all 0.2s ease"}
  [:&:hover {:background-color (if active?
                                 (color/lighten (:chromatic-active colors) 5)
                                 (color/darken (:inactive colors) 5))}]
  [:&:focus {:background-color (if active?
                                 (color/lighten (:chromatic-active colors) 5)
                                 (color/darken (:inactive colors) 5))
             :outline "2px solid #3498db"}])

;; Piano key styles
(defclass white-key [pressed?]
  {:width (px 28)
   :border-right "1px solid black"
   :height (px 60)
   :background (if pressed?
                 (:white-key-pressed colors)
                 (:white-key-default colors))
   :margin (str "0 " (px 1))
   :position "relative"
   :z-index 1
   :transition "background-color 0.2s ease"})

(defclass white-key-indicator []
  {:width (px 10)
   :height (px 10)
   :border-radius "50%"
   :z-index 999
   :background-color (:indicator-white colors)
   :margin-bottom (px 15)
   :position "relative"
   :left (px 8)})

(defclass white-key-label [note]
  {:position "absolute"
   :bottom 0
   :left (px 1)
   :right 0
   :text-align "center"
   :color (if (not (nil? note)) (:text-dark colors) "transparent")
   :font-size (px 10)
   :font-weight "bold"})

(defclass black-key [pressed?]
  {:width (px 16)
   :height (px 40)
   :background (if pressed? (:black-key-pressed colors) (:black-key-default colors))
   :position "absolute"
   :z-index 2
   :transition "background-color 0.2s ease"})

;; Black key position classes
(defclass black-key-position [position-index]
  (let [positions {1 (px 19)
                   2 (px 47)
                   3 (px 75)
                   4 (px 103)
                   5 (px 131)
                   6 (px 159)
                   7 (px 187)}
        position-value (get positions position-index (px 0))]
    {:position "absolute"
     :left position-value}))

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
   :padding-left (px 10)
   :width (px 245)})

(defclass keys-container []
  {:position "relative"
   :height (px 70)
   :margin-top (px 10)})

(defclass keys-relative-container []
  {:position "relative"
   :padding-right (px 0)})

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
  {:color (:text-black colors)
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

(defclass dropdown []
  {:color "black"
   :cursor "pointer"}
  [:&:hover {:background-color (color/darken (:bg-light colors) 5)}]
  [:&:focus {:background-color (color/darken (:bg-light colors) 5)
             :outline "2px solid #3498db"}]

  ;; Target the chosen-single child
  [".chosen-single" {:cursor "pointer"
                     :transition "background-color 0.2s ease"}]
  [".chosen-single:hover" {:background-color (color/darken (:bg-light colors) 5)}]
  [".chosen-single:focus" {:background-color (color/darken (:bg-light colors) 5)
                           :outline "2px solid #3498db"}])
