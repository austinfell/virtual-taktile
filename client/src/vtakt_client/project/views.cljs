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

(defn load-project [enabled? project-id]
  [re-com/button
   :label "Load Project"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/load-project project-id])])

(defn save-project-as-btn [enabled? project-name]
  ;; TODO - This needs a flow where if no project name is given, we will create a modal to select
  ;; a project name.
  [re-com/button
   :label "Save Project As"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/save-project-as project-name])])

(defn delete-projects [enabled? project-ids]
  [re-com/button
   :label "Delete Project(s)"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/delete-projects project-ids])])

(defn project-manager []
  (let [current-project (re-frame/subscribe [::subs/current-project])
        loaded-projects (re-frame/subscribe [::subs/loaded-projects])
        selected-projects (re-frame/subscribe [::subs/selected-projects])]
    [re-com/v-box
     :class (general-styles/configurator-container)
     :gap "15px"
     :children [[re-com/title :label "Manage Projects" :level :level2]
                [re-com/h-box :gap "15px" :children [[:p "Project: " (:name @current-project)]]]
                [re-com/h-box :gap "15px" :children [[re-com/selection-list
                                                      :choices @loaded-projects
                                                      :model @selected-projects
                                                      :on-change #(re-frame/dispatch [::events/set-selected-projects %])
                                                      :id-fn :id
                                                      :label-fn :name
                                                      :multi-select? true]
                                                     [re-com/v-box
                                                      :gap "10px"
                                                      :children [[refresh-projects]
                                                                 [save-project-as-btn
                                                                  (< (count @selected-projects) 2)
                                                                  (first @selected-projects)]
                                                                 [load-project
                                                                  (= (count @selected-projects) 1)
                                                                  (first @selected-projects)]
                                                                 [delete-projects
                                                                  (> (count @selected-projects) 0)
                                                                  @selected-projects]]]]]]]))

(defn save-project-as []
  (let [project-name (re-frame/subscribe [::subs/project-name])]
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
                     :change-on-blur? false
                     :on-change #(re-frame/dispatch [::events/set-project-name %])]
                    [re-com/button
                     :label "Save As"
                     :disabled? (empty? @project-name)
                     :on-click #(do
                                  (re-frame/dispatch [::events/save-project-as @project-name])
                                  (re-frame/dispatch [::events/set-project-name ""]))]]]]]))

