package main

import (
	"strings"

	"gorm.io/gorm"
)

type Name struct {
	gorm.Model
	Name string
	Tags []Tag `gorm:"many2many:name_tags;"`
}

func (n Name) HasTag(tag string) bool {
	tag = strings.TrimSpace(tag)

	for _, t := range n.Tags {
		if t.Tag == tag {
			return true
		}
	}

	return false
}

func (n *Name) Untag(tags ...string) {
	for _, tag := range n.Tags {
		for _, untag := range tags {
			if tag.Tag == untag {
				Database.Model(&n).Association("Tags").Delete(tag)
			}
		}
	}
}

func (n Name) TagStrings() []string {
	tags := make([]string, 0, len(n.Tags))

	for _, tag := range n.Tags {
		tags = append(tags, tag.Tag)
	}

	return tags
}

type Tag struct {
	gorm.Model
	Tag   string
	Names []Name `gorm:"many2many:name_tags;"`
}

func (t Tag) HasName(name string) bool {
	name = strings.TrimSpace(name)

	for _, n := range t.Names {
		if n.Name == name {
			return true
		}
	}

	return false
}

func TagOrder(db *gorm.DB) *gorm.DB {
	return db.Order("tags.tag ASC")
}

type NameTag struct {
	TagID  uint
	NameID uint
}
