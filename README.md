# Nym

Name/word database and random name generator.

## Installation

```
$ go install github.com/tokenshift/nym
```

## Usage

Add a name and one or more tags:

```
$ nym put {name} [-t {tag}]...

```

Remove one or more tags from a name:

```
$ nym untag Kostya -t Rusisan
```

Delete a name from the database (along with all of its tags):

```
$ nym rm Kostya
```

Get all of the tags associated with a name:

```
$ nym tags Marinos
Surname
Greek
Male
```

Get a random name:

```
$ nym rand
Alan
```

Get a random name matching the specified glob-style string:

```
$ nym rand -p "Patr*"
Patrick
```

Get a name that matches the specified tags:

```
$ nym rand -q "Male,Surname"
Wilson
```

Stream a continuous list of random names (accepts -q and -Q):

```
$ nym rand -f
Shayne
Peony
Methoataske
Barna
Amantius
Amita
Aslı
Nicolasa
Pearce
Rafael
Suzy
...
```
