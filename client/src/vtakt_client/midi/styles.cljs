(ns vtakt-client.midi.styles
  (:require
   [spade.core :refer [defclass]]
   [garden.units :refer [px]]
   [garden.color :as color]))

(def colors
  {:bg-light "#f9f9f9"
   :border-light "#e0e0e0"
   :border-dark "#d0d0d0"
   :text-dark "#333333"
   :success "#27ae60"
   :error "#e74c3c"
   :active "#3498db"
   :inactive "#95a5a6"})

(defclass midi-panel []
  {:background-color (:bg-light colors)
   :border-radius (px 8)
   :padding (px 15)
   :margin-top (px 20)
   :width "100%"
   :box-shadow "0 2px 4px rgba(0,0,0,0.1)"})

(defclass panel-title []
  {:margin-bottom (px 10)
   :color (:text-dark colors)})

(defclass section-title []
  {:font-weight "bold"
   :margin-bottom (px 5)
   :color (:text-dark colors)})

(defclass status-box [connected?]
  {:background-color (if connected? (:success colors) (:error colors))
   :color "#ffffff"
   :padding (px 8)
   :border-radius (px 4)
   :text-align "center"
   :font-weight "bold"
   :transition "background-color 0.3s ease"})

(defclass dropdown []
  {:width "100%"})

(defclass control-group []
  {:border (str "1px solid " (:border-light colors))
   :border-radius (px 4)
   :padding (px 10)
   :margin-bottom (px 10)})

(defclass slider []
  {:width "100%"})

(defclass slider-value []
  {:font-size (px 14)
   :color (:text-dark colors)
   :text-align "center"})

(defclass enable-switch []
  {:display "flex"
   :align-items "center"
   :gap (px 10)})

(defclass refresh-button []
  {:background-color (:active colors)
   :color "#ffffff"
   :margin-top (px 10)
   :transition "background-color 0.2s ease"}
  [:&:hover {:background-color (color/darken (:active colors) 5)}]
  [:&:active {:background-color (color/darken (:active colors) 10)}])
