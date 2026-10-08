# Response — the plan's open points, and the four questions

Written 2026-09-16/17, rewritten in plain words 2026-09-18. Not a source: what was settled from it is
in `decisions.md`, `inferred-decisions.md` and `questions.md`.

**The three words, as settled.**

- **Removal** — unsign the identity in the occupied set. Nothing else happens.
- **Forget** — drop that identity's data, and the connections structures hold for it. No number moves.
- **Vacuum** — renumber the living identities to close the gaps. Rare: on storing to disk and reloading.

## Part 1 — the objections to the plan

### The core is thin, not empty

Five jobs can only live in the core, because each is about how the components relate to each other:

1. **The two lists** — arity components and structure components, held by contract.
2. **Minting, and telling everyone it grew** — one call, and every structure over that arity follows.
3. **The sync barrier** — the one moment nothing else is running, where the consequences of removal
   settle for several components at once. (*Barrier*: a point where deferred work must finish before
   anything continues.)
4. **The transaction boundary and the journal** — one lock, one list of what changed. (*Journal*: the
   single ordered list of changes; undo, redo and logging all read it instead of each keeping its own.)
5. **Cross-arity work and delegation** — only the core holds both lists.

- **A structure holds arity slots, not arity objects.** (*Slot*: a numbered place, used instead of a
  name or an object reference.) The dependency then runs one way, and a structure can be read on its
  own. Catalogs already work this way.
- **Proxies only at the transaction boundary.** (*Proxy*: an object that stands in front of another and
  runs something extra — a lock, a log line — before passing the call on.) Legacy put one at every
  method entry, which was the problem. One at `runAtomically` costs one extra hop per transaction, no
  matter how many changes happen inside it.

### Removal, sync, forget and vacuum

- **Delegation belongs to the core, and the barrier says when it runs.** No single arity can route a
  removal to a structure that spans three arities, and the barrier is the only quiet moment. So
  `sync()` is not a flush: it is the core's slot for every action that is unsafe at any other time.
- **Keep pending removals in an ordered list, not a set.** Appending is one step; registering into a
  positive set is a sorted insert. Duplicates and already-dead identities do no harm, because the
  occupied set is the only authority on existence.
  - Some structures want a set when draining: the DAG detaches one identity at a time, while a bitmap
    indexer wants `row.andNot(removed)`. Build that set from the list on first use and share it —
    legacy's `Materialized` does exactly this.
- **Sync is an engine-wide forget.** If removal unsigns immediately, "pending" is only the difference
  between the occupied set and what the structures still hold.
  - Running it twice is harmless, and stopping halfway leaves a legal state.
  - It can run in bounded pieces, so a large deletion does not freeze a UI.
  - It composes with `tighten`: settle first, tighten once.
  - Cost: a pending list pays per removal; a full sweep pays per structure size. Few removals favour
    the list, mass removal favours the sweep. Do both, and switch on the size — the same idea as the
    `isTight` escape hatch.
  - Undo of a removal is nearly free until forget runs, since the connections are still there.
- **Where existence is checked**
  - Index → identity through the identitiesSet: no check needed, the set cannot hand out a dead one.
  - Following a reference into another arity (multi-arity structures, and a few other cases): checked.
  - The rule: a reference is checked when it leaves the structure holding it — returned to a caller, or
    used to address another arity. Inside a structure's own walk, a dead identity does no harm.
  - Beside it, the judgment call method, for a client that decides for itself. Name it so that skipping
    the check looks deliberate (`identityAtUnchecked`), rather than making it a boolean flag.
- **The concerns, and where they landed**
  - *Holes* — `contains` and `elementAt` both binary-search the hole list, and stay fast in practice.
    What it does cost is memory: each hole is stored on its own, so a contiguous `truncate` costs one
    entry per identity.
  - *Counts* — not a structure's question. A structure is an add-on, never the authority.
  - *Bulk catalog reads* — catalogs answer for every identity, so a dead one reads zero rather than
    garbage. Intersect the range with the occupied set once, then read; never check per element.
  - *Cleaning up on read — rejected.* One indexer forgetting a dead identity by itself, because a read
    reached it, acts outside the barrier, changes data in the middle of a walk, and makes cheap undo
    depend on what someone happened to read. Cleaning up while already writing is fine.
- **Capturing an identity back is the dangerous case.** A reference to a removed identity can be
  caught; a reference to a *reused* identity silently points at a different live entity. The reused
  identity also still holds the old values, because nothing clears them on removal. So forget must run
  before an identity is captured back.
  - Today the arity cannot tell that it is reusing a number: `ModularArityCatalog.truncate` lowers
    `lastIndex` and never touches `occupied`. Fix: truncate unsigns what it drops, and the arity keeps
    a number that never goes down (`everReached`), so minting can do
    `if(identity <= everReached) forget(identity)` without the caller having to remember.
