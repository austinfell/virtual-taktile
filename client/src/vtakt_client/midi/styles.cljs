(ns vtakt-client.midi.styles
  (:require
   [spade.core  :refer [defglobal defclass]]
   [vtakt-client.styles :as general-styles]))

(defclass status-notification []
  {:margin-bottom 0})

(defclass midi-row []
  {:display "flex" :align-items "center"})

(defclass midi-key-name []
  {:color "black" :margin 0  :margin-right "5px"})

(defclass midi-channel-row []
  {:margin-top "10px"})

