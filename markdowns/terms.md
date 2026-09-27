# Terms

One-line definitions of terms used in the markdowns and in answers, looked up by name when met.
Rules: `CLAUDE.md` → Markdown files.

- **barrier** — a point where deferred work must finish before anything continues.
- **chunked** — a column stored in fixed-size blocks instead of one array, so growing adds a block.
- **codec** — the pair of methods that write a structure's changes into the journal and read them back.
- **ECS** — Entity Component System: bare entity ids, data in columns; its "component" is our catalog.
- **gather** — reading the values of a scattered list of identities, not a contiguous range.
- **invert** — a logging mode whose record holds what is needed to reverse the change.
- **journal** — the one ordered list of what changed; undo, redo and logging all read it.
- **microkernel / kernel** — the smallest core, holding components and passing work between them; the plug-in pattern.
- **monotonic** — a number that only ever goes up, like `everReached`.
- **n-ary** — taking any number of inputs: one AND over five filters, not four nested pairs.
- **normalise** — rewriting a filter tree into a standard shape, so its parts can be compared and reordered.
- **note-only** — a logging method recording only that something changed, since the state can be rebuilt.
- **proxy** — a class that handles entry-exit conditions of another class.
- **restore** — bringing a removed identity's data back under a new number; undo keeps the original.
- **single writer** — one thread changes the engine at a time; readers work on a snapshot.
- **slot** — a numbered place in an array or list, used instead of a name or an object reference.
- **snapshot** — a copy of the state, or of a region, kept before a change.
- **tombstone** — a marker left where an element was removed, so the positions after it do not move.
- **write barrier** — one private method every mutator calls first, so no journal entry is forgotten.
