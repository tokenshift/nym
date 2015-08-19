(ns nym.filter
  (:require [clojure.string :as string]
            [instaparse.core :as insta]))

;; Word Filters

(declare match-anywhere? match-beginning?)

(defn- match-anywhere?
  "Looks for the next match anywhere in the input string."
  [[match & ms] input]
  {:pre [(string? match) (string? input)]}
  (if (= "*" match)
    (or (empty? ms) (match-anywhere? ms input))
    (let [i (.indexOf input match)]
      (if (> i -1)
        (if (empty? ms)
          (= (.length input) (+ i (.length match)))
          (match-beginning? ms (.substring input (+ i (.length match)))))
        false))))

(defn- match-beginning?
  "Looks for the next match at the beginning of the input string."
  [[match & ms] input]
  {:pre [(string? match) (string? input)]}
  (if (= "*" match)
    (or (empty? ms) (match-anywhere? ms input))
    (if (.startsWith input match)
      (if (empty? ms)
        (= (.length input) (.length match))
        (match-beginning? ms (.substring input (.length match))))
      false)))

(defn make-word-filter
  "Returns a predicate that will match input strings against the specified filter.

  Asterisks can be used to match arbitrary text at either end or in the middle of
  the query string; a query without any asterisks will match if it is found at
  the beginning of the test word (that is, 'filter' is equivalent to 'filter*')."
  [query]
  {:pre [(string? query)]}
  (let [query (if (.contains query "*") query (str query "*"))
        matchers (->> (re-seq #"([^\*]+)|\*" query)
                      (map first))]
    ; e.g. "Wha*te**ver*" => ("Wha" "*" "te" "*" "*" "ver" "*")
    ; or "Testing" => ("Testing" "*")
    (fn [word]
    {:pre [(string? word)]}
      (match-beginning? matchers word))))

;; Tag Filters

(def parse-tag-filter
  (insta/parser
    "filters = filter+
     filter  = tag | or | and | not
     or      = <'(' 'or'>  filters <')'>
     and     = <'(' 'and'> filters <')'>
     not     = <'(' 'not'> filters <')'>
     tag     = #'[^,\\(\\)]+'"
    :auto-whitespace :comma))

; Example filters:
; "(or (and (not Tag 1) Tag 2) (and Tag 3 (not Tag 4))" =>
; [:filters
;   [:filter
;    [:or
;     [:filters
;      [:filter
;       [:and
;        [:filters
;         [:filter [:not [:filters [:filter [:tag "Tag 1"]]]]]
;         [:filter [:tag "Tag 2"]]]]]
;      [:filter
;       [:and
;        [:filters
;         [:filter [:tag "Tag 3"]]
;         [:filter
;          [:not [:filters [:filter [:tag "Tag 4"]]]]]]]]]]]]
;
; "Test Tag" =>
; [:filters [:filter [:tag "Test Tag"]]]
;
; "Tag 1, Tag 2" =>
; [:filters [:filter [:tag "Tag 1"]]
;           [:filter [:tag "Tag 2"]]]
;
; "(or Tag 1, Tag 2)" =>
; [:filters
;   [:filter
;    [:or
;     [:filters [:filter [:tag "Tag1"]] [:filter [:tag "Tag2"]]]]]]

(defmulti filter-matches?
  "Checks a parsed tag filter against a list of tags."
  (fn [parsed tags] (first parsed)))

(defmethod filter-matches? :default
  [parsed tags]
  (throw (UnsupportedOperationException. "Not Implemented")))

(defmethod filter-matches? :filters
  [[_ & fs] tags]
  (every? #(filter-matches? % tags) fs))

(defmethod filter-matches? :filter
  [[_ clause] tags]
  (filter-matches? clause tags))

(defmethod filter-matches? :tag
  [[_ tag] tags]
  (boolean (some #{(string/trim tag)} tags)))

(defmethod filter-matches? :or
  [[_ [_ & fs]] tags]
  (boolean (some #(filter-matches? % tags) fs)))

(defmethod filter-matches? :and
  [[_ fs] tags]
  (filter-matches? fs tags))

(defmethod filter-matches? :not
  [[_ [_ & fs]] tags]
  (not (some #(filter-matches? % tags) fs)))

(defn make-tags-filter
  "Returns a predicate that will match a set of tags against the specified filter."
  [query]
  (let [parsed (parse-tag-filter query)]
    (fn [tags]
      (filter-matches? parsed tags))))
