package main

import (
	"fmt"
	"math/rand"
	"os"
	"time"

	"github.com/alecthomas/kingpin"
	"github.com/tokenshift/env"
)

var (
	putCommand = kingpin.Command("put", "Add a name and tags to the database.")
	putName    = putCommand.Arg("name", "The name to add or update.").Required().String()
	putTags    = putCommand.Flag("tag", "Tag to add to the name.").Short('t').Strings()

	untagCommand = kingpin.Command("untag", "Remove one or more tags from a name.")
	untagName    = untagCommand.Arg("name", "The name to add or update.").Required().String()
	untagTags    = untagCommand.Flag("tag", "Tag to add to the name.").Short('t').Strings()

	rmCommand = kingpin.Command("rm", "Delete a name and all of its tags.")
	rmName    = rmCommand.Arg("name", "The name to add or update.").Required().String()

	tagsCommand = kingpin.Command("tags", "Get all of the tags associated with a name.")
	tagsName    = tagsCommand.Arg("name", "The name to add or update.").Required().String()

	randCommand = kingpin.Command("rand", "Get a random name.")
	randTags    = randCommand.Flag("tag", "Filter for names with the specified tags.").Short('t').Strings()
	randStream  = randCommand.Flag("stream", "Stream a continuous list of random names.").Short('f').Bool()
)

func main() {
	dbFile, _ := env.GetDefault("DB_FILE", "names.db")

	db, err := NewDB(dbFile)
	if err != nil {
		fmt.Fprintln(os.Stderr, "Error:", err)
		os.Exit(1)
	}
	defer db.Close()

	rand.Seed(time.Now().UnixNano())

	switch kingpin.Parse() {
	case "put":
		db.AddName(*putName)
		for _, tag := range *putTags {
			db.AddTag(*putName, tag)
		}
	case "untag":
		for _, tag := range *untagTags {
			db.RemoveTag(*untagName, tag)
		}
	case "rm":
		db.RemoveName(*rmName)
	case "tags":
		for _, tag := range db.GetTags(*tagsName) {
			fmt.Println(tag)
		}
	case "rand":
		filters := ParseFilters(*randTags)

		if *randStream {
			for {
				outputNames(db, filters)
			}
		} else {
			outputNames(db, filters)
		}
	}
}

func outputNames(db DB, filters []Filter) {
	for i, filter := range filters {
		if i > 0 {
			fmt.Print(" ")
		}

		fmt.Print(db.RandomName(filter))
	}

	fmt.Println()
}
