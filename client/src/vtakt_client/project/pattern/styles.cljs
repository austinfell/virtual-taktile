(ns vtakt-client.project.pattern.styles
  (:require
   [vtakt-client.styles :as app-styles :refer [colors]]
   [spade.core :refer [defclass]]))

;; Pattern selection component
(defclass pattern []
  {:background-color "#1a1a1a"
   :color "#4a9eff"
   :display :flex
   :align-items :center
   :justify-content :center
   :width "37.2px"
   :height "37.2px"
   :border "1px solid #333"
   :border-radius "2px"
   :cursor :pointer
   :transition "all 0.15s ease"
   :font-weight "500"
   :font-size "14px"}

  [:&:hover
   {:background-color "#2a2a2a"
    :color "#6bb6ff"}])

(defclass pattern-active []
  {:background-color "#4a9eff"
   :color "#1a1a1a"
   :font-weight "600"
   :box-shadow "0 0 8px rgba(74, 158, 255, 0.3)"}
  [:&:hover
   {:background-color "#6bb6ff"
    :box-shadow "0 0 12px rgba(74, 158, 255, 0.5)"}])
