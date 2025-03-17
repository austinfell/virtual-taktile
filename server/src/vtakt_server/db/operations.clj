(ns vtakt-server.operations
  (:require [datomic.api :as d]
            [clojure.spec.alpha :as s]
            [vtakt-server.schema :as schema]
            [clojure.walk :as walk])
  (:import [java.util UUID Date]))


;; ----- Database Connection -----

(defn create-database
  "Create a new database and return a connection"
  [db-uri]
  (d/create-database db-uri)
  (let [conn (d/connect db-uri)]
    (schema/install-schema conn)
    conn))

;; ----- Helper Functions -----

(defn uuid
  "Generate a random UUID"
  []
  (UUID/randomUUID))

(defn now
  "Return current date-time"
  []
  (Date.))

(defn expand-entity
  "Pull all attributes of an entity by its id"
  [db entity-id]
  (d/pull db '[*] entity-id))

;; ----- Project Operations -----

(defn create-project
  "Create a new VTakt project"
  [conn {:keys [name author bpm] :or {bpm 120.0}}]
  (let [project-id (uuid)
        project-data {:db/id "new-project"
                      :project/id project-id
                      :project/name name
                      :project/author author
                      :project/bpm bpm
                      :project/created-at (now)
                      :project/updated-at (now)}
        tx-result @(d/transact conn [project-data])]
    project-id))

(defn update-project
  "Update a VTakt project"
  [conn project-id project-data]
  (let [db (d/db conn)
        existing-entity (d/pull db '[*] [:project/id project-id])
        update-data (assoc project-data
                           :db/id (:db/id existing-entity)
                           :project/updated-at (now))
        tx-result @(d/transact conn [update-data])]
    project-id))

(defn delete-project
  "Delete a VTakt project"
  [conn project-id]
  (let [db (d/db conn)
        entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        tx-result @(d/transact conn [[:db/retractEntity entity-id]])]
    true))

(defn get-project
  "Get a VTakt project by id"
  [db project-id]
  (let [project-pattern '[*
                          {:project/patterns
                           [*
                            {:pattern/tracks
                             [*
                              {:track/sound-ref [*]}
                              {:track/steps
                               [*
                                {:step/parameter-locks [*]}]}]}]}
                          {:project/sounds
                           [*
                            {:sound/operators [*]}
                            {:sound/envelope-settings [*]}]}]
        project (d/pull db project-pattern [:project/id project-id])]
    project))

(defn list-projects
  "List all VTakt projects"
  [db]
  (let [projects (d/q '[:find [(pull ?e [:project/id :project/name :project/author :project/created-at]) ...]
                         :where [?e :project/id]]
                       db)]
    projects))

;; ----- Pattern Operations -----

(defn add-pattern
  "Add a pattern to a project"
  [conn project-id {:keys [name length] :or {length 16}}]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-id (uuid)
        pattern-data {:db/id "new-pattern"
                      :pattern/id pattern-id
                      :pattern/name name
                      :pattern/length length}
        tx-data [pattern-data
                 {:db/id project-entity-id
                  :project/patterns "new-pattern"
                  :project/updated-at (now)}]
        tx-result @(d/transact conn tx-data)]
    pattern-id))

(defn update-pattern
  "Update a pattern"
  [conn pattern-id pattern-data]
  (let [db (d/db conn)
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/id pattern-id]))
        tx-data [(assoc pattern-data :db/id pattern-entity-id)]
        tx-result @(d/transact conn tx-data)]
    pattern-id))

(defn delete-pattern
  "Delete a pattern from a project"
  [conn project-id pattern-id]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/id pattern-id]))
        tx-data [[:db/retract project-entity-id :project/patterns pattern-entity-id]
                 [:db/retractEntity pattern-entity-id]
                 {:db/id project-entity-id
                  :project/updated-at (now)}]
        tx-result @(d/transact conn tx-data)]
    true))

;; ----- Sound Operations -----

