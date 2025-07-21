(ns vtakt-server.db.operations

  (:require [datomic.api :as d]
            [vtakt-server.db.schema :as schema]
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

;; ----- Project Operations -----
(defn create-project
  "Create a new VTakt project"
  [conn {:keys [name author global-bpm patterns] :or {global-bpm 120.0 patterns []}}]
  (let [project-id (UUID/randomUUID)
        project-data {:db/id "new-project"
                      :project/id project-id
                      :project/name name
                      :project/author author
                      :project/patterns (->> patterns (map #(assoc (val %) :id (key %)))
                                             vec)
                      :project/global-bpm (double global-bpm)
                      :project/created-at (Date.)
                      :project/updated-at (Date.)}
        tx-result @(d/transact conn [project-data])]
    project-id))

(defn generate-new-ids-on-project [entity]
  (cond
    ;; Project level - new temp ID and new UUID
    (:project/id entity)
    (let [new-proj-tempid (str "proj-" (UUID/randomUUID))
          new-uuid (UUID/randomUUID)]
      (-> entity
          (assoc :db/id new-proj-tempid
                 :project/id new-uuid)
          (update :project/patterns #(mapv generate-new-ids-on-project %))))
    ;; Pattern level - new temp ID, link to parent
    (:pattern/bank entity)
    (let [new-patt-tempid (str "patt-" (UUID/randomUUID))
          parent-tempid (:db/id (:pattern/project entity))]
      (-> entity
          (assoc :db/id new-patt-tempid
                 :pattern/project parent-tempid)
          (update :pattern/project+bank+number #(assoc % 0 parent-tempid))
          (update :pattern/tracks #(mapv (fn [track]
                                           (generate-new-ids-on-project
                                            (assoc track :track/pattern new-patt-tempid))) %))))
    ;; Track level - new temp ID, link to pattern
    (:track/number entity)
    (let [new-track-tempid (str "track-" (UUID/randomUUID))
          pattern-tempid (:db/id (:track/pattern entity))]
      (-> entity
          (assoc :db/id new-track-tempid)
          (update :track/pattern+number #(assoc % 0 pattern-tempid))
          (update :track/steps #(mapv generate-new-ids-on-project %))))
    ;; Steps, plocks, etc - same pattern
    :else entity))

(defn clone-project
  "Clones a VTakt project"
  [conn project-id new-name]
  (let [db (d/db conn)
        ;; Pull entire structure.
        project-pattern '[*
                          {:project/patterns
                           [*
                            {:pattern/tracks
                             [*
                              {:track/steps
                               [*
                                {:step/parameter-locks [*]}]}]}]}
                          {:project/sounds
                           [*
                            {:sound/operators [*]}
                            {:sound/envelope-settings [*]}]}]
        original-project (d/pull db project-pattern [:project/id project-id])

        cloned-project (-> original-project
                           generate-new-ids-on-project
                           (assoc :project/name new-name))
        proj-tempid (:db/id cloned-project)  ; "proj-abc123..."

        _ (println cloned-project)
        ;; Transact and get the result
        tx-result (try @(d/transact conn [cloned-project])
                       (catch Exception e (println e)))

        ;; Resolve temp ID to real entity ID
        real-entity-id (d/resolve-tempid (:db-after tx-result)
                                         (:tempids tx-result)
                                         proj-tempid)

        ;; Get the business UUID if you want that instead
        new-db (d/db conn)
        project-uuid (:project/id (d/entity new-db real-entity-id))]

    project-uuid))

(defn update-project
  "Update a VTakt project"
  [conn project-id project-data]
  (let [db (d/db conn)
        existing-entity (d/pull db '[*] [:project/id project-id])
        global-bpm (double (:global-bpm project-data))
        project-name (:name project-data)
        transformed-data (assoc (assoc existing-entity :project/name project-name) :project/global-bpm global-bpm)
        update-data (assoc transformed-data
                           :db/id (:db/id existing-entity)
                           :project/updated-at (Date.))
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
                          {:project/patterns [:pattern/bank :pattern/number]}
                          {:project/sounds
                           [*
                            {:sound/operators [*]}
                            {:sound/envelope-settings [*]}]}]
        project (d/pull db project-pattern [:project/id project-id])]
    project))

(defn list-projects
  "List all VTakt projects"
  [db]
  (d/q '[:find [(pull ?e [:project/id
                          :project/name
                          :project/author
                          :project/created-at
                          :project/global-bpm
                          {:project/patterns [:pattern/bank :pattern/number]}]) ...]
         :where [?e :project/id]]
       db))

;; ----- Pattern Operations -----

(defn add-pattern
  "Add a pattern to a project"
  [conn project-id {:keys [length bank number] :or {length 16}}]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-tempid (d/tempid :db.part/user)
        pattern-data {:db/id pattern-tempid
                      :pattern/bank bank
                      :pattern/number number
                      :pattern/length length
                      :pattern/project project-entity-id}
        tx-data [pattern-data
                 {:db/id project-entity-id
                  :project/patterns pattern-tempid
                  :project/updated-at (Date.)}]
        _ (println tx-data)
        tx-result @(d/transact conn tx-data)]
    [bank number]))

(defn get-pattern
  [db project-id bank number]
  (let [project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-pull '[:pattern/bank
                       :pattern/number
                       :pattern/length
                       {:pattern/tracks
                        [:track/number
                         :track/midi-channel]}]
        lookup-key [:pattern/project+bank+number [project-entity-id bank number]]
        pattern (d/pull db pattern-pull lookup-key)]
    pattern))

(defn update-pattern
  "Update a pattern"
  [conn project-id bank-number pattern-number pattern-data]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/project+bank+number [project-entity-id bank-number pattern-number]]))
        ;; TODO - I really need to implement a consistent way to do this.
        namespaced-data (reduce-kv (fn [m k v]
                                     (assoc m (keyword "pattern" (name k)) v))
                                   {}
                                   pattern-data)
        tx-data (merge {:db/id pattern-entity-id} namespaced-data)]
    @(d/transact conn [tx-data])))

