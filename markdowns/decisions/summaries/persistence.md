# Persistence — summary

Claude's summary of `decisions.md` → Persistence and `persistence.md`; written 2026-10-18. A reading
aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

## General

- The author thinks this database will not change once done: it is not planned as a library in near
  time, so the author guesses class updates can be handled by bumping FORMAT; supporting former
  versions would mean storing all their classes, hard for version control.
  > “remembering to bump the FORMAT” · “store all of the former version classes”
- No multiple programs: one program open as a service, the rest are requests.
  > “won't have multiple programs”
- Roaring is a dependency.
  > “include Roaring as a dependency”
- Loading a database, acses uses only its classes and only knows the contracts: the database is the
  authority about implementation.
  > “the authority about implementation”

## Details

- The core persists through methods (and contracts where necessary) that store everything into a
  target folder (2026-10-11).
  > “take a target folder and store everything in their files”
- Loading uses a public static method per class, listed in indexer.bin, not no-argument constructors
  (2026-10-12).
  > “make a public static method, and an indexer.bin”
- Numbered folders shouldn't be the core's job: ACSE_PersistenceManager loads or stores into a new
  folder each time.
  > “make an "ACSE_PersistenceManager" class”
- The persistence manager could store all classes as a jar, for easier loading.
  > “store all the classes as a jar file”
- If space is not a concern, a jar on each save gives locality of behaviour, as Gradle does.
  > “it is what Gradle does”
- A class without a static loadFrom has not a saving method, so it likely doesn't need to persist:
  leave it out, with a warning.
  > “Just warn about it”
- A persistence manager contract can exist; the manager class sits in the root folder.
  > “in the root folder”
- The persistence manager can be variable (2026-10-13).
  > “persistence manager also variable”
- Linking unchanged files between stores is version control; the author doubts we should, to keep
  the database simple.
  > “that is version control, you sure we should do it?”
- Changing base classes is given up; a user can simply choose a different class.
  > “I gave up the idea”
- acses provides the base class files only once, at the database's creation.
  > “only once, during [the] creation of the database”
- Creating: acses copies acse-persistence.jar from the device config, loads it, loads the base
  classes from the device config, creates an empty core, and saves everything. Loading: acses loads
  the jar at the path; its persistence manager finds the current folder from acse-persistence.data,
  loads that jar and creates the core, which loads all instances by static load methods; the manager
  returns the core, recorded under the --calledBy name, or the folder's name if the request didn't
  give one.
  > “the acse-persistence.jar file, loads it” ·
  > “finds the current folder from the acse-persistence.data” ·
  > “didn't give it with "--calledBy <name>"”
- The initial implementation classes stay with the ACSES application, which copies them into each
  new database; a config folder is not obligatory (2026-10-14).
  > “using the "config" folder is not obligatory”
