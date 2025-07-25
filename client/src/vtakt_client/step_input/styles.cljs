(ns vtakt-client.step-input.styles
  (:require
   [vtakt-client.styles :as app-styles]
   [vtakt-client.styles :refer [colors]]
   [spade.core :refer [defclass]]
   [garden.units :refer [px]]
   [garden.color :as color]))

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
  {:color "red"})

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