- **Vacuum is the undo horizon.** Undo does not reach past a vacuum. Two notes for later:
  - Reaching past it needs a mapper, not edits inside the journal. Editing rewrites history and pays
    for records nobody will replay; a mapper is computed when asked, chains across two vacuums, and is
    the same object an outside consumer needs (a vector store keyed by identity). The mapping is "how
    many dead identities are below this number", which `IdentitiesSetNegative*` already computes both
    ways (`elementAt`, `exclusionSlot`).
  - Restore is not undo. Re-creating the same data under a new number is restore; undo gives the
    original number back. Keeping the names apart keeps dangling references visible.

### Persistence does not need the journal

- A snapshot and a journal answer different questions, and databases keep both.
- The data is already columns, so writing a snapshot is close to a plain byte write, while replaying a
  journal to reach the same state is strictly more work.
- So: the snapshot is the persistence path. The journal serves undo/redo, logging reads it, and it can
  optionally cover durability between snapshots — which lets it live in memory, bounded by the undo
  depth or by the last snapshot.
- Storing and reloading is the natural moment to vacuum.

### Undo: plan, then act

- Your idea, and it is right. The plan phase applies the rules (no cycles, tightness) and produces
  plain writes; the act phase applies no rules. Undo replays through act.
- The former state was valid, so applying the rules on the way back is wasted work, and can be wrong:
  refusing an edge that was legitimate, or tightening differently.
- **Order stops mattering once the writes are absolute.** `addParent` under `isTight` needing the
  *same* order rather than the reverse is a symptom of writes that are relative to a position —
  `removeInt(at)` shifts everything after it. Record slot, index, old value and new value, and each
  write lands in its own cell, so any order reaches the same state.
  - Redo then answers itself: undo writes the old value, redo writes the new one, the same loop.
  - A transaction can be refused before its first write (a cycle, say), which also gives a free dry run.
- **Careful:** ignoring the rules has to be a property of the act phase itself, never a public flag
  like `setTight(false)` that someone leaves switched off. The load-time escape hatch (clear the flag,
  bulk load, `tighten(ROOT)`) is a separate and legitimate client choice.
- **Open:** you said undo sometimes needs extra details — still to be explained.

### Logging modes: three, not two

| Mode | What the journal keeps | When |
|---|---|---|
| Invert | the absolute writes | ordinary edits of bounded size |
| Snapshot | a copy of the region before the change | one change touching many cells (`detach` on a wide node) |
| Note | only that it happened | data that can be rebuilt |

- Slot 0's root-children list is the example that already exists. A structure should declare which of
  its state is rebuildable, so the journal and the snapshot skip it and rebuild it on load.

## Part 2 — the four questions

### 1. Filtering

- **Two kinds of leaf.**
  - *Structural* leaves read shape and supply no meaning, so the engine ships them: catalog comparisons
    (`eq ne lt lte gt gte between`) and reachability (`childOf parentOf descendantOf ancestorOf
    siblingOf`).
  - *Interpreting* leaves stay the client's: `namedLike`, units, locale, similarity.
  - Same leaf interface; the combinators do not know which kind they are holding.
- **On the operators**
  - `CONTRADICTS` (disjoint?) and `IFOF` (equal?) look like questions answered true or false, not set
    operations — `intersectionCount == 0`, and `containsAll` both ways. Keep them apart in the API.
  - AND, OR, NOT plus a universe are enough; everything else can default to them, as `IdentitiesSet`
    already does.
  - The universe is the occupied set. NOT over a negative-form set flips the representation instead of
    scanning.
- **Restructuring the tree by looking at it**, beyond legacy's `isEnumeratePreferred()` and
  leftmost-wins — its own javadoc admits `And(broad, narrow)` drives off the broad side only because it
  is leftmost. Split the work into three steps, where legacy does all of it in one recursive walk.
  - *Rewriting, with no measurements.*
    - Push NOT down to the leaves. A NOT in the middle forces building the whole set and complementing
      it, which is the scan the resolver exists to avoid. `And(A, NOT B)` becomes `Subtract(A, B)`, so
      Subtract needs no separate handling.
    - Absorb NOT into a leaf where the leaf can take it: `NOT(x > 5)` becomes `x <= 5`. Reachability
      leaves usually cannot, and should say so.
    - Flatten to one AND over many children instead of nested pairs. A binary tree hides an order in
      its shape, and flattening is what makes reordering possible at all.
    - Simple laws: `A∧A→A`, `A∧¬A→empty`, `A∨(A∧B)→A`. Legacy's records already compare structurally.
    - Merge ranges on the same catalog: `(x>5)∧(x>3)→(x>5)`, `(x>5)∧(x<3)→empty`, with no data read.
      Possible only because the engine ships the comparison leaves instead of opaque client predicates.
  - *Ordering.* Replace the boolean with two estimates: how many identities a leaf yields, and what one
    check on it costs. AND drives off the smallest; the rest are checked cheapest-first, by cost
    divided by how much the leaf cuts down. Needed because leaves differ on both counts — `namedLike`
    cuts a lot and costs a lot, an int range cuts little and costs little. The estimates only need to
    be in the right order: a set's `size()` is free, `childOf` is a child count, `descendantOf` wants a
    cached subtree size, comparisons want min/max/count. `isEnumeratePreferred()` survives as the
    simplest case of this.
  - *Pushing the candidate set down*, which legacy left out on purpose. Hand the candidates into the
    leaf, so it only produces identities inside them. This needs **gather** — reading the values for a
    scattered list of identities rather than a contiguous range — which makes gather the keystone of
    the filtering work. It pushes through AND freely, through OR per branch, and through NOT only with
    care; pushing it wrongly over-matches silently.
  - *Representation.* When every operand is a Roaring set, intersecting them as bitmaps, smallest
    first, beats driving off one and probing the others. Ask "can all of you work in bulk?" before
    picking a driver.

