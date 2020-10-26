# Nym

Name/word database and random name generator.

## Installation

```
$ go install github.com/tokenshift/nym
```

## Usage

```
$ nym --help
```

Add a name and one or more tags:

```
$ nym put {name} [-t {tag}]...

```

Remove one or more tags from a name:

```
$ nym untag Kostya Russian
```

Delete a name from the database (along with all of its tags):

```
$ nym rm Kostya
```

List all the names in the database (optionally with their tags):

```
$ nym ls [-t]
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

Get a random name that matches the specified tags:

```
$ nym rand -t "Male,Surname"
Wilson
```

Stream a continuous list of random names (accepts `-t`):

```
$ nym rand -s
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

Provide multiple tag filters to get a match for each on each line:

```
$ nym rand -s -t Female -t "Ancient Roman" -t Surname
Hildegard Ennius Van Althuis
Sabeen Alba Dawson
Łucja Aetius Berg
Deja Secundinus Leandres
Melina Cato Moser
Ilona Iulianus Franjic
Arista Octavianus Kendrick
Hena Caelia Petersson
Krystyna Julia Traviss
Élise Aelianus Dalca
Fidela Livia Masin
Magalie Marcellinus Moloney
Miloslava Tiberius Ziemniak
Thirza Antonia Werner
...
```

Load names & tags from an input text stream with names and tags
on alternating lings:

```
$ cat <<EOF
> Name1
> Tag1
> Name1
> Tag2
> Name2
> Tag1
> Name2
> Tag2
> EOF | nym load
```
