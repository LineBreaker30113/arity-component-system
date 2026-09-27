# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Markdown
files.

## Unreviewed

### Urgent work — the old `ARCHITECTURE.md` handoffs

The task wording is Claude's; the facts in it were checked 2026-09-17. *(Author: "The handoffs are
things that are decided to be done and urgent.")*

- Design the binary structure (class ↔ element indexer) in `structures/indexers/`. Read
  `legacy/core/segments/manager/DoubleArrayedIndexer` first: `ArrayList<BitSet>` one way,
  `ArrayList<IntArrayList>` the other, with tombstone lists.
- Write `package-info.java` for `structures/graphs/` (the author writes it).
- Resolve `catalogs/LongIntCatalog`, an unreferenced near-duplicate of `IntegerValueCatalog`.
- DAG tests in `src/test/` (the author writes them). Coverage: encoding transitions, root list after
  every phase, cycle refusal, slot reuse, `isTight` both ways, loose-then-`tighten(ROOT)` equivalence,
  reduction preserving reachability.
