# Persistence — how is a database stored, loaded and created?

Quotes of `citable.md` → Persistence, written 2026-10-21. Rules: `CLAUDE.md` → Decisions.

1. “I think we will not change this database once it is done” · “I guess we can handle” · “bump the
   FORMAT” · “if we will support former versions we will need to store all of the former version
   classes”
2. “won't have multiple programs” · “open as a service, [the] rest are requests” · “include Roaring
   as a dependency”
3. “acses will use only classes from the database” · “the authority about implementation”
4. “take a target folder and store everything in their files” · “a public static method, and an
   indexer.bin”
5. “it shouldn't [be] the responsibility of this class [the core]” · “make an
   "ACSE_PersistenceManager" class” · “We can make a persistence manager contract” · “in the root
   folder” · “We can make the persistence manager also variable”
6. “it would be so cool if it was able to store all the classes as a jar file” · “If space is not
   concerning, adding the jar for each save”
7. “not a method of saving, likely it doesn't need to persist. Just warn about it”
8. “you sure we should do it?” · “keep the database somewhat simple” · “gave up the idea for [the]
   ability to change base classes” · “can just decide to”
9. “provide the base class files only once” · “stored for the ACSES application” · “using the
   "config" folder is not obligatory”
10. “creates an empty core, then saves everything with persistence” · “finds the current folder from
    the acse-persistence.data” · “loads all the necessary instances using the static load methods” ·
    “didn't give it with "--calledBy <name>"”
