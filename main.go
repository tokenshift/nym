package main

import (
	"fmt"
	"math/rand"
	"os"
	"time"

	"github.com/alecthomas/kong"
	_ "github.com/mattn/go-sqlite3"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var Args struct {
	Put   PutCmd   `kong:"cmd,help='Add a name and tags to the database.'"`
	Untag UntagCmd `kong:"cmd,help='Remove one or more tags from a name.'"`
	Rm    RmCmd    `kong:"cmd,help='Delete a name and all of its tags.'"`
	List  ListCmd  `kong:"cmd,name='ls',help='List all of the names in the database.'"`
	Tags  TagsCmd  `kong:"cmd,help='Get all of the tags associated with a name.'"`
	Rand  RandCmd  `kong:"cmd,help='Get a random name.'"`

	Filename string `kong:"arg name='filename',short='f',help='Database filename.',default='nym.sqlite3',type='path'"`
	Verbose  bool   `kong:"arg name='verbose',short='v',help='Turn on verbose logging.'"`
	Debug    bool   `kong:"arg name='debug',short='d',help='Turn on debug logging (including SQL queries).'"`
}

var Database *gorm.DB

func main() {
	rand.Seed(time.Now().UnixNano())

	ctx := kong.Parse(&Args, kong.UsageOnMissing())

	var err error

	var logLevel logger.LogLevel

	if Args.Debug {
		logLevel = logger.Info
	} else {
		logLevel = logger.Silent
	}

	Database, err = gorm.Open(sqlite.Open(Args.Filename), &gorm.Config{
		Logger: logger.Default.LogMode(logLevel),
	})

	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}

	Database.AutoMigrate(&Name{})
	Database.AutoMigrate(&Tag{})

	err = ctx.Run()
	ctx.FatalIfErrorf(err)
}
