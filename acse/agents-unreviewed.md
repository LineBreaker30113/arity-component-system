# acse: entries to review

# Unreviewed

- A row a request makes keeps its file at the database's root, `arity-<k>-row-<j>.bin`, or
  `arity-<k>-row-<j>plus.bin` for one file of several rows; a store keeps its path relative to the root,
  and a load copies the stored file to the loaded database's root. *(Author, 2026-10-19: "Use a path
  relative to database root, created automatically, calculated by arity and row index like data.";
  "Having a separate file for every type of record is better in general, but we still allowed a single
  file for multi row option, we can append its name "plus" as in `arity-<k>-row-<j>plus.bin`.";
  2026-10-20: "the file of the file row moves to the version on save, so the root also serves as a
  temporal place. Thus copy them to the database root on load.")*
  Section: Persistence / Details
- A removed component leaves a tombstone rather than shifting the pointers, which also allows a
  compacting load. *(Author, 2026-10-20: "in case of an Component removal, we need to either shift the
  pointers or to keep tombstones. I will go with tombstones, it also allows loading with a compacting.
  Lastly, since in the intended (for the project vision) use case there will be at most 2^20 arities (of
  2^(22 to 28) identities), it is not going to be a huge problem.")*
  Section: Architecture
