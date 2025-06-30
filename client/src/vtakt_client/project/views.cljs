(ns vtakt-client.project.views
  (:require
   [vtakt-client.styles :as general-styles]
   [vtakt-client.project.styles :as styles]
   [vtakt-client.project.events :as events]
   [vtakt-client.project.subs :as subs]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [re-com.core :as re-com]))

(defn refresh-projects []
  [re-com/button
   :label "Refresh Projects"
   :on-click #(re-frame/dispatch [::events/load-projects])])

(defn load-project [enabled? project-id]
  [re-com/button
   :label "Load Project"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/load-project project-id])])

(defn delete-projects [enabled? project-ids]
  [re-com/button
   :label "Delete Project(s)"
   :disabled? (not enabled?)
   :on-click #(re-frame/dispatch [::events/delete-projects project-ids])])

(defn project-editable-field [{:keys [field-name field-type project-key event]}]
  (let [new-value (reagent/atom (case field-type
                                 :string ""
                                 :number 0))
        editing (reagent/atom false)]
    (fn [current-project]
      [:p {:class (styles/project-name-container)} (str field-name ": ")
       (if @editing
         [:input {:type (case field-type
                                 :string "text"
                                 :number "number")
                  :value @new-value
                  :auto-focus true
                  :on-change #(reset! new-value (-> % .-target .-value))
                  :on-key-down (fn [e]
                                 (case (.-key e)
                                   "Enter" (do
                                             (re-frame/dispatch [event (case field-type
                                                                         :string @new-value
                                                                         :number (js/parseFloat @new-value))])
                                             (reset! editing false))
                                   "Escape" (reset! editing false)
                                   nil))
                  :on-blur (fn [_] (reset! editing false))}]
         [:span
          {:class (styles/project-name)
           :on-click #(do (reset! new-value (project-key current-project))
                          (reset! editing true))}
          (project-key current-project)])])))

(defn project-name-display []
  (project-editable-field {:field-name "Project Name"
                          :field-type :string
                          :project-key :name
                          :event ::events/change-project-name}))

(defn project-bpm-display []
  (project-editable-field {:field-name "BPM"
                          :field-type :number
                          :project-key :bpm
                          :event ::events/change-project-bpm}))

(defn project-manager []
  (let [current-project (re-frame/subscribe [::subs/current-project])
        loaded-projects (re-frame/subscribe [::subs/loaded-projects])
        selected-projects (re-frame/subscribe [::subs/selected-projects])]
    [re-com/v-box
     :class (general-styles/configurator-container)
     :children [[re-com/title :label "Manage Projects" :level :level2]
                [:p]
                [project-name-display @current-project]
                [project-bpm-display @current-project]
                [re-com/h-box :gap "15px" :children [[re-com/selection-list
                                                      :choices @loaded-projects
                                                      :model @selected-projects
                                                      :class (styles/project-list)
                                                      :on-change #(re-frame/dispatch [::events/set-selected-projects %])
                                                      :id-fn :id
                                                      :label-fn (fn [{:keys [id name]}]
                                                                  (if (= (:id @current-project) id)
                                                                    [:p {:class (styles/project-name)} name]
                                                                    [:p name]))
                                                      :multi-select? true]
                                                     [re-com/v-box
                                                      :gap "10px"
                                                      :children [[refresh-projects]
                                                                 [load-project
                                                                  (= (count @selected-projects) 1)
                                                                  (first @selected-projects)]
                                                                 [delete-projects
                                                                  (> (count @selected-projects) 0)
                                                                  @selected-projects]]]]]]]))
(defn save-project-as []
  (let [project-name (re-frame/subscribe [::subs/project-name])
        new-name (reagent/atom @project-name)] ; local state
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
                     :model @new-name
                     :change-on-blur? false
                     :on-change #(reset! new-name %)]
                    [re-com/button
                     :label "Save As"
                     :disabled? (empty? @new-name)
                     :on-click #(do
                                  (re-frame/dispatch [::events/save-project-as @new-name])
                                  (reset! new-name ""))]]]]])))
