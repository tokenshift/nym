(ns nym.filter)

(defn make-word-filter
  "Returns a predicate that will match input strings against the specified filter.

  Asterisks can be used to match arbitrary text at either end or in the middle of
  the query string; a query without any asterisks will match if it is found at
  the beginning of the test word (that is, 'filter' is equivalent to 'filter*')."
  [query]
  (fn [word]
    false))

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
