(ns vtakt-client.project.styles
  (:require
   [vtakt-client.styles :as app-styles :refer [colors]]
   [spade.core :refer [defclass]]))

;; Project Name Display
(defclass project-name-container []
  {:color (:text-black colors)})

(defclass project-name []
  {:font-weight "bold"
   :cursor "pointer"
   :color (:text-black colors)})

(defclass project-bpm []
  {:font-weight "bold"
   :margin-top "0px"
   :color (:text-black colors)})

;; List Styles
(defclass project-list []
  {:width "200px"
   :color (:text-black colors)}
  ["> .list-group" {:width "100%"}])

