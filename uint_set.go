package main

type UintSet map[uint]struct{}

func NewUintSet() UintSet {
	return make(map[uint]struct{})
}

func (s UintSet) Add(val uint) bool {
	if _, ok := s[val]; ok {
		return false
	}

	s[val] = struct{}{}
	return true
}

func (s UintSet) AddList(vals []uint) int {
	count := 0

	for _, val := range vals {
		if s.Add(val) {
			count++
		}
	}

	return count
}

func (s UintSet) Contains(val uint) bool {
	_, ok := s[val]
	return ok
}

func (s UintSet) Remove(val uint) bool {
	if _, ok := s[val]; ok {
		delete(s, val)
		return true
	}

	return false
}

func (s UintSet) Count() int {
	return len(s)
}

func (s UintSet) Intersection(other UintSet) UintSet {
	result := NewUintSet()

	for val, _ := range s {
		if other.Contains(val) {
			result.Add(val)
		}
	}

	return result
}

func (s UintSet) ToList() []uint {
	list := make([]uint, 0, s.Count())

	for val, _ := range s {
		list = append(list, val)
	}

	return list
}
