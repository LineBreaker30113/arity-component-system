# Persistence — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

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
- I gave up the idea for [the] ability to change base classes; if [a] user wants to use a different
  class, it can just decide to.
- When acses is going to create a new database, the acses will provide the base class files only
  once, during [the] creation of the database.
- [The two cases:]
  > Acses given a task to create a database in some path: ACSES creates (by copying from the device
  > config or equals) the acse-persistence.jar file, loads it. Then [it] loads the base implementation
  > classes from [the] device config file (or whatever it is called). Then [it] creates an empty core,
  > then saves everything with persistence.
  >
  > Acses given a task to load a database: it loads the acse-persistence.jar file on the target path.
  > [The] rest is handled by the persistence manager instance found there: the persistence manager
  > finds the current folder from the acse-persistence.data (hereby, for less confusion). It loads the
  > jar on the target folder, and creates the core instance. [The] core instance loads all the
  > necessary instances using the static load methods of the classes in the jar. [The] persistence
  > manager returns the core object. The acses service records the core with a name; if [the] request
  > didn't give it with "--calledBy <name>", the name of the folder that contains the
  > acse-persistence.jar file is the name.
- [The device config:] I mean the initial implementation classes we are creating here will be stored
  for the ACSES application; the user will run the ACSES application to create a database anywhere it
  wants, then the ACSES application will copy class files on its side; using the "config" folder is
  not obligatory. *(2026-10-14)*
