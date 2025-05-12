(ns vtakt-client.styles
  (:require-macros
   [garden.def :refer [defcssfn]])
  (:require
   [spade.core   :refer [defglobal defclass]]
   [garden.units :refer [deg px]]
   [garden.color :as color]))

(defcssfn linear-gradient
 ([c1 p1 c2 p2]
  [[c1 p1] [c2 p2]])
 ([dir c1 p1 c2 p2]
  [dir [c1 p1] [c2 p2]]))

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

(defglobal defaults
  [:body
   {:color               :red
    :background-color    :#ddd
    :background-image    [(linear-gradient :white (px 2) :transparent (px 2))
                          (linear-gradient (deg 90) :white (px 2) :transparent (px 2))
                          (linear-gradient (color/rgba 255 255 255 0.3) (px 1) :transparent (px 1))
                          (linear-gradient (deg 90) (color/rgba 255 255 255 0.3) (px 1) :transparent (px 1))]
    :background-size     [[(px 100) (px 100)] [(px 100) (px 100)] [(px 20) (px 20)] [(px 20) (px 20)]]
    :background-position [[(px -2) (px -2)] [(px -2) (px -2)] [(px -1) (px -1)] [(px -1) (px -1)]]}])

;; Helper functions to convert the class names to style maps for use with re-com
(defn class->style [class-fn & args]
  {:class (apply class-fn args)})

;; General styles for styling application components.
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