(defn add-sound
  "Add a sound to a project"
  [conn project-id sound-data]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        sound-id (uuid)
        base-sound {:db/id "new-sound"
                    :sound/id sound-id}
        sound-with-data (merge base-sound sound-data)
        tx-data [sound-with-data
                 {:db/id project-entity-id
                  :project/sounds "new-sound"
                  :project/updated-at (now)}]
        tx-result @(d/transact conn tx-data)]
    sound-id))

(defn update-sound
  "Update a sound"
  [conn sound-id sound-data]
  (let [db (d/db conn)
        sound-entity-id (:db/id (d/pull db '[:db/id] [:sound/id sound-id]))
        tx-data [(assoc sound-data :db/id sound-entity-id)]
        tx-result @(d/transact conn tx-data)]
    sound-id))

(defn delete-sound
  "Delete a sound from a project"
  [conn project-id sound-id]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        sound-entity-id (:db/id (d/pull db '[:db/id] [:sound/id sound-id]))
        tx-data [[:db/retract project-entity-id :project/sounds sound-entity-id]
                 [:db/retractEntity sound-entity-id]
                 {:db/id project-entity-id
                  :project/updated-at (now)}]
        tx-result @(d/transact conn tx-data)]
    true))

;; ----- Track Operations -----

(defn add-track
  "Add a track to a pattern"
  [conn pattern-id {:keys [number sound-id]}]
  (let [db (d/db conn)
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/id pattern-id]))
        sound-entity-id (:db/id (d/pull db '[:db/id] [:sound/id sound-id]))
        track-id (uuid)
        track-data {:db/id "new-track"
                    :track/id track-id
                    :track/number number
                    :track/sound-ref sound-entity-id}
        tx-data [track-data
                 {:db/id pattern-entity-id
                  :pattern/tracks "new-track"}]
        tx-result @(d/transact conn tx-data)]
    track-id))

;; ----- Step Operations -----

(defn add-step
  "Add a step to a track"
  [conn track-id step-data]
  (let [db (d/db conn)
        track-entity-id (:db/id (d/pull db '[:db/id] [:track/id track-id]))
        step-id (uuid)
        step-with-id (assoc step-data 
                            :db/id "new-step"
                            :step/id step-id)
        tx-data [step-with-id
                 {:db/id track-entity-id
                  :track/steps "new-step"}]
        tx-result @(d/transact conn tx-data)]
    step-id))

(defn update-step
  "Update a step"
  [conn step-id step-data]
  (let [db (d/db conn)
        step-entity-id (:db/id (d/pull db '[:db/id] [:step/id step-id]))
        tx-data [(assoc step-data :db/id step-entity-id)]
        tx-result @(d/transact conn tx-data)]
    step-id))

;; ----- Parameter Lock Operations -----

(defn add-parameter-lock
  "Add a parameter lock to a step"
  [conn step-id {:keys [parameter value]}]
  (let [db (d/db conn)
        step-entity-id (:db/id (d/pull db '[:db/id] [:step/id step-id]))
        plock-id (uuid)
        plock-data {:db/id "new-plock"
                    :plock/id plock-id
                    :plock/parameter parameter
                    :plock/value value}
        tx-data [plock-data
                 {:db/id step-entity-id
                  :step/parameter-locks "new-plock"}]
        tx-result @(d/transact conn tx-data)]
    plock-id))

;; ----- Utility Functions -----

(defn export-project
  "Export a project to EDN format"
  [db project-id]
  (let [project (get-project db project-id)]
    project))

(defn import-project
  "Import a project from EDN format"
  [conn project-data]
  (let [;; Handle nested components by assigning temporary IDs
        next-temp-id (atom -1)
        assign-temp-ids (fn [form]
                          (walk/postwalk
                           (fn [x]
                             (if (and (map? x) (not (:db/id x)))
                               (assoc x :db/id (str "temp-" (swap! next-temp-id dec)))
                               x))
                           form))
        project-with-ids (assign-temp-ids project-data)
        project-id (or (:project/id project-with-ids) (uuid))
        project-with-dates (-> project-with-ids
                               (assoc :project/id project-id)
                               (assoc :project/created-at (now))
                               (assoc :project/updated-at (now)))
        tx-result @(d/transact conn [project-with-dates])]
    project-id))
