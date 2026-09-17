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

### Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **Identities are dense and local to an arity**, minted from 0, so their values stay small. Ends if
  identities become global across arities or carry generation counters.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
