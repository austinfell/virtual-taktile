(ns vtakt-client.step-input.styles
  (:require
   [vtakt-client.styles :as app-styles]
   [vtakt-client.styles :refer [colors]]
   [spade.core :refer [defclass]]
   [garden.units :refer [px]]
   [garden.color :as color]))

;; Button styles
(defclass step-trigger-button []
  {:width (px 30)
   :display "flex"
   :align-items "center"
   :padding 0
   :justify-content "center"
   :text-decoration "underline solid black 1px"
   :height (px 40)})

(defclass step-trigger-red []
  {:color "red"})
(defclass step-trigger-green []
  {:color "green"})
(defclass step-trigger-white []
  {:color "blue"})
(defclass step-trigger-off []
  {:color "black"})

(defclass step-number-container []
  {:display "flex"
   :height "90%"
   :border-radius (px 3)
   :justify-content "center"
   :align-items "center"
   :width (px 20)
   :border "1px solid black"})

(defclass step-number []
  {:margin-bottom (px 0)})

