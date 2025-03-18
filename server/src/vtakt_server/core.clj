(ns vtakt-server.api
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [response created not-found]]
            [ring.adapter.jetty9 :as j]
            [datomic.api :as d]
            [vtakt-server.operations :as ops]
            [vtakt-server.schema :as s]
            [clojure.walk :as walk]))


;; ----- Helper Functions -----
(defn db
  "Get the current database value from the connection"
  [conn]
  (d/db conn))

(defn keywordize-ids
  "Convert string UUIDs to keywords to match Datomic's format"
  [data]
  (walk/postwalk
   (fn [x]
     (if (and (string? x) (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" x))
       (java.util.UUID/fromString x)
       x))
   data))

(defn prepare-response
  "Convert Datomic entities to a format suitable for JSON responses"
  [data]
  (walk/postwalk
   (fn [x]
     (cond
       (instance? java.util.Date x) (.toString x)
       (instance? java.util.UUID x) (.toString x)
       (= (type x) datomic.db.DbId) (str x)
       :else x))
   data))


;; ----- Routes -----

(defn create-routes
  "Create routes with a database connection"
  [conn]
  (defroutes app-routes
    ;; Project routes
    (GET "/api/projects" []
      (response (prepare-response (ops/list-projects (db conn)))))

    (GET "/api/projects/:id" [id]
      (let [project-id (java.util.UUID/fromString id)
            project (ops/get-project (db conn) project-id)]
        (if project
          (response (prepare-response project))
          (not-found {:error "Project not found"}))))

    (POST "/api/projects" {:keys [body]}
      (let [project-id (ops/create-project conn body)]
        (created (str "/api/projects/" project-id)
                 {:id (str project-id)})))

    (PUT "/api/projects/:id" [id :as {:keys [body]}]
      (let [project-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-project conn project-id body-with-ids)
        (response {:id id})))

    (DELETE "/api/projects/:id" [id]
      (let [project-id (java.util.UUID/fromString id)]
        (ops/delete-project conn project-id)
        (response {:deleted true})))

    ;; Pattern routes
    (POST "/api/projects/:project-id/patterns" [project-id :as {:keys [body]}]
      (let [proj-id (java.util.UUID/fromString project-id)
            pattern-id (ops/add-pattern conn proj-id body)]
        (created (str "/api/patterns/" pattern-id)
                 {:id (str pattern-id)})))

    (PUT "/api/patterns/:id" [id :as {:keys [body]}]
      (let [pattern-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-pattern conn pattern-id body-with-ids)
        (response {:id id})))

    (DELETE "/api/projects/:project-id/patterns/:pattern-id" [project-id pattern-id]
      (let [proj-id (java.util.UUID/fromString project-id)
            pat-id (java.util.UUID/fromString pattern-id)]
        (ops/delete-pattern conn proj-id pat-id)
        (response {:deleted true})))

    ;; Sound routes
    (POST "/api/projects/:project-id/sounds" [project-id :as {:keys [body]}]
      (let [proj-id (java.util.UUID/fromString project-id)
            body-with-ids (keywordize-ids body)
            sound-id (ops/add-sound conn proj-id body-with-ids)]
        (created (str "/api/sounds/" sound-id)
                 {:id (str sound-id)})))

    (PUT "/api/sounds/:id" [id :as {:keys [body]}]
      (let [sound-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-sound conn sound-id body-with-ids)
        (response {:id id})))

    (DELETE "/api/projects/:project-id/sounds/:sound-id" [project-id sound-id]
      (let [proj-id (java.util.UUID/fromString project-id)
            snd-id (java.util.UUID/fromString sound-id)]
        (ops/delete-sound conn proj-id snd-id)
        (response {:deleted true})))

    ;; Track routes
    (POST "/api/patterns/:pattern-id/tracks" [pattern-id :as {:keys [body]}]
      (let [pat-id (java.util.UUID/fromString pattern-id)
            body-with-ids (keywordize-ids body)
            track-id (ops/add-track conn pat-id body-with-ids)]
        (created (str "/api/tracks/" track-id)
                 {:id (str track-id)})))

    ;; Step routes
    (POST "/api/tracks/:track-id/steps" [track-id :as {:keys [body]}]
      (let [trk-id (java.util.UUID/fromString track-id)
            body-with-ids (keywordize-ids body)
            step-id (ops/add-step conn trk-id body-with-ids)]
        (created (str "/api/steps/" step-id)
                 {:id (str step-id)})))

    (PUT "/api/steps/:id" [id :as {:keys [body]}]
      (let [step-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-step conn step-id body-with-ids)
        (response {:id id})))

    ;; Parameter lock routes
    (POST "/api/steps/:step-id/parameter-locks" [step-id :as {:keys [body]}]
      (let [stp-id (java.util.UUID/fromString step-id)
            body-with-ids (keywordize-ids body)
            plock-id (ops/add-parameter-lock conn stp-id body-with-ids)]
        (created (str "/api/parameter-locks/" plock-id)
                 {:id (str plock-id)})))

    ;; Import/Export routes
    (GET "/api/projects/:id/export" [id]
      (let [project-id (java.util.UUID/fromString id)
            project-data (ops/export-project (db conn) project-id)]
        (response (prepare-response project-data))))

    (POST "/api/projects/import" {:keys [body]}
      (let [body-with-ids (keywordize-ids body)
            project-id (ops/import-project conn body-with-ids)]
        (created (str "/api/projects/" project-id)
                 {:id (str project-id)})))

    ;; Static resources and 404
    (route/resources "/")
    (route/not-found {:error "Not Found"}))

  ;; Apply middleware
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))

;; ----- Server -----

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-d" "--database URI" "Datomic database URI"
    :default ""]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["VTakt Project Server"
        ""
        "Usage: lein run [options]"
        ""
        "Options:"
        options-summary]
       (clojure.string/join \newline)))

(def s (let [db-uri "datomic:mem://vtakt"
     conn (if (d/create-database db-uri)
            (let [new-conn (d/connect db-uri)]
              (schema/install-schema new-conn)
              new-conn)
            (d/connect db-uri))
     server (api/start-server conn {:port (:port options)})]
  server
      ))


