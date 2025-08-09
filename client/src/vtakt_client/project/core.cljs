(ns vtakt-client.project.core
  (:require [clojure.spec.alpha :as s]))

;; ID Specs
;; We have a ton of different ways that IDs are represented here. Let's generically
;; define specs for them.
(def uuid-regex #"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
(s/def ::guid (s/and string? #(re-matches uuid-regex %)))

;; Patterns are essentially multi-part sequences of notes. They compose
;; of multiple tracks of note data.
;; 8 * 16 = 128 unique patterns
(def max-bank 8)
(def max-number 16)

;; The unique identifier for a pattern: patterns are identified
;; by a bank and a number in that bank.
(s/def ::bank (s/and number? #(and (> % 0) (<= % max-bank))))
(s/def ::number (s/and number? #(and (> % 0) (<= % max-number))))
(s/def ::pattern-id (s/keys :req-un [::bank ::number]))
(s/fdef create-pattern-id
  :args (s/cat :bank ::bank :number ::number)
  :ret ::pattern-id)
(defn create-pattern-id
  "Creates a pattern-id from bank and number values."
  [bank number]
  {:pre [(s/valid? ::bank bank) (s/valid? ::number number)]}
  {:bank bank
   :number number})

(s/def ::pattern-id-string (s/and string? #(re-matches #"\d+-\d+" %)))
(s/fdef pattern-id->string
  :args (s/cat :pattern-id ::pattern-id)
  :ret ::pattern-id-string)
(defn pattern-id->string
  "Converts a pattern-id to its string representation (e.g. '2-4')."
  [{:keys [bank number]}]
  {:pre [(s/valid? ::bank bank) (s/valid? ::number number)]}
  (str bank "-" number))

(s/fdef string->pattern-id
  :args ::pattern-id-string
  :ret ::pattern-id)
(defn string->pattern-id
  "Parses a pattern-id string (e.g. '2-4') back into a pattern-id."
  [s]
  {:pre [(s/valid? ::pattern-id-string s)]}
  (let [[bank-str number-str] (clojure.string/split s #"-")]
    (create-pattern-id (js/parseInt bank-str) (js/parseInt number-str))))

;; Track data structure. This is *the* polyphonic sequence that can
;; be played on a sound generation device. It says where to emit
;; notes, what those notes are, etc.
(def max-midi-channel 15)
(s/def ::midi-channel (s/and number? #(and (>= % 0) (< % max-midi-channel))))
(s/def ::track (s/keys :req-un [::midi-channel]))
(s/fdef create-track
  :args (s/keys :req-un [::midi-channel])
  :ret ::track)
(defn create-track
  "Creates a track given a output midi channel."
  [{:keys [midi-channel]}]
  {:pre [(s/valid? ::midi-channel midi-channel)]}
  {:midi-channel midi-channel})

(s/def ::track-number (s/int-in 1 5))
(s/def ::tracks (s/map-of ::track-number ::track))
(s/def ::length int?)
(s/def ::pattern (s/keys :req-un [::tracks ::length ::bank ::number]))
(s/def ::patterns (s/map-of ::pattern-id ::pattern))

;; Base level project. This contains basic root metadata as
;; well as more specific data about underlying patterns a
;; project has.
(s/def ::id ::guid)
(s/def ::author ::guid)
(s/def ::name string?)
(s/def ::global-bpm number?)
(s/def ::project (s/keys :opt-un [::id ::author] :req-un [::name ::global-bpm ::patterns]))

;; Utilities that make working with projects easier.
;;
;; These are things like allowing easy traversal of the project structure
;; to modify sub-structures of our project tree. Thing: change bank 1,
;; pattern 1, track 1 to have this new attribute.
;; Specs for the query function
;;
;; TODO - Need formal specs here.
(defn project-valid? [p] (s/valid? ::project p))

;; Minimal project definition we can start with.
(def default-project-bpm 120)
(def initial-project {:global-bpm default-project-bpm
                      :author "Undefined"
                      :name "Untitled"
                      :patterns {}})

;; TODO This will change as we expand functionality - right now doesn't actually
;; use underlying project structure.
(defn create-default-pattern
  "Given current global state of project, builds a default pattern."
  [project {:keys [bank number]}]
  {:length 16
   :bank bank
   :number number
   :tracks {}})

(defn query-project
  "Allows querying of a project"
  [{:keys [patterns]} {:keys [bank pattern track track-keys]}]
  (cond
    ;; Just bank - return all patterns in that bank
    (nil? pattern)
    (->> patterns
         (filter (fn [[pattern-id _]] (= (:bank pattern-id) bank)))
         (into {}))

    ;; Bank + pattern - return the pattern
    (nil? track)
    (get patterns (create-pattern-id bank pattern))

    ;; Bank + pattern + track - return the track
    (nil? track-keys)
    (get-in patterns [(create-pattern-id bank pattern) :tracks track])

    ;; Bank + pattern + track + keys - return specific track data
    :else
    (get-in patterns [(create-pattern-id bank pattern) :tracks track] track-keys)))

(defn upsert-project
  "Updates project data at the specified path"
  [project {:keys [bank pattern track track-key]} value]
  (let [pattern-id (create-pattern-id bank pattern)]
    (cond
      ;; Replace entire pattern
      (nil? track)
      (assoc-in project [:patterns pattern-id] value)

      ;; Replace entire track
      (nil? track-key)
      (assoc-in project [:patterns pattern-id :tracks track] value)

      ;; Replace specific track property
      :else
      (assoc-in project [:patterns pattern-id :tracks track]
                (assoc (get-in project [:patterns pattern-id :tracks track]) track-key value)))))
