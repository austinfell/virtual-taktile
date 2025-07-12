(ns vtakt-client.project.pattern.events
  (:require
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [re-frame.core :as re-frame]))


(re-frame/reg-event-fx
 ::set-active-pattern
 (fn [{:keys [db]} [_ bank pattern]]
   (let [project-id (get-in db [:current-project :id])
         get-url (str "http://localhost:8002/api/projects/" project-id "/patterns/" bank "-" pattern)]
     {:db (assoc db 
                :active-bank bank
                :active-pattern pattern)
      :http-xhrio {:method :get
                   :uri get-url
                   :timeout 8000
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::pattern-fetch-success bank pattern]
                   :on-failure [::pattern-fetch-failure bank pattern]}})))

(re-frame/reg-event-db
 ::pattern-fetch-success
 (fn [db [_ bank pattern response]]
   (-> db
       (assoc-in [:current-project :patterns bank pattern] response)
       (assoc-in [:ui :save-status] :loaded))))

(re-frame/reg-event-fx
 ::pattern-fetch-failure
 (fn [{:keys [db]} [_ bank pattern error]]
   ;; Pattern doesn't exist, create it
   (let [project-id (get-in db [:current-project :id])
         create-url (str "http://localhost:8002/api/projects/" project-id "/patterns")]
     {:http-xhrio {:method :post
                   :uri create-url
                   :params {:name "Main Beat"
                            :length 16
                            :bank bank
                            :number pattern}
                   :timeout 8000
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::pattern-create-success bank pattern]
                   :on-failure [::pattern-create-failure]}})))

(re-frame/reg-event-db
 ::pattern-create-success
 (fn [db [_ bank pattern response]]
   (-> db
       (assoc-in [:current-project :patterns bank pattern] 
                 {:bank bank :number pattern :length 16 :name "Main Beat" :tracks []})
       (assoc-in [:ui :save-status] :created))))

(re-frame/reg-event-db
 ::pattern-create-failure
 (fn [db [_ error]]
   (assoc-in db [:ui :error-message] "Failed to create pattern")))
