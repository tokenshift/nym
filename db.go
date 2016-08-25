package main

import (
	"bytes"
	"math/rand"
	"sort"
	"strings"
	"time"

	"github.com/boltdb/bolt"
)

type DB interface {
	AddName(name string)
	AddTag(name, tag string)
	Close()
	GetTags(name string) []string
	RandomName(filter Filter) string
	RemoveName(name string)
	RemoveTag(name, tag string)
}

type dbWrapper struct {
	db *bolt.DB
}

func NewDB(filename string) (DB, error) {
	if db, err := bolt.Open(filename, 0600, &bolt.Options{Timeout: 1 * time.Second}); err != nil {
		return nil, err
	} else {
		db.Update(func(tx *bolt.Tx) error {
			tx.CreateBucketIfNotExists([]byte("Names"))
			return nil
		})
		return dbWrapper{db}, nil
	}
}

func bytesToStrings(bs []byte) []string {
	words := bytes.Split(bs, []byte{0})
	ss := make([]string, 0, len(words))
	for _, word := range words {
		if len(word) > 0 {
			ss = append(ss, string(word))
		}
	}
	return ss
}

func stringsToBytes(ss []string) []byte {
	return []byte(strings.Join(ss, "\x00"))
}

func (self dbWrapper) AddName(name string) {
	name = strings.TrimSpace(name)
	if name == "" {
		return
	}

	self.db.Update(func(tx *bolt.Tx) error {
		bucket := tx.Bucket([]byte("Names"))
		tags := bucket.Get([]byte(name))
		if tags == nil {
			bucket.Put([]byte(name), []byte{})
		}

		return nil
	})
}

func (self dbWrapper) AddTag(name, tag string) {
	name = strings.TrimSpace(name)
	tag  = strings.TrimSpace(tag)
	if name == "" || tag == ""{
		return
	}

	self.db.Update(func(tx *bolt.Tx) error {
		bucket := tx.Bucket([]byte("Names"))
		tags := bucket.Get([]byte(name))
		if len(tags) == 0 {
			bucket.Put([]byte(name), []byte(tag))
		} else {
			tags = append(tags, 0)
			tags = append(tags, []byte(tag)...)
			bucket.Put([]byte(name), tags)
		}

		return nil
	})
}

func (self dbWrapper) Close() {
	self.db.Close()
}

func (self dbWrapper) GetTags(name string) []string {
	var tags []string

	self.db.View(func(tx *bolt.Tx) error {
		bucket := tx.Bucket([]byte("Names"))
		buffer := bucket.Get([]byte(name))

		if buffer != nil {
			tags = bytesToStrings(buffer)
		}

		return nil
	})

	sort.Strings(tags)
	return tags
}

func (self dbWrapper) RandomName(filter Filter) string {
	matchedName := "NO_MATCH"

	self.db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("Names"))
		c := b.Cursor()

		i := 0
		for k, v := c.First(); k != nil; k, v = c.Next() {
			name := string(k)
			tags := bytesToStrings(v)

			if !filter.Match(name, tags) {
				continue
			}

			// [Reservoir sampling](https://en.wikipedia.org/wiki/Reservoir_sampling)
			r := rand.Intn(i+1)
			if r == 0 {
				matchedName = name
			}

			i += 1
		}

		return nil
	})

	return matchedName
}

func (self dbWrapper) RemoveName(name string) {
	name = strings.TrimSpace(name)

	self.db.Update(func(tx *bolt.Tx) error {
		bucket := tx.Bucket([]byte("Names"))
		bucket.Delete([]byte(name))
		return nil
	})
}

func (self dbWrapper) RemoveTag(name, tagToRemove string) {
	name = strings.TrimSpace(name)

	self.db.Update(func(tx *bolt.Tx) error {
		bucket := tx.Bucket([]byte("Names"))
		buffer := bucket.Get([]byte(name))

		if buffer != nil {
			tags := strings.Split(string(buffer), "\x00")

			for i, tag := range tags {
				if tag == tagToRemove {
					tags = append(tags[:i], tags[i+1:]...)
				}
			}

			bucket.Put([]byte(name), []byte(strings.Join(tags, "\x00")))
		}

		return nil
	})
}
