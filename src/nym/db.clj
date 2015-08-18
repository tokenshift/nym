(ns nym.db
  (:require [clojure.set :refer [difference union]]))

; Some sample data until I get sqlite hooked up.
(def db (atom {"Abasolo"       #{"Surname" "Basque"}
               "Abatangelo"    #{"Surname" "Italian"}
               "Abatantuono"   #{"Surname" "Italian"}
               "Abate"         #{"Surname" "Italian"}
               "Abategiovanni" #{"Surname" "Italian"}
               "Abatescianni"  #{"Surname" "Italian"}
               "Abbadelli"     #{"Surname" "Italian"}
               "Abbas"         #{"Male" "Arabic" "Persian"}
               "Abbascia"      #{"Surname" "Italian"}
               "Abbatangelo"   #{"Surname" "Italian"}
               "Abbatantuono"  #{"Surname" "Italian"}
               "Abbate"        #{"Surname" "Italian"}
               "Abbatelli"     #{"Surname" "Italian"}
               "Abbaticchio"   #{"Surname" "Italian"}
               "Abbe"          #{"Male" "Frisian"}
               "Abbes"         #{"Surname" "Dutch"}
               "Abbey"         #{"Female" "English" "Surname"}
               "Abbi"          #{"Female" "English"}
               "Abbiati"       #{"Surname" "Italian"}
               "Abbie"         #{"Female" "English"}
               "Abbing"        #{"Surname"}}))

(defn- to-name-object
  [name tags])

(defn get-name
  [name]
  {:pre [(string? name)]}
  (when (contains? @db name) {:name name :tags (get @db name)}))

(defn get-names
  []
  (sort-by :name (into [] (for [[k v] @db] {:name k :tags v}))))

(defn put-name!
  [name tags]
  {:pre [(string? name) (every? string? tags)]}
  (swap! db #(update-in % [name] (fn [old-tags] (union old-tags (set tags)))))
  (println @db))

(defn del-name!
  [name]
  {:pre [(string? name)]}
  (swap! db #(dissoc % name)))

(defn del-tags!
  [name tags]
  {:pre [(string? name) (every? string? tags)]}
  (swap! db #(update-in % [name] (fn [old-tags] (difference old-tags tags)))))

(defn get-tags
  []
  (sort (apply union (vals @db))))
