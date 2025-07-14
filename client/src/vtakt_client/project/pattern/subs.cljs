(ns vtakt-client.project.pattern.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-bank
 (fn [db _]
   (get-in db [:active-pattern :bank])))

(re-frame/reg-sub
 ::active-pattern
 (fn [db _]
   (get-in db [:active-pattern :number])))
