(ns vtakt-client.project.pattern.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-bank
 (fn [db _]
   (:active-bank db)))

(re-frame/reg-sub
 ::active-pattern
 (fn [db _]
   (:active-pattern db)))
