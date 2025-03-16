(ns vtakt-client.utils
  (:require [clojure.string :as str]))


(defn format-keyword
  "Formats a keyword into a readable string:
   - Removes the colon prefix
   - Replaces hyphens with spaces
   - Capitalizes the first letter of each word"
  [kw]
  (when kw
    (->> (clojure.string/split (name kw) #"-")
         (map clojure.string/capitalize)
         (clojure.string/join " "))))
