(ns vtakt-client.project.track.views
  (:require
   [vtakt-client.styles :as general-styles]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com]
   [vtakt-client.project.track.subs :as subs]))

(defn track-selector []
  [re-com/horizontal-tabs
   :model @selected-tab
   :tabs [{:id :track-1 :label "1"}
           {:id :track-2 :label "2"}
           {:id :track-3 :label "3"}
           {:id :track-4 :label "4"}]
   :on-change #(reset! selected-tab %)])
