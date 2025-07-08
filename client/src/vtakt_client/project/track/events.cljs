(ns vtakt-client.project.track.events
  (:require
   [day8.re-frame.http-fx]
   [vtakt-client.midi.events :as midi-events]
   [ajax.core :as ajax]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-active-track
 (fn [db [_ track]]
   (assoc db :active-track track)))

(re-frame/reg-event-fx
 ::save-current-track
 (fn [{:keys [db]} _]
   (let [project-id (get-in db [:current-project :id])
         bank (get db :active-bank)
         pattern (get db :active-pattern)
         track-num (get db :active-track)
         pattern-id (str bank "-" pattern)
         track-data (get-in db [:current-project :patterns bank pattern track-num])]
     (print project-id)
     (print (get-in db [:current-project :patterns 1]))
     (print track-data)
     (when (and project-id track-data)
       {:http-xhrio {:method :put
                     :uri (str "http://localhost:8002/api/projects/"
                               project-id "/patterns/" pattern-id "/tracks/" track-num)
                     :params (select-keys track-data [:number :midi-channel])
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [::save-track-success]
                     :on-failure [::save-track-failure]}}))))

(re-frame/reg-event-fx
 ::set-selected-midi-channel-for-track-and-save
 (fn [{:keys [db]} [_ new-channel]]
   {:dispatch-n [[::midi-events/set-selected-midi-channel-for-track new-channel]
                 [::save-current-track]]}))

(re-frame/reg-event-db
 ::save-track-success
 (fn [db [_ response]]
   (assoc db :save-status :success)))

(re-frame/reg-event-db
 ::save-track-failure
 (fn [db [_ response]]
   (assoc db :save-status :error)))
