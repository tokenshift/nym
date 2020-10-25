package main

import (
	"errors"
	"fmt"
	"math/rand"
	"os"
	"strings"

	"gorm.io/gorm"

	"github.com/alecthomas/kong"
)

type PutCmd struct {
	Name string   `kong:"arg,required,name='name',help='The name to add or update.'"`
	Tags []string `kong:"arg,required,name='tag',help='Tag(s) to add to the name.'"`
}

func (put *PutCmd) Run(ctx *kong.Context) error {
	var name Name
	Database.Preload("Tags").FirstOrCreate(&name, Name{Name: strings.TrimSpace(put.Name)})

	for _, tagVal := range put.Tags {
		tagVal = strings.TrimSpace(tagVal)

		var tag Tag
		Database.FirstOrCreate(&tag, Tag{Tag: tagVal})

		if !name.HasTag(tagVal) {
			name.Tags = append(name.Tags, tag)
		}
	}

	Database.Save(&name)

	if Args.Verbose {
		Database.Preload("Tags", TagOrder).First(&name, Name{Name: strings.TrimSpace(put.Name)})
		fmt.Fprintf(os.Stderr, "%s: %s\n", name.Name, strings.Join(name.TagStrings(), ", "))
	}

	return nil
}

type UntagCmd struct {
	Name string   `kong:"arg,required,name='name',help='The name to remove tags from.'"`
	Tags []string `kong:"arg,required,name='tag',help='Tag(s) to remove from the name.'"`
}

func (untag *UntagCmd) Run(ctx *kong.Context) error {
	var name Name
	Database.Preload("Tags").FirstOrCreate(&name, Name{Name: strings.TrimSpace(untag.Name)})
	name.Untag(untag.Tags...)

	if Args.Verbose {
		Database.Preload("Tags", TagOrder).First(&name, Name{Name: strings.TrimSpace(untag.Name)})
		fmt.Fprintf(os.Stderr, "%s: %s\n", name.Name, strings.Join(name.TagStrings(), ", "))
	}

	return nil
}

type RmCmd struct {
	Name string `kong:"arg,required,name='name',help='The name to remove.'"`
}

func (rm *RmCmd) Run(ctx *kong.Context) error {
	var name Name
	result := Database.First(&name, Name{Name: strings.TrimSpace(rm.Name)})
	if !errors.Is(result.Error, gorm.ErrRecordNotFound) {
		Database.Delete(&name)
	}

	return nil
}

type ListCmd struct {
	Tags  bool `kong:"short='t',help='List tags as well as names.'"`
	Count bool `kong:"short='c',help='Only output the number of names.'"`
}

func (list *ListCmd) Run(ctx *kong.Context) error {
	if list.Count {
		var count int64
		Database.Model(&Name{}).Count(&count)
		fmt.Println(count)
		return nil
	}

	var names []Name
	Database.Preload("Tags", TagOrder).Find(&names).Order("name DESC")

	for _, name := range names {
		if list.Tags {
			fmt.Printf("%s: %s\n", name.Name, strings.Join(name.TagStrings(), ", "))
		} else {
			fmt.Println(name.Name)
		}
	}

	return nil
}

type TagsCmd struct {
	Name string `kong:"arg,required,name='name',help='The name whose tags will be returned.'"`
}

func (tags *TagsCmd) Run(ctx *kong.Context) error {
	var name Name
	Database.Preload("Tags", TagOrder).FirstOrCreate(&name, Name{Name: strings.TrimSpace(tags.Name)})
	for _, tag := range name.Tags {
		fmt.Println(tag.Tag)
	}

	return nil
}

type RandCmd struct {
	Tags   []string `kong:"name='tag',short='t',help='Filter for names with the specified tag(s).'"`
	Stream bool     `kong:"name='stream',short='s',help='Stream a continuous list of random names.'"`
}

func (rand *RandCmd) Run(ctx *kong.Context) error {
	filters := ParseFilters(rand.Tags)

	if rand.Stream {
		for {
			outputNames(filters)
		}
	} else {
		outputNames(filters)
	}

	return nil
}

func outputNames(filters []Filter) {
	for i, filter := range filters {
		if i > 0 {
			fmt.Print(" ")
		}

		fmt.Print(randomName(filter))
	}

	fmt.Println()
}

func randomName(filter Filter) string {
	// TODO: Redo this in a way that doesn't require fetching all names first,
	// iterating through them. Turn `filter.go` into a way of generating DB
	// queries, maybe?
	var allNames []Name
	Database.Preload("Tags", TagOrder).Find(&allNames)

	matchedNames := make([]Name, 0, 1024)

	for _, name := range allNames {
		if filter.Match(name.Name, name.TagStrings()) {
			matchedNames = append(matchedNames, name)
		}
	}

	if len(matchedNames) == 0 {
		return ""
	}

	name := matchedNames[rand.Intn(len(matchedNames))]
	return name.Name
}
