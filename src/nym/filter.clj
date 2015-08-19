(ns nym.filter
  (:require [clojure.string :as string]))

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

(defn make-tags-filter
  "Returns a predicate that will match a set of tags against the specified filter.

  Filter format:
  `Tag 1` - matches if the tag 'Tag 1' is found.
  `Tag 1+Tag 2` - matches if 'Tag 1' AND 'Tag 2' were found.
  `Tag 1|Tag 2` - matches if 'Tag 1' OR 'Tag 2' were found.
  `Tag 1-Tag 2` - unary '-'; matches if 'Tag 1' and NOT 'Tag 2' were found.
  `Tag 1+Tag2|(Tag 3-Tag 4) - matches if 'Tag 1' and 'Tag 2', OR 'Tag 3' and not 'Tag 4' were found.
  Anything that is not a '+', '-', '|', '(', or ')' is assumed to be part of a tag."
  [query]
  (fn [tags]
    false))
