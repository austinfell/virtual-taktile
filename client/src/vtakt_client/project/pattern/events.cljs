(ns vtakt-client.project.pattern.events
  (:require
   [vtakt-client.project.core :as pj]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-active-pattern
 (fn [db [_ bank pattern]]
   (assoc db :active-pattern [bank pattern])))
