# Nym

Name database and random name generator.

## Running

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

To start a web server for the application, run:

    lein ring server-headless

## Endpoints

  * GET `/names`
    Returns all names, with their tags.
    Names can be filtered by tags; TODO: specification for tag filters.
  * PUT `/names/{name}`
    Creates a name, if it does not already exist.
  * DELETE `/names/{name}`
    Deletes a name.
  * PUT `/names/{name}/{tag}`
    Adds a tag to a name, creating both if necessary.
  * DELETE `/names/{name}/{tag}`
    Removes a tag from a name.
  * GET `/tags`
    Returns all tags (without names).
