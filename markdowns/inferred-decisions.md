# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Markdown
files.

## Unreviewed

### Architecture

- Arities are the first type of element; structures are the second. *(Author: "I needed to make a
  "Structure" interface, meant to be the second type of elements (the relations between sorts of
  elements) in the "OntologyEngineCore".")*
- "Component" replaces "Module" as the word for plugins, in every name. *(Author: "rename things like
  "ArityComponent" and "StructureComponent".")*

### Identities and removal

- The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. *(Claude proposed that rule; the author
  answered: "We should allow a judgment call method alongside auto method (does what you say) for the
  client.")*
- Bulk catalog reads intersect the range with the occupied set once, then read, instead of checking
  each element. *(Claude proposed "intersect the range with occupied once, then read"; the author
  answered: "This can be handled.")*
- No structure vacuums a dead identity it reaches on its own, not only indexers; Claude's
  validate-and-prune suggestion is rejected. *(Author: "I am not sure I got it: consider there is an
  indexer (2-arity structure) that reached a dead identity. Should it vacuum it alone? That would be
  harmful.")*

### Urgent work — the old `ARCHITECTURE.md` handoffs

The task wording is Claude's; the facts in it were checked 2026-09-17. *(Author: "The handoffs are
things that are decided to be done and urgent.")*

- Design the binary structure (class ↔ element indexer) in `structures/indexers/`. Read
  `legacy/core/segments/manager/DoubleArrayedIndexer` first: `ArrayList<BitSet>` one way,
  `ArrayList<IntArrayList>` the other, with tombstone lists.
- Write `package-info.java` for `structures/graphs/` (the author writes it).
- Resolve `catalogs/LongIntCatalog`, an unreferenced near-duplicate of `IntegerValueCatalog`.
- Add gather to catalogs: read an arbitrary identity list, not only a contiguous range.
- Resolve catalog names to slots; nothing does today.
- DAG tests in `src/test/` (the author writes them). Coverage: encoding transitions, root list after
  every phase, cycle refusal, slot reuse, `isTight` both ways, loose-then-`tighten(ROOT)` equivalence,
  reduction preserving reachability.

### Words

Claude's definitions, mostly from `GOALS.md`. *(Claude proposed putting "the defined words and the
load-bearing assumptions" in `decisions.md`; the author answered: "Proposed split for goal is
accepted.")*

- **Ontology** — the philosophical sense: any classification or representation of elements as beings.
  Not OWL, RDF, or a reasoner.
- **Entity / identity** — a bare non-negative `int` or `long`, minted densely from 0, unique within its
  arity. Carries uniqueness only.
- **Arity** (noun) — one identity space: the occupied set plus its columns. As a number, how many
  identity spaces a structure spans.
- **Catalog** — one value per identity; a column. Total: an unwritten slot reads as zero, not absent.
- **Structure** — data about relations, over one arity or several, under any rule.
- **Component** — a plugin the core holds (`ArityComponent`, `StructureComponent`).

### Terms used in questions and inferences

Claude's wording, written because `CLAUDE.md` requires a term the author does not already use to be
defined where it first appears. Delete any that are already the author's own.

- **journal** — the single ordered list of what changed. Undo, redo and logging read it instead of
  each keeping a record of their own.
- **slot** — a numbered place in an array or list, used instead of a name or an object reference. The
  DAG's `slotOf` is one.
- **gather** — reading the values for a scattered list of identities, instead of a contiguous range.
- **proxy** — an object standing in front of another, running something extra (a lock, a log line)
  before passing the call on.
- **codec** — the pair of methods that write a structure's change into the journal and read it back.
- **write barrier** — one private method every mutator calls before changing anything, so a journal
  entry cannot be forgotten in one place.
- **barrier** — a point where deferred work must finish before anything continues; the sync barrier.
- **tombstone** — a marker left where an element was removed, so the positions after it do not move.
- **single writer** — only one thread changes the engine at a time; readers work on a snapshot.
- **snapshot** — a copy of the state, or of a region, kept before a change.
- **invert** — a logging mode: the record holds what is needed to reverse the change.
- **note-only** — a logging mode: the journal records that something changed, but not the old values,
  because that state can be rebuilt.
- **restore** — bringing a removed identity's data back under a new identity number. Not undo, which
  returns the original number.
- **normalise** — rewriting a filter tree into a standard shape before running it, so the parts can be
  compared and reordered.
- **n-ary** — taking any number of inputs: one AND over five filters, rather than four nested pairs.
- **monotonic** — a number that only ever goes up, never down; `everReached` would be one.
- **microkernel / kernel** — the smallest core that only holds the components and passes work between
  them. Also called the plug-in pattern.
- **ECS** — Entity Component System, the game-engine pattern: bare entity ids with their data in
  columns. Its "component" is what this project calls a catalog.
- **chunked** — storing a column in fixed-size blocks instead of one array, so growing adds a block.

### Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **Identities are dense and local to an arity**, minted from 0, so their values stay small. Ends if
  identities become global across arities or carry generation counters.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
