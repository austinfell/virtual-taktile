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
   (get-in db [:active-pattern])))

(re-frame/reg-sub
 ::loaded-patterns
 (fn [db [_ bank]]
   (->> (get-in db [:current-project :patterns])
        (filter #(= bank (:bank (key %))))
        (map (fn [[k v]] [(:number k) v]))
        (into {}))))
