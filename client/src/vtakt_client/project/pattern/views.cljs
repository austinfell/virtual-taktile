(ns vtakt-client.project.pattern.views
  (:require
   [vtakt-client.step-input.views :as sv]
   [vtakt-client.project.pattern.subs :as subs]
   [vtakt-client.project.pattern.events :as events]
   [vtakt-client.project.core :as p]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]))

(defn bank-and-pattern-select []
  (let [active-bank (re-frame/subscribe [::subs/active-bank])
        active-pattern (re-frame/subscribe [::subs/active-pattern])
        selected-bank (reagent/atom @active-bank)]
    (fn []
      (let [pattern @active-pattern
            bank @active-bank]
        [sv/step-input
         {:on-step-release-handler
          (fn [_ num]
            (re-frame/dispatch [::events/set-active-pattern (p/create-pattern-id bank num)]))}
     ;; TODO - Filling this out requires some basic pattern data to be loaded on bank selection.
     ;; That is not done right now, once we do that, we can trivially determine active pattern and
     ;; populated patterns.
         {pattern {:active true}}
         #(if (:active %) :green :off)]))))

