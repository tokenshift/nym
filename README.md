# Nym

Name/word database and random name generator.

## Running

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

To start a web server for the application, run:

    lein ring server-headless

## Endpoints

  * GET `/`
    Returns a random word.
    Parameters:
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

## Filters

TODO