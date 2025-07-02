(ns vtakt-client.project.track.views
  (:require
   [re-com.core :as re-com]
   [vtakt-client.project.track.styles :as styles]
   [vtakt-client.styles :as general-styles]
   [vtakt-client.project.track.subs :as subs]
   [vtakt-client.project.track.events :as events]
   [re-frame.core :as re-frame]))

(defn track-select []
  (let [active-track (re-frame/subscribe [::subs/active-track])]
    (fn []
      [re-com/v-box
       :class (general-styles/configurator-container)
       :gap "15px"
       :children
       [[re-com/title
         :label "Track"
         :level :level2]
        [re-com/v-box
         :gap "8px"
         :children
         (mapv (fn [row]
                 [re-com/h-box
                  :gap "8px"
                  :children
                  (mapv (fn [col]
                          (let [track-num (+ (* row 2) col 1)]
                            [:div
                             {:key track-num
                              :on-click #(re-frame/dispatch [::events/set-active-track track-num])
                              :class [(styles/track track-num)
                                      (when (= track-num @active-track)
                                        (styles/track-active track-num))]}
                             (str "T" track-num)]))
                        (range 2))])
               (range 2))]]])))
