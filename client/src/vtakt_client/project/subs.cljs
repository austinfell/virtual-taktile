(ns vtakt-client.project.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::current-project
 (fn [db _]
   (:current-project db)))

(re-frame/reg-sub
 ::loaded-projects
 (fn [db _]
   (:loaded-projects db)))

(re-frame/reg-sub
 ::selected-projects
 (fn [db _]
   (:selected-projects db)))

(re-frame/reg-sub
 ::project-name
 (fn [db _]
   (get-in db [:current-project :project-name])))
