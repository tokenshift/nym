package main

type StringSet map[string]struct{}

func NewStringSet() StringSet {
	return make(map[string]struct{})
}

func (s StringSet) Add(val string) bool {
	if _, ok := s[val]; ok {
		return false
	}

	s[val] = struct{}{}
	return true
}

func (s StringSet) Remove(val string) bool {
	if _, ok := s[val]; ok {
		delete(s, val)
		return true
	}

	return false
}

func (s StringSet) Count() int {
	return len(s)
}

func (s StringSet) ToList() []string {
	list := make([]string, 0, s.Count())

	for val, _ := range s {
		list = append(list, val)
	}

	return list
}
