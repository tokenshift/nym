(ns nym.db
  (:require [environ.core :refer [env]]
            [korma.core :refer :all]
            [korma.db :refer [defdb sqlite3]]))

(defdb db (sqlite3 {:db (:db-string env)}))

(declare names tags name-tags)

(defentity names
  (pk :id)
  (entity-fields :id :name)
  (many-to-many tags :name_tags {:lfk :name_id :rfk :tag_id}))

(defentity tags
  (pk :id)
  (entity-fields :tag :id)
  (many-to-many tags :name_tags {:lfk :tag_id :rfk :name_id}))

(defentity name-tags
  (table :name_tags)
  (entity-fields :name_id :tag_id))

(defn get-name
  [name]
  (first (select names (with tags) (where (= :name name)) (limit 1))))

(defn get-names
  []
  (select names (with tags) (order :name) (limit 10)))

(defn put-name!
  [name tags]
  {:pre [(string? name) (every? string? tags)]}
  nil)

(defn del-name!
  [name]
  {:pre [(string? name)]}
  nil)

(defn del-tags!
  [name tags]
  {:pre [(string? name) (every? string? tags)]}
  nil)

(defn get-tags
  []
  nil)
