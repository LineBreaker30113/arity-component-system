# Persistence (2 of 2)

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
