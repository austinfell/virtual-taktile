(ns vtakt-client.project.events
  (:require
   [vtakt-client.project.core :as pj]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::save-project-as
 (fn [{:keys [db]} [_ project-name]]
   ;; Eventually, I want to generate this and the associated ID server side.
   {:db (assoc db :current-project (pj/->Project nil project-name "Austin Fell" "Tue May 20 09:41:07 EDT 2025"))}))

(re-frame/reg-event-fx
 ::load-projects
 (fn [{:keys [db]} _]
   {:db (assoc db :loading-projects? true)
    :http-xhrio {:method          :get
                 :uri             "http://localhost:8002/api/projects"
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-projects-success]
                 :on-failure      [::fetch-projects-failure]}}))


;; Event to handle successful response
(re-frame/reg-event-fx
 ::fetch-projects-success
 (fn [{:keys [db]} [_ response]]
   ;; Transform the response data to match your record structure
   (let [projects (mapv (fn [project]
                          (pj/->Project
                           (:id project)
                           (:name project)
                           (:author project)
                           (:created-at project)))
                        response)]
     {:db (-> db
              (assoc :loaded-projects projects)
              (dissoc :loading-projects?))})))

;; Event to handle request failure
(re-frame/reg-event-db
 ::fetch-projects-failure
 (fn [db [_ error]]
   (-> db
       (assoc :request-error (str "Failed to fetch projects: " (get-in error [:response :status-text])))
       (dissoc :loading-projects?))))

(re-frame/reg-event-fx
 ::save-project-as
 (fn [{:keys [db]} [_ project-name]]
   ;; TODO - Implement this
   {:db db}))

(re-frame/reg-event-fx
 ::load-project
 (fn [{:keys [db]} [_ project-name]]
   ;; TODO - Implement this
   {:db db}))

(re-frame/reg-event-fx
 ::delete-projects
 (fn [{:keys [db]} [_ project-ids]]
   ;; TODO - Implement this
   {:db db}))
