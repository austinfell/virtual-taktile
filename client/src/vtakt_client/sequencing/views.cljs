(ns vtakt-client.sequencing.views
  (:require [re-com.core :as re-com]
            [reagent.core :as reagent]))

(defn controls []
  (let [state   (reagent/atom {:playing? false :recording? false})
        blink   (reagent/atom true)
        blink-i (reagent/atom nil)]
    (fn []
      (let [{:keys [playing? recording?]} @state]
        [re-com/h-box
         :gap "8px"
         :align :center
         :style {:border "1px solid #d0d0d0"
                 :border-radius "4px"
                 :padding "10px 14px"}
         :children
         [[:div {:style {:background "black" :color "red" :padding "5px"}}
           [re-com/md-icon-button
            :md-icon-name "zmdi-circle"
            :emphasise? recording?
            :style {:color (if (and recording? @blink) "white" "red")
                    :background "black"
                    :transition "color 0.1s"
                    :outline "none"
                    :box-shadow "none"}
            :on-click #(do
                         (swap! state update :recording? not)
                         (if (:recording? @state)
                           (reset! blink-i (js/setInterval
                                            (fn [] (swap! blink not))
                                            500))
                           (do (js/clearInterval @blink-i)
                               (reset! blink true))))]]
          [:div {:style {:background "black" :padding "5px"}}
           [re-com/md-icon-button
            :md-icon-name (if playing? "zmdi-pause" "zmdi-play")
            :emphasise? playing?
            :style {:color (if playing? "white" "#7f8c8d")
                    :background "black"
                    :transition "color 0.1s"
                    :outline "none"
                    :box-shadow "none"}
            :on-click #(swap! state update :playing? not)]]
          [:div {:style {:background "black" :padding "5px"}}
           [re-com/md-icon-button
            :md-icon-name "zmdi-stop"
            :style {:color "#7f8c8d"
                    :transition "color 0.1s"
                    :background "black"
                    :outline "none"
                    :box-shadow "none"}
            :on-click #(do
                         (swap! state assoc :playing? false :recording? false)
                         (js/clearInterval @blink-i)
                         (reset! blink true))]]]]))))
