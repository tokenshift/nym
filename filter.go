package main

import (
	"strings"
)

// A Filter is a predicate on a name and its tags.
type Filter interface {
	Match(name string, tags []string) bool
}

// Helper pred that matches any name.
type AlwaysTrue struct {}
func (_ AlwaysTrue) Match(name string, tags []string) bool {
	return true
}

// "Normal" filters are disjunctions of conjunctions.

type Disjunction []Conjunction
type Conjunction []string

func ParseFilters(filterStrings []string) []Filter {
	filters := make([]Filter, 0, len(filterStrings))

	for _, filterString := range filterStrings {
		filter := ParseDisjunction(filterString)
		if filter != nil {
			filters = append(filters, filter)
		}
	}

	// By default, use the "always true" predicate.
	if len(filters) == 0 {
		filters = append(filters, AlwaysTrue{})
	}

	return filters
}

func ParseDisjunction(input string) Disjunction {
	input = strings.TrimSpace(input)

	if input == "" {
		return nil
	}

	clauses := strings.Split(input, "|")

	conjs := make([]Conjunction, 0, len(clauses))

	for _, clause := range clauses {
		conj := ParseConjunction(clause)
		if conj != nil {
			conjs = append(conjs, conj)
		}
	}

	return Disjunction(conjs)
}

func ParseConjunction(input string) Conjunction {
	tags := strings.Split(input, ",")

	// Filter out any blank strings.
	for i, tag := range tags {
		tag = strings.TrimSpace(tag)
		if tag == "" {
			tags = append(tags[:i], tags[i+1:]...)
		} else {
			tags[i] = tag
		}
	}

	return Conjunction(tags)
}

func (this Disjunction) Match(name string, tags []string) bool {
	for _, conj := range this {
		if conj.Match(name, tags) {
			return true
		}
	}

	return false
}

func (this Conjunction) Match(name string, tags []string) bool {
	for _, tag := range this {
		matched := false

		for _, otherTag := range tags {
			if tag == otherTag {
				matched = true
				break
			}
		}

		if !matched {
			return false
		}
	}

	return true
}
