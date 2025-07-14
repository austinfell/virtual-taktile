(ns vtakt-client.project.pattern.events
  (:require
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [vtakt-client.project.core :as p]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::set-active-pattern
 (fn [{:keys [db]} [_ {:keys [bank number] :as pattern-id}]]
   (let [project-id (get-in db [:current-project :id])]
     {:db (assoc db :active-pattern (p/create-pattern-id bank number))
      :http-xhrio {:method :get
                   :uri (str "http://localhost:8002/api/projects/"
                             project-id
                             "/patterns/"
                             (p/pattern-id->string pattern-id))
                   :timeout 8000
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::load-pattern-success pattern-id]
                   :on-failure [::load-pattern-failure pattern-id]}})))

(re-frame/reg-event-db
 ::load-pattern-success
 (fn [{:keys [current-project] :as db} [_ {:keys [bank number]} response]]
   (assoc db :current-project (p/upsert-project current-project
                                              {:bank bank
                                               :pattern number}
                                              response))))

;; TODO This needs special http status code handling - big different if we get a 404 vs a 400.
(re-frame/reg-event-fx
 ::load-pattern-failure
 (fn [{:keys [db]} [_ pattern-id]]
   ;; Pattern doesn't exist, create it
   (let [project-id (get-in db [:current-project :id])
         current-project (:current-project db)]
     {:http-xhrio {:method :post
                   :uri (str "http://localhost:8002/api/projects/" project-id "/patterns")
                   :params (p/create-default-pattern (:id current-project) pattern-id)
                   :timeout 8000
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::pattern-create-success pattern-id]
                   :on-failure [::pattern-create-failure]}})))

(re-frame/reg-event-db
 ::pattern-create-success
 (fn [db [_ {:keys [bank number] :as pattern-id}]]
   (assoc db :current-project
          (p/upsert-project
           (:current-project db)
           {:bank bank
            :pattern number}
           (p/create-default-pattern (get-in db [:current-project :id]) pattern-id)))))

(re-frame/reg-event-db
 ::pattern-create-failure
 (fn [db [_ error]]
   (assoc-in db [:ui :error-message] "Failed to create pattern")))
