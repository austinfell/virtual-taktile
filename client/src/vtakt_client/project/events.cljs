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
   {:db (assoc db :saving-project? true)
    :http-xhrio {:method          :post
                 :uri             "http://localhost:8002/api/projects"
                 :params {:name project-name
                          :author "Austin Fell"
                          :bpm (double 402)
                          }

                 :timeout         8000
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::save-project-success]
                 :on-failure      [::save-project-failure]}}))


;; TODO - Still need to feed in the original parameters I used to
;; actually build the post request.
(re-frame/reg-event-fx
 ::save-project-success
 (fn [{:keys [db]} [_ response]]
     {:dispatch [::load-projects]}))

(re-frame/reg-event-db
 ::save-project-failure
 (fn [db [_ error]]
   (-> db
       (assoc :request-error "Failure in saving project"))))

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

(re-frame/reg-event-fx
 ::fetch-projects-success
 (fn [{:keys [db]} [_ response]]
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

(re-frame/reg-event-db
 ::fetch-projects-failure
 (fn [db [_ error]]
   (-> db
       (assoc :request-error (str "Failed to fetch projects: " (get-in error [:response :status-text])))
       (dissoc :loading-projects?))))

(re-frame/reg-event-fx
 ::load-project
 (fn [{:keys [db]} [_ project-name]]
   ;; TODO - Implement this
   {:db db}))

(re-frame/reg-event-fx
 ::delete-projects
 (fn [{:keys [db]} [_ project-ids]]
   (let [current-pending (get db :pending-deletes 0)
         new-pending (+ current-pending (count project-ids))]
     {:db (-> db
              (assoc :deleting-projects? true)
              (assoc :pending-deletes new-pending))
      :fx (mapv (fn [project-id]
                  [:http-xhrio {:method          :delete
                                :uri             (str "http://localhost:8002/api/projects/" project-id)
                                :timeout         8000
                                :format          (ajax/url-request-format)
                                :response-format (ajax/json-response-format)
                                :on-success      [::delete-completed project-id nil]
                                :on-failure      [::delete-completed project-id]}])
                project-ids)})))

(re-frame/reg-event-fx
 ::delete-completed
 (fn [{:keys [db]} [_ project-id response-or-error]]
   (let [is-error (or (nil? response-or-error)
                      (not (:deleted response-or-error)))]
     (when is-error
       ;; TODO - Handle error (ownership changed, etc.)
       ;; Could dispatch another event with project-id and error details
       )
     (let [remaining (dec (:pending-deletes db))]
       (if (zero? remaining)
         {:db (-> db (dissoc :pending-deletes :deleting-projects?))
          :dispatch [::load-projects]}
         {:db (assoc db :pending-deletes remaining)})))))
