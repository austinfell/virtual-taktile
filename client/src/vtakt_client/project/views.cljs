(ns vtakt-client.project.views
  (:require
   [vtakt-client.styles :as general-styles]
   [vtakt-client.project.events :as events]
   [vtakt-client.project.subs :as subs]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com]))

(defn project-name-display []
  (let [project-name (:name @(re-frame/subscribe [::subs/current-project]))]
    [:p project-name]))

(defn refresh-projects []
  [re-com/button
   :label "Refresh Projects"
   :on-click #(re-frame/dispatch [::events/load-projects])])

(defn save-project-as-btn [enabled?]
  [re-com/button
   :label "Save Project As"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/load-projects])])

(defn load-project [enabled?]
  [re-com/button
   :label "Load Project"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/load-projects])])

(defn delete-projects [enabled?]
  [re-com/button
   :label "Delete Project(s)"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/load-projects])])

(def selected-projects (reagent/atom #{}))

(defn project-manager []
  (let [current-project (re-frame/subscribe [::subs/current-project])
        loaded-projects (re-frame/subscribe [::subs/loaded-projects])]
    [re-com/v-box
     :class (general-styles/configurator-container)
     :gap "15px"
     :children [[re-com/title :label "Manage Projects" :level :level2]
                [re-com/h-box :gap "15px" :children [[:p "Project: " (:name @current-project)]]]
                [re-com/h-box :gap "15px" :children [[re-com/selection-list
                                                      :choices @loaded-projects
                                                      :model @selected-projects
                                                      :on-change #(reset! selected-projects %)
                                                      :id-fn :id
                                                      :label-fn :name
                                                      :multi-select? true]
                                                     [re-com/v-box
                                                      :gap "10px"
                                                      :children [[refresh-projects]
                                                                 [save-project-as-btn (< (count @selected-projects) 2)]
                                                                 [load-project (= (count @selected-projects) 1)]
                                                                 [delete-projects (> (count @selected-projects) 0)]]]]]]]))

(defn save-project-as []
  (let [project-name (reagent/atom "")]
    (fn []
      [re-com/v-box
       :class (general-styles/configurator-container)
       :gap "15px"
       :children
       [[re-com/title
         :label "Save Project As"
         :level :level2]
        [re-com/h-box
         :gap "15px"
         :children [[re-com/input-text
                     :model @project-name
                     :on-change #(reset! project-name %)]
                    [re-com/button
                     :label "Save As"
                     :on-click #(re-frame/dispatch [::events/save-project-as @project-name])]]]]])))

