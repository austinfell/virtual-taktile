(ns vtakt-client.project.track.events
  (:require
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [re-frame.core :as re-frame]
   [vtakt-client.project.core :as p]))

(re-frame/reg-event-db
 ::set-active-track
 (fn [db [_ track-num]]
   (assoc db :active-track track-num)))

(re-frame/reg-event-fx
 ::set-midi-channel-on-current-track
 (fn [{:keys [db]} [_ midi-channel]]
   (let [current-project (:current-project db)
         id (:id current-project)
         {:keys [bank number]} (:active-pattern db)
         active-track (:active-track db)
         updated-project (p/upsert-project current-project
                                           {:bank bank
                                            :pattern number
                                            :track active-track
                                            :track-key :midi-channel}
                                           midi-channel)]
     {:db (assoc db :current-project updated-project)
      :http-xhrio {:method :put
                   :uri (str "http://localhost:8002/api/projects/" id "/patterns/" bank "-" number "/tracks/" active-track)
                   :params (p/query-project updated-project {:bank bank :pattern number :track active-track})
                   :timeout 8000
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format)
                   :on-success [::save-track-success-optimistic updated-project]
                   :on-failure [::save-track-failure]}})))

(re-frame/reg-event-db
 ::save-track-success-optimistic
 (fn [db [_ track]]
   (println "Success.")))

(re-frame/reg-event-db
 ::save-track-failure
 (fn [db [_ {:keys [status] :as response}]]
   (if (and (>= status 400) (< status 500))
     (println "4xx")
     (println status))))
