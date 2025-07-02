(ns vtakt-client.project.track.styles
  (:require
   [spade.core :refer [defclass]]))

(def track-colors
  {1 {:bg "#cc4125" :hover "#e55a3a"}
   2 {:bg "#d4af37" :hover "#e6c14d"}
   3 {:bg "#2d8659" :hover "#3ca06b"}
   4 {:bg "#7b4397" :hover "#9257b3"}})

(defclass track [track-num]
    {:background-color (:bg (get track-colors track-num))
     :color "#ffffff"
     :display :flex
     :align-items :center
     :justify-content :center
     :width "60px"
     :height "60px"
     :border "2px solid transparent"
     :border-radius "4px"
     :cursor :pointer
     :transition "all 0.2s ease"
     :font-weight "600"
     :font-size "18px"
     :text-shadow "0 1px 2px rgba(0,0,0,0.3)"}

    [:&:hover
     {:background-color (:hover (get track-colors track-num))
      :box-shadow "0 4px 12px rgba(0,0,0,0.2)"}])

(defclass track-active [track-num]
  {:filter "brightness(0.7) saturate(1.2)"
   :border "2px solid #ffffff"
   :box-shadow "0 0 16px rgba(255,255,255,0.6), inset 0 2px 8px rgba(0,0,0,0.4)"}
  [:&:hover
   {:filter "brightness(0.8) saturate(1.2)"
    :box-shadow "0 0 20px rgba(255,255,255,0.8), inset 0 2px 12px rgba(0,0,0,0.5)"}])
