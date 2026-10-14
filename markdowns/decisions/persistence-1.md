# Persistence (1 of 2)

- I want you to properly handle the persistence using methods in the core (and contracts where
  necessary) that take a target folder and store everything in their files. *(2026-10-11)*
- [Instead of a no-argument constructor and fields that stop being final:] make a public static
  method, and an indexer.bin that tells each class. *(2026-10-12)*
- [The numbered folders 000001/, 000002/, …] are cool, but it shouldn't [be] the responsibility of
  this class [the core]; make an "ACSE_PersistenceManager" class that loads or stores in new folders
  each time.
- [The persistence manager:] it would be so cool if it was able to store all the classes as a jar file
  for easier loading.
- If space is not concerning, adding the jar for each save for locality of behaviour is a good idea;
  it is what Gradle does.
- [A class without a static loadFrom:] leave it out, because if it has not a method of saving, likely
  it doesn't need to persist. Just warn about it.
- We can make a persistence manager contract, and the persistence manager class will [be] in the root
  folder.
- We can make the persistence manager also variable. *(2026-10-13)*
- [Linking unchanged files between stores:] that is version control, you sure we should do it? I
  wanted to keep the database somewhat simple.
- I think we will not change this database once it is done, I mean since I am not planning to provide
  this as a database library in near time, I guess we can handle everything about the class updates
  with remembering to bump the FORMAT; after all, if we will support former versions we will need to
  store all of the former version classes, which is hard for version control.
- I gave up the idea for [the] ability to change base classes; if [a] user wants to use a different
  class, it can just decide to.
- The expected use case won't have multiple programs. One program that will be open as a service,
  [the] rest are requests.
- Let's include Roaring as a dependency.
- Now the new plan is acses will work as a bootstrapper when loading databases, I mean even the base
  classes will move to the target workspace; the acses will be responsible for loading them using the
  convention.
- The acses is thin: if there is already a process of acses running it will only be a requester; if
  not it will become the process.
- When requested to load a database, acses will use only classes from the database; acses only knows
  the contracts, so [the] database is the authority about implementation.
- When acses is going to create a new database, the acses will provide the base class files only
  once, during [the] creation of the database.
