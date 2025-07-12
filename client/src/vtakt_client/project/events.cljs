(ns vtakt-client.project.events
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-selected-projects
 (fn [db [_ project-ids]]
   (assoc db :selected-projects project-ids)))

(re-frame/reg-event-fx
 ::change-project-bpm
 (fn [{:keys [db]} [_ new-project-bpm]]
   (let [current-project (:current-project db)
         project-id (:id current-project)
         modified-project (assoc current-project :global-bpm new-project-bpm)]
     (if (nil? project-id)
       ;; New project - nothing exists server side for us to sync to... Define
       ;; the project name and call it a day.
       {:db (assoc db current-project modified-project)}

       ;; Existing project - update via PUT. Only update client side once everything
       ;; is done.
       {:http-xhrio {:method          :put
                     :uri             (str "http://localhost:8002/api/projects/" project-id)
                     :params          modified-project
                     :timeout         8000
                     :format          (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success      [::save-project-success-optimistic modified-project]
                     :on-failure      [::save-project-failure]}}))))
(re-frame/reg-event-fx
 ::change-project-name
 (fn [{:keys [db]} [_ new-project-name]]
   (let [current-project (:current-project db)
         project-id (:id current-project)
         modified-project (assoc current-project :name new-project-name)
         base-request {:params          modified-project
                       :timeout         8000
                       :format          (ajax/json-request-format)
                       :response-format (ajax/json-response-format {:keywords? true})
                       :on-success      [::save-project-success-optimistic modified-project]
                       :on-failure      [::save-project-failure]}]
     {:http-xhrio (if (nil? project-id)
                    (merge base-request
                           {:method :post
                            :uri    "http://localhost:8002/api/projects"})
                    (merge base-request
                           {:method :put
                            :uri    (str "http://localhost:8002/api/projects/" project-id)}))})))

(re-frame/reg-event-fx
 ::save-project-as
 (fn [{:keys [db]} [_ project-name]]
   (let [project-to-save (assoc (:current-project db) :name project-name)]
     {:db (assoc db :saving-project? true)
      :http-xhrio {:method          :post
                   :uri             "http://localhost:8002/api/projects"
                   :params          project-to-save
                   :timeout         8000
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::save-project-success project-to-save]
                   :on-failure      [::save-project-failure]}})))

;; Doesn't use the response! This is really only used if we are 100% sure we can synchronize
;; state without doing an additional GET. Beware!!
(re-frame/reg-event-fx
 ::save-project-success-optimistic
 (fn [{:keys [db]} [_ saved-project]]
   {:db (assoc db :current-project saved-project
                  :loaded-projects (mapv #(if (= (:id %) (:id saved-project))
                                            saved-project
                                            %)
                                         (:loaded-projects db)))}))

(re-frame/reg-event-fx
 ::save-project-success
 (fn [{:keys [db]} [_ saved-project response]]
   {:db (assoc db :current-project (assoc saved-project :id (:id response)))
    :dispatch [::load-projects]}))

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
   (let [projects response]
     {:db (assoc db :loaded-projects projects)})))

(re-frame/reg-event-db
 ::fetch-projects-failure
 (fn [db [_ error]]
   (assoc db :request-error "Failure in saving project")))

;; TODO Figure out how to handle API failures.
(re-frame/reg-event-db
 ::save-project-failure
 (fn [db [_ error]]
   (assoc db :request-error "Failure in saving project")))

(re-frame/reg-event-fx
 ::load-project
 (fn [{:keys [db]} [_ project-id]]
   {:db (assoc db :loading-project? true)
    :http-xhrio {:method          :get
                 :uri             (str "http://localhost:8002/api/projects/" project-id)
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-project-success]
                 :on-failure      [::fetch-project-failure]}}))

(re-frame/reg-event-fx
 ::fetch-project-success
 (fn [{:keys [db]} [_ response]]
   (let [project response]
     {:db (-> db
              (assoc :current-project project)
              (assoc :selected-projects #{}))})))

(re-frame/reg-event-db
 ::fetch-project-failure
 (fn [db [_ error]]
   (assoc db :request-error (str "Failed to fetch project: " (get-in error [:response :status-text])))))

(re-frame/reg-event-fx
 ::delete-projects
 (fn [{:keys [db]} [_ project-ids]]
   (let [current-pending (get db :pending-deletes 0)
         new-pending (+ current-pending (count project-ids))]
     {:db (-> db
              (assoc :deleting-projects? true)
              (assoc :selected-projects #{})
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
     (let [remaining (dec (:pending-deletes db))
           active-project-deleted? (= project-id (get-in db [:current-project :id]))]
       (if (zero? remaining)
         {:db (cond-> db
                true (dissoc :pending-deletes :deleting-projects?)
                active-project-deleted? (assoc-in [:current-project :id] nil)
                active-project-deleted? (assoc-in [:current-project :name] "Untitled"))
          :dispatch [::load-projects]}
         {:db (assoc db :pending-deletes remaining)})))))