### 2. "Plugin" architecture

- Agreed, and it is one named pattern: microkernel, also called plug-in — a small core plus plug-in
  modules, with Eclipse as the example.
- I would not write "the core is almost redundant" in the documentation, because it invites scattering
  the five jobs. Suggested wording: *the core is thin by design — it holds the lists, mints identities,
  runs the barrier, owns the transaction, and resolves arities for structures. Everything else is a
  plugin.*

### 3. `ArityComponent`, `StructureComponent`

- Fixes the collision with `java.lang.Module`.
- Caveat: in ECS (Entity Component System — the game-engine pattern of bare ids with their data in
  columns) a *component* is the per-entity data, which is a **catalog** here. So either apply the word
  to all three (`CatalogComponent` too) and define it once in `inferred-decisions.md` → Words, or use
  no suffix at all (`Arity`, `Structure`, `Catalog`).

### 4. Client-supplied identity sets for computed arities

- Works today with no change to the contract: `contains(i)` is `0 <= i && i < n`, `size()` is `n`, and
  `identityAt(i)` is `i`. It stores nothing and inherits the whole algebra. It is the set-side twin of
  catalogs being interfaces, so computed values cost no memory.
- Ship a dense-range set: append-only arities are common, and a negative set with no holes carries the
  hole machinery for nothing.
- The perceptron weight arity is a product of two arities: identity `i * outputs + j`, the same
  arithmetic `LongChunkIntUniform` already does. A computed set plus a computed index; worth naming if
  matrices turn out to be common.
- Cautions: `register` and `unsign` on a computed set must throw rather than do nothing, and a free set
  does not mean free columns — the catalogs still grow.
- It is also the test of the plugin claim: if an unknown `IdentitiesSet` breaks something, that names
  the place where the engine still assumes one of its own.

## Part 3 — file layout

`ontructure` becomes the live root (renamable; `kernel` suggested — avoid `core`, which
`legacy/core/` and `OntologyEngineCore` already use). Unwanted incubation files stay where they are.

```
incubation/
  ApplicationData, OE_IntSet, OE_Catalog, IntSetLister*,      ← left untouched
  BidirectionalIndexingService, OntologyEngineCore
  ontructure/            (the core: the two lists, journal, barrier — at the root)
    sets/                IdentitiesSet and its representations
    catalogs/            column contract, integer/, floating/, shared value vocabulary
    arity/               ArityComponent, ModularArityCatalog
    structures/          StructureComponent, the plan type, graphs/, indexers/
    filters/             a peer package — it spans sets, catalogs and structures
```

- `structurer/` disappears, which ends the structurer / structures / ontructure collision.
- `sets/` and `catalogs/` leave the arity package: they are layers under it, and structures need
  `IdentitiesSet` too.
- `PREFERRED_*`, `typeName` and the `exact*` conversions move off `ACSE_ArityComponentBase` into `catalogs/`, and
  `CatalogModuleBase` stops re-declaring the constants and becomes top-level there.
- The listers move beside the chunked forms in `integer/` and `floating/`. As nested interface members
  they are implicitly public, despite looking package-private.
- The plan phase is a type — passed, inspected, discarded — beside the structure contract.
- `structures/indexers/` is empty, so git does not track it; give it a `package-info.java`.
- Delete `catalogs/LongIntCatalog`, an unreferenced duplicate of `IntegerValueCatalog`.
- `ACSE_ModularArityCatalog` is a table named "catalog", where catalog means column — rename candidate.

**Sets.** The byte and short widths are deleted, and `IdentitiesSetPositiveInt` and `PositiveLong` are
written first, since positive exists only at the narrow widths today. `firstIndex` is not needed while
the occupied set always starts at 0 — low priority.

## Sources

- [Software Architecture Patterns — microkernel / plug-in (O'Reilly)](https://www.oreilly.com/library/view/software-architecture-patterns/9781098134280/ch04.html)
- [ARIES: A Transaction Recovery Method Supporting Fine-Granularity Locking (Mohan et al.)](https://web.stanford.edu/class/cs345d-01/rl/aries.pdf)
- [ARIES: Logging and Recovery — UC Berkeley CS262a](https://people.eecs.berkeley.edu/~kubitron/courses/cs262a-F12/lectures/lec05-aries-rev.pdf)
- [Predicate Migration: Optimizing Queries with Expensive Predicates — Hellerstein & Stonebraker, SIGMOD 1993](https://dl.acm.org/doi/pdf/10.1145/170036.170078)
- [Optimization of Conjunctive Predicates for Main Memory Column Stores — VLDB](http://www.vldb.org/pvldb/vol9/p1125-kastrati.pdf)
