(ns vtakt-client.project.pattern.views
  (:require
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.styles :as general-styles]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]))

(defn bank-and-pattern-select []
  (let [selected-bank (reagent/atom 1)]
    (fn []
      [re-com/v-box
       :class (general-styles/configurator-container)
       :gap "15px"
       :children
       [[re-com/title
         :label "Bank & Pattern"
         :level :level2]
        [re-com/v-box
         :gap "8px"
         :children
         [[re-com/horizontal-tabs
           :model selected-bank
           :tabs (mapv (fn [bank-num]
                         {:id bank-num
                          :label (str bank-num)})
                       (range 1 9))
           :on-change #(reset! selected-bank %)]]]]])))
