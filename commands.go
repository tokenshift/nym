package main

import (
	"bufio"
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
	PutNameTags(put.Name, put.Tags...)

	if Args.Verbose {
		var name Name
		Database.Preload("Tags", TagOrder).First(&name, Name{Name: strings.TrimSpace(put.Name)})
		fmt.Fprintf(os.Stderr, "%s: %s\n", name.Name, strings.Join(name.TagStrings(), ", "))
	}

	return nil
}

func PutNameTags(name string, tags ...string) {
	name = strings.TrimSpace(name)

	var n Name
	Database.Preload("Tags").FirstOrCreate(&n, Name{Name: name})

	for _, tagVal := range tags {
		tagVal = strings.TrimSpace(tagVal)

		var tag Tag
		Database.FirstOrCreate(&tag, Tag{Tag: tagVal})

		if !n.HasTag(tagVal) {
			n.Tags = append(n.Tags, tag)
		}
	}

	Database.Save(&n)
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
	Database.Preload("Tags", TagOrder).Order("name ASC").Find(&names)

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
	Names []string `kong:"arg,required,name='name',help='The name(s) whose tags will be returned.'"`
}

func (tags *TagsCmd) Run(ctx *kong.Context) error {
	for _, n := range tags.Names {
		if name, ok := getName(n); ok {
			fmt.Print(name.Name, ":")
			for i, tag := range name.Tags {
				if i > 0 {
					fmt.Print(",")
				}

				fmt.Print(" ", tag.Tag)
			}
			fmt.Println()
		} else {
			fmt.Printf("%s: NOT FOUND\n", strings.TrimSpace(n))
		}
	}

	return nil
}

func getName(name string) (result Name, ok bool) {
	tx := Database.Preload("Tags", TagOrder).First(&result, Name{Name: strings.TrimSpace(name)})

	if tx.RowsAffected == 0 {
		// Try a case-insensitive search
		tx = Database.Preload("Tags", TagOrder).
			Where("name LIKE ?", strings.TrimSpace(name)).
			First(&result)
	}

	if tx.RowsAffected == 0 {
		return result, false
	}

	return result, true
}

type RandCmd struct {
	Number int      `kong:"short='n',xor='Number',default='1',help='Number of names to return. Defaults to 1.'"`
	Tags   []string `kong:"name='tag',short='t',sep='none',help='Filter for names with the specified tag(s).'"`
	Stream bool     `kong:"name='stream',short='s',xor='Number',help='Stream a continuous list of random names.'"`
}

func (rand *RandCmd) Run(ctx *kong.Context) error {
	// TODO: Parse the filters once, instead of re-parsing them for every selection.
	if rand.Stream {
		for {
			outputRandomNames(rand.Tags)
		}
	} else {
		for i := 0; i < rand.Number; i++ {
			outputRandomNames(rand.Tags)
		}
	}

	return nil
}

func outputRandomNames(filters []string) {
	if len(filters) == 0 {
		if name, ok := getAnyRandomName(); ok {
			fmt.Println(name.Name)
		} else {
			fmt.Println("<NONE>")
		}

		return
	}

	for i, filter := range filters {
		if i > 0 {
			fmt.Print(" ")
		}

		if name, ok := getRandomName(filter); ok {
			fmt.Print(name.Name)
		} else {
			fmt.Print("<NONE>")
		}
	}

	fmt.Println()
}

type LoadCmd struct{}

func (load *LoadCmd) Run(ctx *kong.Context) error {
	scanner := bufio.NewScanner(os.Stdin)

	var name, tag string

	readName := true

	for scanner.Scan() {
		if readName {
			name = strings.TrimSpace(scanner.Text())
		} else {
			tag = strings.TrimSpace(scanner.Text())

			if name != "" && tag != "" {
				if Args.Verbose {
					fmt.Println(name, "=>", tag)
				}

				PutNameTags(name, tag)
			}
		}

		readName = !readName
	}

	return nil
}