(defn delete-pattern
  "Delete a pattern from a project"
  [conn project-id bank-number pattern-number]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/project+bank+number [project-entity-id bank-number pattern-number]]))
        tx-data [[:db/retractEntity pattern-entity-id]
                 {:db/id project-entity-id
                  :project/updated-at (Date.)}]]
    @(d/transact conn tx-data)))

;; ----- Track Operations -----
(defn add-track
  "Add a track to a pattern"
  [conn project-id bank-number pattern-number track-number {:keys [midi-channel]}]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        pattern-entity-id (:db/id (d/pull db '[:db/id] [:pattern/project+bank+number [project-entity-id bank-number pattern-number]]))
        track-num (Integer/parseInt track-number)
        track-entity-id (:db/id (d/pull db '[:db/id] [:track/pattern+number [pattern-entity-id track-num]]))
        midi-chan (Long/valueOf midi-channel)
        track-tempid (if (nil? track-entity-id) (d/tempid :db.part/user) track-entity-id)
        tx-data [{:db/id track-tempid
                  :track/pattern pattern-entity-id
                  :track/number track-num
                  :track/midi-channel midi-chan}
                 {:db/id pattern-entity-id
                  :pattern/tracks track-tempid}
                 {:db/id project-entity-id
                  :project/updated-at (Date.)}]]
    @(d/transact conn tx-data)
    track-num))

;; ----- Sound Operations -----
(defn add-sound
  "Add a sound to a project"
  [conn project-id sound-data]
  (let [db (d/db conn)
        project-entity-id (:db/id (d/pull db '[:db/id] [:project/id project-id]))
        sound-id (UUID/randomUUID)
        base-sound {:db/id "new-sound"
                    :sound/id sound-id}
        sound-with-data (merge base-sound sound-data)
        tx-data [sound-with-data
                 {:db/id project-entity-id
                  :project/sounds "new-sound"
                  :project/updated-at (Date.)}]
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
                  :project/updated-at (Date.)}]
        tx-result @(d/transact conn tx-data)]
    true))

;; ----- Step Operations -----

(defn add-step
  "Add a step to a track"
  [conn track-id step-data]
  (let [db (d/db conn)
        track-entity-id (:db/id (d/pull db '[:db/id] [:track/id track-id]))
        step-id (UUID/randomUUID)
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
        plock-id (UUID/randomUUID)
        plock-data {:db/id "new-plock"
                    :plock/id plock-id
                    :plock/parameter parameter
                    :plock/value value}
        tx-data [plock-data
                 {:db/id step-entity-id
                  :step/parameter-locks "new-plock"}]
        tx-result @(d/transact conn tx-data)]
    plock-id))

