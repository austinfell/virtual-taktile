(ns vtakt-client.utils.specs
  (:require [clojure.spec.alpha :as s]))

;;; ---------------------------------------------------------------------------
;;; Reagent Specs
;;; ---------------------------------------------------------------------------

;; Reagent component - can be a function, vector, or other valid Reagent form
(s/def ::reagent-component
  (s/or :function fn?
        :vector vector?
        :string string?
        :number number?
        :nil nil?))

;; Reagent props - typically a map of properties passed to components
(s/def ::props (s/nilable map?))

;; Reagent children - components nested within a parent component
(s/def ::children (s/coll-of ::reagent-component))

;; Reagent element vector - the core of Reagent's hiccup-like syntax
(s/def ::element-vector
  (s/cat :tag (s/or :keyword keyword?
                    :function fn?
                    :component ::reagent-component)
         :props (s/? ::props)
         :children (s/* ::reagent-component)))

;; Reagent atom - the reactive state container
(s/def ::ratom any?)

;; Reagent cursor - derived view into a reagent atom
(s/def ::cursor any?)

;; Reagent reaction - computed value that tracks dependencies
(s/def ::reaction any?)

;; Reagent RCursor path - path into an atom
(s/def ::cursor-path (s/coll-of (s/or :keyword keyword?
                                      :string string?
                                      :number int?)))

;; Track function - functions that track reactive dependencies
(s/def ::track-fn fn?)

;; Component lifecycle methods
(s/def ::component-did-mount fn?)
(s/def ::component-did-update fn?)
(s/def ::component-will-unmount fn?)
(s/def ::get-initial-state fn?)
(s/def ::get-derived-state-from-props fn?)
(s/def ::should-component-update fn?)
(s/def ::render fn?)

;; Form-2 and Form-3 component return value
(s/def ::component-return-value
  (s/or :vector vector?
        :nil nil?
        :string string?
        :number number?))

;; Form-3 component definition
(s/def ::form-3-component
  (s/keys :opt-un [::component-did-mount
                   ::component-did-update
                   ::component-will-unmount
                   ::get-initial-state
                   ::get-derived-state-from-props
                   ::should-component-update
                   ::render]))

;;; ---------------------------------------------------------------------------
;;; Re-frame Specs
;;; ---------------------------------------------------------------------------

;; Event id - keyword that identifies an event
(s/def ::event-id keyword?)

;; Event vector - vector containing event id and optional parameters
(s/def ::event-vector
  (s/cat :id ::event-id
         :args (s/* any?)))

;; Event handler - function that handles an event
(s/def ::event-handler fn?)

;; Event registration - associates an event id with a handler
(s/def ::event-registration
  (s/cat :id ::event-id
         :interceptors (s/? (s/coll-of any?))
         :handler ::event-handler))

;; Subscription id - keyword that identifies a subscription
(s/def ::subscription-id keyword?)

;; Subscription vector - vector containing subscription id and optional parameters
(s/def ::subscription-vector
  (s/cat :id ::subscription-id
         :args (s/* any?)))

;; Subscription handler - function that computes a subscription value
(s/def ::subscription-handler fn?)

;; Subscription registration - associates a subscription id with a handler
(s/def ::subscription-registration
  (s/cat :id ::subscription-id
         :input-signals (s/? (s/coll-of any?))
         :computation-fn ::subscription-handler))

;; App-db - the central re-frame state atom
(s/def ::app-db map?)

;; Interceptor - middleware that processes re-frame events
(s/def ::interceptor-id keyword?)
(s/def ::before fn?)
(s/def ::after fn?)
(s/def ::interceptor
  (s/keys :req-un [::interceptor-id]
          :opt-un [::before ::after]))

;; Coeffect - side data given to an event handler
(s/def ::coeffects map?)

;; Effect - side effects produced by an event handler
(s/def ::effects map?)

;; Context - data structure passed through interceptor chain
(s/def ::context
  (s/keys :opt-un [::coeffects ::effects ::queue ::stack ::original-event]))

;; Effect handler - function that performs side effects
(s/def ::effect-id keyword?)
(s/def ::effect-handler fn?)
(s/def ::effect-registration
  (s/cat :id ::effect-id
         :handler ::effect-handler))

;; Coeffect handler - function that sources coeffects
(s/def ::coeffect-id keyword?)
(s/def ::coeffect-handler fn?)
(s/def ::coeffect-registration
  (s/cat :id ::coeffect-id
         :handler ::coeffect-handler))

;; Common re-frame functions
(s/def ::reg-event-db-args ::event-registration)
(s/def ::reg-event-fx-args ::event-registration)
(s/def ::reg-sub-args ::subscription-registration)
(s/def ::dispatch-args ::event-vector)
(s/def ::dispatch-sync-args ::event-vector)
(s/def ::subscribe-args ::subscription-vector)

;; Re-frame chain handler (common pattern in re-frame for event handlers)
(s/def ::db map?)
(s/def ::event ::event-vector)
(s/def ::event-db-handler (s/fspec :args (s/cat :db ::db :event ::event)
                                   :ret ::db))
(s/def ::event-fx-handler (s/fspec :args (s/cat :cofx ::coeffects :event ::event)
                                   :ret ::effects))
