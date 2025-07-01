(ns vtakt-client.project.pattern.views
  (:require
   [re-com.core :as re-com :refer [at]]
   [vtakt-client.project.pattern.styles :as styles]
   [vtakt-client.styles :as general-styles]
   [vtakt-client.project.pattern.subs :as subs]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]))

(defn bank-and-pattern-select []
  (let [active-bank (re-frame/subscribe [::subs/active-bank])
        selected-bank (reagent/atom @active-bank)]
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
           :on-change #(reset! selected-bank %)]

          [re-com/v-box
           :gap "4px"
           :children
           (mapv (fn [row]
                   [re-com/h-box
                    :gap "4px"
                    :children
                    (mapv (fn [col]
                            (let [pattern-num (+ (* row 8) col 1)]
                              [:div
                               {:key pattern-num
                                :class (styles/pattern)}
                               (str pattern-num)]))
                          (range 8))])
                 (range 2))]]]]])))
