(ns vtakt-client.project.track.events
  (:require
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-active-track
 (fn [db [_ track-num]]
   (assoc db :active-track track-num)))

(re-frame/reg-event-fx
 ::upsert-track-into-current-pattern
 (fn [{:keys [db]} [_ track]]
   (let [{:keys [id]} (:current-project db)
         {:keys [active-bank active-pattern active-track]} db
         track-path [:current-project :patterns active-bank active-pattern active-track]
         api-url (str "http://localhost:8002/api/projects/" id "/patterns/" active-bank "-" active-pattern "/tracks/" active-track)]
     (println (get-in db [:current-project :patterns ]))
     {:db (update-in db [:current-project :patterns active-bank active-pattern :tracks]
                     assoc active-track track)
      :http-xhrio {:method          :put
                   :uri             api-url
                   :params          track
                   :timeout         8000
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::save-track-success track]
                   :on-failure      [::save-track-failure]}})))

(re-frame/reg-event-db
 ::save-track-success
 (fn [db [_ track response]]
   (println "hi")))

(re-frame/reg-event-db
 ::save-track-failure
 (fn [db [_ error]]
   (println "bye")))
