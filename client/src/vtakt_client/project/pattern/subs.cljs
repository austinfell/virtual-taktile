(ns vtakt-client.project.pattern.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-bank
 (fn [db _]
   ;; First dimension in tuple is bank
   (first (:active-pattern db))))

(re-frame/reg-sub
 ::active-pattern-in-bank
 (fn [db _]
   ;; Second dimension in tuple is pattern
   (second (:active-pattern db))))
