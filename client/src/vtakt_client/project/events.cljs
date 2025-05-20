(ns vtakt-client.project.events
  (:require
   [vtakt-client.project.core :as pj]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::save-project-as
 (fn [{:keys [db]} [_ project-name]]
   ;; Eventually, I want to generate this and the associated ID server side.
   {:db (assoc db :current-project (pj/->Project project-name 1 false))}))

(re-frame/reg-event-fx
 ::load-projects
 (fn [{:keys [db]} _]
   ;; Eventually, I want to generate this and the associated ID server side.
   {:db (assoc db :loaded-projects [(pj/->Project "Untitled" 0 false)
                                    (pj/->Project "loaded-project" 2 false)
                                    (pj/->Project "loaded-project 2" 3 false)])}))
