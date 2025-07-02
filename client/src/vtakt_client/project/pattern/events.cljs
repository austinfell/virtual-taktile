(ns vtakt-client.project.pattern.events
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::set-active-pattern
 (fn [db [_ bank pattern]]
   (assoc db
          :active-bank bank
          :active-pattern pattern)))
