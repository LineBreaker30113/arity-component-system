# Terms

One-line definitions of terms used in the markdowns and in answers, looked up by name when met.
Rules: `CLAUDE.md` → Markdown files.

- **abstraction barrier** — the line a contract draws: code using it calls only its methods and never sees the representation behind them, so the representation can change unnoticed (SICP §2.1.2).
- **chunked** — a column stored in fixed-size blocks instead of one array, so growing adds a block.
- **codec** — the pair of methods that write a structure's changes into the journal and read them back.
- **composable** — put together from parts added and removed one at a time: `ACSE_ComposableArityCatalog` is its catalog rows, each its own object in a slot, where a hand-written arity is one class with its columns as fields.
- **ECS** — Entity Component System: bare entity ids, data in columns; its "component" is our catalog.
- **gather** — reading the values of a scattered list of identities, not a contiguous range.
- **horizon** — the point undo cannot go back past: a vacuum (decided), or a step that breaks undo history; `ACSE_JournalBasic.horizon` is the first transaction still undoable.
- **invert** — a logging mode whose record holds what is needed to reverse the change.
- **journal** — the one ordered list of what changed; undo, redo and logging all read it.
- **locality of behaviour** — a unit's behaviour is plain from reading that unit alone, as each component's `request` shows what it accepts; it trades against separation of concerns, which the journal's undo follows (Carson Gross, htmx.org, 2020, after Richard Gabriel's *Patterns of Software*).
- **n-ary** — taking any number of inputs: one AND over five filters, not four nested pairs.
- **normalise** — rewriting a filter tree into a standard shape, so its parts can be compared and reordered.
- **proxy** — a class that handles entry-exit conditions of another class.
- **single writer** — one thread changes the engine at a time; readers work on a snapshot.
- **slot** — a numbered place in an array or list, used instead of a name or an object reference.
- **snapshot** — a copy of the state, or of a region, kept before a change.
- **SPDX** — the standard short names for licences and exceptions that tools and package registries read, such as `AGPL-3.0-only WITH Universal-FOSS-exception-1.0`.
- **tombstone** — a marker left where an element was removed, so the positions after it do not move.
- **walk** — finding every class a class needs to run: read its class file for the classes it names, keep those from the same jar, and repeat on each one kept until none is new; `ACSE_PersistenceManagerBasic.storeClasses` does it to fill `classes.jar`.
- **write-ahead log** — the journal written to disk before a change is applied, so a crash can replay or roll it back.
- **write barrier** — one private method every mutator calls first, so no journal entry is forgotten.
