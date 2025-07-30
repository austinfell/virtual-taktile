(ns vtakt-client.project.pattern.views
  (:require
   [vtakt-client.step-input.views :as sv]
   [vtakt-client.project.pattern.subs :as subs]
   [vtakt-client.project.pattern.events :as events]
   [vtakt-client.project.core :as p]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]))

(defn should-show-pattern-as-allocated
  [p]
  (and
   (not (nil? p))
   (seq (:tracks p))))

(defn bank-select [on-bank-select]
  (let [active-bank (re-frame/subscribe [::subs/active-bank])]
    (fn []
      (let [bank @active-bank
            steps (range 9 17)
            active-step (+ 8 bank)
            step-states (reduce (fn [acc step]
                                  (assoc acc step {:active (= step active-step)}))
                                {}
                                steps)]
        [sv/step-input
         {:on-step-release-handler on-bank-select}
         step-states
         (fn [step-data]
           (cond
             (true? (:active step-data)) :white
             (contains? step-data :active) :red
             :else :off))]))))

(defn pattern-select [bank on-selection]
  (let [active-pattern (re-frame/subscribe [::subs/active-pattern])
        patterns (re-frame/subscribe [::subs/loaded-patterns bank])]
    (fn []
      (let [pattern @active-pattern]
        [sv/step-input
         {:on-step-release-handler
          (fn [_ num]
            (re-frame/dispatch-sync [::events/set-active-pattern (p/create-pattern-id bank num)])
            (on-selection))}
         (if (= (:bank pattern) bank)
           (update @patterns (:number pattern) #(assoc % :active true))
           @patterns)
         #(cond
            (contains? % :active) :green
            (should-show-pattern-as-allocated %) :white
            :else :off)]))))


(defn pattern-change-workflow [on-selection]
  (let [selected-bank (reagent/atom nil)]
    (fn []
      (if (nil? @selected-bank)
        [bank-select (fn [_ num] (reset! selected-bank (- num 8)))]
        [pattern-select @selected-bank on-selection]))))
