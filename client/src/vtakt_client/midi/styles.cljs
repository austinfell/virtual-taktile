(ns vtakt-client.midi.styles
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

(defclass configurator-title []
  {:margin-bottom (px 5)})

(defclass configurator-container []
  {:background-color (:bg-light colors)
   :border-radius (px 8)
   :padding (px 15)})
