# acse

- The journal's `storeAll`: the author has a small feeling it fits, though databases often use a
  write-ahead log, which can come later (1); its format can be JSON or better, keeping every
  transaction in its own section, not one list (2).
- The journal's boolean data uses a bitset or Roaring, since a boolean array costs a byte per bit (3).
- Catalog setters and bulk writes log the change as `new ^ old`, for rows whose bit count does not
  change; that is not open (4, 5, 6). A client wanting both versions can write its own journal, or a
  separate one can be made (4).
- The journal's creation can be independent of the core: `indexer.bin` names its class beside the
  core's, and a load builds both through the folder's jar (7).
- Descriptions are a few strings that do not take much space; the easiest representation can do (8).
- Each class loads through a public static method, and `indexer.bin` names each class (9);
  `ACSE_PersistenceManager`, not the core, stores to and loads from new numbered folders each time (10).
- It would be cool to store all the classes as a jar (11), one per save if space is not concerning, as
  Gradle does (12); a class without a static loadFrom is left out with a warning, since it likely does
  not need to persist (13).
- Linking unchanged files between stores is version control; the author doubts we should, wanting the
  database somewhat simple (14).
- `arity/sets/CLAUDE.md`: the identity set implementations.
