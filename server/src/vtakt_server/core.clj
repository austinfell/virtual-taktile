(ns vtakt-server.core
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [response created not-found]]
            [ring.adapter.jetty9 :as j]
            [ring.middleware.reload :refer [wrap-reload]]
            [vtakt-server.db.schema :as s]
            [vtakt-server.db.operations :as ops]
            [datomic.api :as d]
            [clojure.walk :as walk]))


;; ----- Helper Functions -----

(defonce server-state (atom nil))
(defonce db-conn (atom nil))

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
  (println data)
  (walk/postwalk
   (fn [x]
     (cond
       (instance? java.util.Date x)
       (.format (java.time.format.DateTimeFormatter/ISO_INSTANT)
                (.toInstant x))

       (instance? java.util.UUID x) (.toString x)
       (= (type x) datomic.db.DbId) (str x)
       (map? x) (reduce-kv
                 (fn [m k v]
                   (assoc m (name k) v))
                 {}
                 x)
       :else x))
   data))

;; ----- Routes -----

(defn create-routes
  "Create routes with a database connection"
  []
  (defroutes app-routes
    ;; Project routes
    (GET "/api/projects" []
      (response
       (prepare-response
        ;; Not always a guarantee datomic will give us patterns. This normalizes it for the
        ;; client.
        (->> (ops/list-projects (d/db @db-conn))))))

    (GET "/api/projects/:id" [id]
      (let [project-id (java.util.UUID/fromString id)
            project (ops/get-project (d/db @db-conn) project-id)]
        (if project
          (response (prepare-response project))
          (not-found {:error "Project not found"}))))

    (POST "/api/projects/:id/clone" [id :as {:keys [body]}]
      (let [project-id (java.util.UUID/fromString id)
            new-name (:name (keywordize-ids body))
            _ (println new-name)
            new-project (ops/clone-project @db-conn project-id new-name)]
        (if new-project
          (response (prepare-response {:id new-project}))
          (not-found {:error "Project not found"}))))

    (POST "/api/projects" {:keys [body]}
      (let [project-id (ops/create-project @db-conn body)]
        (created (str "/api/projects/" project-id)
                 {:id (str project-id)})))

    (PUT "/api/projects/:id" [id :as {:keys [body]}]
      (let [project-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-project @db-conn project-id body-with-ids)
        (response {:id id})))

    (DELETE "/api/projects/:id" [id]
      (let [project-id (java.util.UUID/fromString id)]
        (ops/delete-project @db-conn project-id)
        (response {:deleted true})))

    ;; Pattern routes
    (POST "/api/projects/:id/patterns" [id :as {:keys [body]}]
      (let [proj-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)
            pattern-vec (ops/add-pattern @db-conn proj-id body-with-ids)
            computed-id (clojure.string/join "-" (mapv str pattern-vec))]
        (created (str "/api/projects/" proj-id "/patterns/" computed-id)
                 {:id computed-id})))

    (GET "/api/projects/:project-id/patterns/:id" [id project-id]
      (let [project-uuid (java.util.UUID/fromString project-id)
            [bank-number pattern-number] (mapv Long/parseLong (clojure.string/split id #"-"))]
        (response (prepare-response (ops/get-pattern (d/db @db-conn) project-uuid bank-number pattern-number)))))

    (PUT "/api/projects/:project-id/patterns/:id" [id project-id :as {:keys [body]}]
      (let [project-uuid (java.util.UUID/fromString project-id)
            [bank-number pattern-number] (mapv Long/parseLong (clojure.string/split id #"-"))]
        (ops/update-pattern @db-conn project-uuid bank-number pattern-number body)
        (response {:id id})))

    (DELETE "/api/projects/:project-id/patterns/:pattern-id" [pattern-id project-id]
      (let [project-uuid (java.util.UUID/fromString project-id)
            [bank-number pattern-number] (mapv Long/parseLong (clojure.string/split pattern-id #"-"))]
        (ops/delete-pattern @db-conn project-uuid bank-number pattern-number)
        (response {:deleted true})))

    ;; Track routes
    (PUT "/api/projects/:project-id/patterns/:pattern-id/tracks/:track-id" [pattern-id project-id track-id :as {:keys [body]}]
      (let [project-uuid (java.util.UUID/fromString project-id)
            [bank-number pattern-number] (mapv Long/parseLong (clojure.string/split pattern-id #"-"))
            ;; TODO - This should probably be delegated client side.
            pattern (try (ops/get-pattern (d/db @db-conn) project-uuid bank-number pattern-number)
                         (catch Exception e nil))]
        (when (nil? pattern)
          (ops/add-pattern @db-conn project-uuid {:bank bank-number :number pattern-number :length 16}))
        (let [track-id (ops/add-track @db-conn project-uuid bank-number pattern-number track-id body)]
          (created (str "/api/projects/" project-id "/patterns/" pattern-id "/tracks")
                   {:id track-id}))))

;; Sound routes
    (POST "/api/projects/:project-id/sounds" [project-id :as {:keys [body]}]
      (let [proj-id (java.util.UUID/fromString project-id)
            body-with-ids (keywordize-ids body)
            sound-id (ops/add-sound @db-conn proj-id body-with-ids)]
        (created (str "/api/sounds/" sound-id)
                 {:id (str sound-id)})))

    (PUT "/api/sounds/:id" [id :as {:keys [body]}]
      (let [sound-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-sound @db-conn sound-id body-with-ids)
        (response {:id id})))

    (DELETE "/api/projects/:project-id/sounds/:sound-id" [project-id sound-id]
      (let [proj-id (java.util.UUID/fromString project-id)
            snd-id (java.util.UUID/fromString sound-id)]
        (ops/delete-sound @db-conn proj-id snd-id)
        (response {:deleted true})))
    ;; Step routes
    (POST "/api/tracks/:track-id/steps" [track-id :as {:keys [body]}]
      (let [trk-id (java.util.UUID/fromString track-id)
            body-with-ids (keywordize-ids body)
            step-id (ops/add-step @db-conn trk-id body-with-ids)]
        (created (str "/api/steps/" step-id)
                 {:id (str step-id)})))

    (PUT "/api/steps/:id" [id :as {:keys [body]}]
      (let [step-id (java.util.UUID/fromString id)
            body-with-ids (keywordize-ids body)]
        (ops/update-step @db-conn step-id body-with-ids)
        (response {:id id})))

    ;; Parameter lock routes
    (POST "/api/steps/:step-id/parameter-locks" [step-id :as {:keys [body]}]
      (let [stp-id (java.util.UUID/fromString step-id)
            body-with-ids (keywordize-ids body)
            plock-id (ops/add-parameter-lock @db-conn stp-id body-with-ids)]
        (created (str "/api/parameter-locks/" plock-id)
                 {:id (str plock-id)})))

    ;; Import/Export routes
    (GET "/api/projects/:id/export" [id]
      (let [project-id (java.util.UUID/fromString id)
            project-data (ops/export-project (d/db @db-conn) project-id)]
        (response (prepare-response project-data))))

    (POST "/api/projects/import" {:keys [body]}
      (let [body-with-ids (keywordize-ids body)
            project-id (ops/import-project @db-conn body-with-ids)]
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
;; Create our handler with middleware
(defn wrap-cors [handler]
  (fn [request]
    (if (= (:request-method request) :options)
      {:status 200
       :headers {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE, OPTIONS"
                 "Access-Control-Allow-Headers" "Content-Type, Authorization"}}
      (let [response (handler request)]
        (-> response
            (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
            (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
            (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))))))

(def app
  (-> (create-routes)
      wrap-cors
      wrap-reload
      (wrap-json-response)
      (wrap-defaults api-defaults)))

(defn init-db! []
  (when-not @db-conn
    (d/create-database "datomic:mem://digitone")
    (let [conn (d/connect "datomic:mem://digitone")]
      (s/install-schema conn)
      (reset! db-conn conn))))

(defn start-server! [port]
  (init-db!)
  (when-let [server @server-state]
    (.stop server))
  (reset! server-state
          (j/run-jetty app
                       {:port port :join? false})))

(defn stop-server! []
  (when-let [server @server-state]
    (.stop server)
    (reset! server-state nil)))
