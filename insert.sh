#!/bin/sh

# Reads output from crawl.rb and inserts into sqlite db.

DB_FILE=$1

echo $DB_FILE

while read name; do
  read tag

  echo $name '=>' $tag

  sqlite3 $DB_FILE "INSERT INTO names (name) SELECT \"$name\" WHERE NOT EXISTS \
    (SELECT 1 FROM names WHERE name=\"$name\");"

  sqlite3 $DB_FILE "INSERT INTO tags (tag) SELECT \"$tag\" WHERE NOT EXISTS \
    (SELECT 1 FROM tags WHERE tag=\"$tag\");"

  name_id=`sqlite3 $DB_FILE "SELECT id FROM names WHERE name=\"$name\";"`
  tag_id=`sqlite3 $DB_FILE "SELECT id FROM tags WHERE tag=\"$tag\";"`

  sqlite3 $DB_FILE "INSERT INTO name_tags (name_id, tag_id) \
    SELECT $name_id, $tag_id WHERE NOT EXISTS \
    (SELECT 1 FROM name_tags WHERE name_id=$name_id AND tag_id=$tag_id);"
done
