# Nym

Name/word database and random name generator.

## Running

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

To start a web server for the application, run:

    lein ring server-headless

## Endpoints

  * GET `/`
    Returns random words.
    Parameters:
      * `limit` - Limits the number of results to return. Defaults to 10.
      * `query`, `tags` - Filter what words to return. See Filters, below.
  * GET `/words`
    Returns all words, with their tags.
    Parameters:
      * `offset`, `limit` - Control paging. Offset is 0-based.
      * `query`, `tags` - Filter what words to return. See Filters, below.
  * PUT `/words/{word}`
    Creates a word, if it does not already exist.
  * DELETE `/words/{word}`
    Deletes a word.
  * PUT `/words/{word}/{tag}`
    Adds a tag to a word, creating both if necessary.
  * DELETE `/words/{word}/{tag}`
    Removes a tag from a name.
  * GET `/tags`
    Returns all tags (without associated words).
  * GET `/random`
    Returns a continuous stream of random words.
    Parameters:
      * `query`, `tags` - Filter what words to return. See Filters, below.

## Filters

### Query

A glob-style string to match words by. For example, `word*` would match 'word',
'words', or 'wordy', but not 'unword'.

Multiple asterisks can be included; each will match zero-or-more characters.

Without any asterisks, the filter behaves as though there was one asterisk on
the end; e.g. `word` and `word*` are equivalent.

### Tags

Takes an S-expression of `and`, `or`, and `not` predicates on exact tags. As an
example:

  (or (and (not Tag 1, Tag 2) Tag 3, Tag 4) (and Tag 5, Tag 6 (not Tag 7, Tag 8, Tag 9)))

or

  (and Surname (or Greek, Roman) (not Etruscan))

Since tags can include whitespace, commas are required as terminators.