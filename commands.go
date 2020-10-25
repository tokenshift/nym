package main

import (
	"errors"
	"fmt"
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
	Tags   []string `kong:"name='tag',short='t',sep='none',help='Filter for names with the specified tag(s).'"`
	Stream bool     `kong:"name='stream',short='s',help='Stream a continuous list of random names.'"`
}

func (rand *RandCmd) Run(ctx *kong.Context) error {
	if rand.Stream {
		for {
			outputRandomNames(rand.Tags)
		}
	} else {
		outputRandomNames(rand.Tags)
	}

	return nil
}

func outputRandomNames(filters []string) {
	if len(filters) == 0 {
		fmt.Println(getAnyRandomName())
		return
	}

	for i, filter := range filters {
		if i > 0 {
			fmt.Print(" ")
		}

		fmt.Print(getRandomName(filter))
	}

	fmt.Println()
}
