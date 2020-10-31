package main

import (
	"math/rand"
	"strings"
)

func getAnyRandomName() (result Name, ok bool) {
	var count int64
	Database.Model(&Name{}).Count(&count)

	if count == 0 {
		return result, false
	}

	r := rand.Intn(int(count))

	Database.Preload("Tags").Offset(r).First(&result)

	return result, true
}

func getNameIdsFromFilter(filterString string) []uint {
	// Step 1: Break the filter string into a disjunction of conjunctions of tags.
	// => [["foo", "bar"],["fizz"],["bar","buzz"]]
	filter := parseFilter(filterString)

	// Step 2: Get a flattened list of all the tags in the disj-of-conj.
	// => ["foo", "bar", "fizz", "buzz"]
	flattened := flattenFilter(filter)

	// Step 3: Get a mapping of tag values to IDs.
	// => ["foo":1,"bar":2,"fizz":3,"buzz":4]
	tagIds := getTagIds(flattened)

	// Step 4: Convert [][]string into [][]int (tag strings into tag IDs).
	// => [[1,2],[3],[2,4]]
	filterTagIds := convertFilterTagsToIds(tagIds, filter)

	// Step 5: Get a flattened list of all the tag IDs
	flattenedTagIds := flattenTagIds(filterTagIds)

	// Step 6: Get all NameTags with matching tag IDs, and turn into a lookup
	// table of tag IDs to lists of matching name IDs (map[uint][]uint).
	tagNameIds := getNameIdsFromTagIds(flattenedTagIds)

	// Step 7: For each tag ID in the filter, convert it into a list of name IDs
	// that have that tag. This should produce a [][][]uint--a disjunction of
	// conjunctions of lists of name IDs.
	// => [[[1,2,3,4],[2,4,7]],[[5]],[[2,4,7],[8]]]
	filterNameLists := convertFilterTagIdsToNameLists(tagNameIds, filterTagIds)

	// Step 8: Replace each conjunction (a list of lists of name IDs) with the
	// intersection of those lists, containing only the name IDs that show up
	// in all the lists in the conjunction.
	// => [[2,4],[5],[]]
	intersectedConjunctions := intersectNameListConjunctions(filterNameLists)

	// Step 9: Perform a union of the lists of name IDs the disjunction now contains.
	// => [2,4,5]
	unionedDisjunctions := unionNameLists(intersectedConjunctions)
	return unionedDisjunctions
}

func getRandomName(nameIds []uint) (result Name, ok bool) {
	if (len(nameIds)) == 0 {
		return result, false
	}

	r := rand.Intn(len(nameIds))
	nameId := nameIds[r]
	Database.Preload("Tags").First(&result, nameId)
	return result, true
}

// parseFilter parses a filter string into a slice of slices of (tag) strings.
//
// Filters are disjunctions of conjunctions, of the form:
// "Foo|Bar,Fizz|Buzz", where `|` is a logical OR, and `,` is an AND.
func parseFilter(filterString string) [][]string {
	filter := make([][]string, 0)

	filterString = strings.TrimSpace(filterString)
	if filterString == "" {
		return filter
	}

	clauses := strings.Split(filterString, "|")

	for _, clause := range clauses {
		clause = strings.TrimSpace(clause)

		tags := strings.Split(clause, ",")

		for i, tag := range tags {
			tag = strings.TrimSpace(tag)
			if tag == "" {
				tags = append(tags[:i], tags[i+1:]...)
			} else {
				tags[i] = tag
			}
		}

		filter = append(filter, tags)
	}

	return filter
}

func flattenFilter(filter [][]string) []string {
	flattened := NewStringSet()

	for _, tags := range filter {
		for i, tag := range tags {
			tags[i] = strings.TrimSpace(tag)
			flattened.Add(tags[i])
		}
	}

	return flattened.ToList()
}

func getTagIds(tagNames []string) map[string]uint {
	var tags []Tag
	Database.Where("tag IN ?", tagNames).Find(&tags)

	tagIds := make(map[string]uint)

	for _, tag := range tags {
		tagIds[tag.Tag] = tag.ID
	}

	return tagIds
}

func convertFilterTagsToIds(tagIds map[string]uint, filter [][]string) [][]uint {
	rows := make([][]uint, len(filter))

	for i, tags := range filter {
		row := make([]uint, len(tags))

		for j, tag := range tags {
			row[j] = tagIds[tag]
		}

		rows[i] = row
	}

	return rows
}

func flattenTagIds(tagIds [][]uint) []uint {
	flattened := NewUintSet()

	for _, ids := range tagIds {
		for _, id := range ids {
			flattened.Add(id)
		}
	}

	return flattened.ToList()
}

func getNameIdsFromTagIds(tagIds []uint) map[uint][]uint {
	// Step 6: Get all NameTags with matching tag IDs, and turn into a lookup
	// table of tag IDs to lists of matching name IDs (map[uint][]uint).
	var nameTags []NameTag
	Database.Where("tag_id IN ?", tagIds).Find(&nameTags)

	tagNames := make(map[uint][]uint)

	for _, nameTag := range nameTags {
		if _, ok := tagNames[nameTag.TagID]; !ok {
			tagNames[nameTag.TagID] = make([]uint, 0)
		}

		tagNames[nameTag.TagID] = append(tagNames[nameTag.TagID], nameTag.NameID)
	}

	return tagNames
}

func convertFilterTagIdsToNameLists(tagNameIds map[uint][]uint, filterTagIds [][]uint) [][][]uint {
	// Step 7: For each tag ID in the filter, convert it into a list of name IDs
	// that have that tag. This should produce a [][][]uint--a disjunction of
	// conjunctions of lists of name IDs.

	result := make([][][]uint, len(filterTagIds))

	for i, conj := range filterTagIds {
		result[i] = make([][]uint, len(conj))

		for j, tagId := range conj {
			result[i][j] = tagNameIds[tagId]
		}
	}

	return result
}

func intersectNameListConjunctions(filterNameLists [][][]uint) [][]uint {
	// Step 8: Replace each conjunction (a list of lists of name IDs) with the
	// intersection of those lists, containing only the name IDs that show up
	// in all the lists in the conjunction.

	result := make([][]uint, len(filterNameLists))

	for i, conj := range filterNameLists {
		var intersection UintSet

		for j, nameIds := range conj {
			other := NewUintSet()
			other.AddList(nameIds)

			if j == 0 {
				intersection = other
			} else {
				intersection = intersection.Intersection(other)
			}
		}

		result[i] = intersection.ToList()
	}

	return result
}

func unionNameLists(intersectedConjunctions [][]uint) []uint {
	// Step 9: Perform a union of the lists of name IDs the disjunction now contains.

	result := NewUintSet()

	for _, conj := range intersectedConjunctions {
		result.AddList(conj)
	}

	return result.ToList()
}
