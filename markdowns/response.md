# Response — on the plan's open points, and the four questions

Written 2026-09-16/17, answering a read of `ARCHITECTURE.md`. Records where the plan was wrong, what
was settled in discussion, and what stays open. Current phase: design the classes; bulk accessors in
every type (int, long, double) are boilerplate that comes later.

**Terms, as settled.** *Removal* unsigns an identity from the occupied set, nothing more. *Forget*
drops an identity's data and the connections structures hold for it, without renumbering. *Vacuum*
renumbers the living to close gaps — rare, at save/reload.

---

## Part 1 — the objections to the plan

### The core feels too empty

Thin is the goal, but not empty. Five jobs can only live in the core, because each is about the
components' relationship to each other:

1. **Registry** — the two lists, typed to contracts.
2. **Minting and the growth broadcast** — every structure over an arity follows in the same call.
3. **The sync barrier** — removal consequences settle there, for several components at once.
4. **Transaction boundary and journal** — one lock, one log.
5. **Cross-arity resolution / delegation** — only the core holds both lists.

**Structures hold arity slots — ints the core resolves — not arity objects.** The dependency then runs
one way and a structure stays readable alone; same decision already made for catalogs.

**Proxies: at the transaction boundary only.** Legacy's proxy failed on granularity — a reflective
call at every method entry. One proxy at `runAtomically` is one hop per transaction, amortised over
every mutation inside, and still keeps the concern in one place. Per-component-method proxies stay
refused.

### Removal, sync and vacuum

**Delegation belongs to the core; the barrier is when it runs.** No arity can route a removal to a
structure spanning three arities, and the barrier is the only moment the engine is quiescent. So
`sync()` is not a flush — it is the core's slot for every cross-component action unsound at any other
moment.

**Accumulating removals: a list, not a set.** Append is O(1); `register` on a positive set is a sorted
insert. Duplicates and already-gone identities are harmless because the occupied set is the authority
on existence — set semantics already live there. Where a set helps is the drain, for some structures
only: the DAG detaches per identity, a bitmap indexer wants `row.andNot(removed)`. So the list is
primary and a set view is derived lazily and shared — the pattern of legacy's `Materialized` (an
`IntArrayList` with a `BitSet` built on first probe).

**Sync as engine-wide forget.** If removal unsigns immediately, "pending" is just the difference
between the occupied set and what structures still hold. That extends an existing invariant —
*existence is the occupied set's question and only its question* — so a stale structure is not
inconsistent; it holds garbage. Consequences:

- **Idempotent and interruptible.** Twice is harmless; half now, half later is a legal state.
- **Incremental.** Bounded work per call keeps a UI from freezing on a large deletion.
- **Composes with `tighten`.** Reconcile, then tighten once.
- **Cost is the GC trade-off.** A pending list is a write barrier (cost per removal, collection ∝
  removals); a sweep is mark-sweep (free per removal, collection ∝ structure). Few removals favour
  the list, mass removal the sweep. Do both, switch on magnitude — same idiom as the `isTight` escape
  hatch.
- **Undo of a removal is nearly free until forget runs** — edges still exist, undo is a re-register.

**Where existence is checked.**

- *Index → identity through the occupied set:* no check needed; the set cannot yield a dead identity.
- *Following a reference into another arity* (multi-arity structures, and a few other cases): checked.

Rule: **a reference is validated when it leaves the structure holding it** — returned to a caller or
used to address another arity. Inside a structure's own walk, staleness is harmless. Alongside the
automatic rule, a manual method for a client that judges for itself; name it so skipping reads as a
decision (`identityAtUnchecked`, not a boolean flag).

Concerns raised, and where they landed:

- **Holes** — `contains` and `elementAt` both binary-search the exclusion list; stays fast in
  practice. The residual cost is memory: holes are stored individually, so a contiguous bulk removal
  (`truncate`) costs one entry per identity.
- **Counts** — not a structure's question; a structure is an add-on, never the authority. Dissolves.
- **Bulk catalog reads** — catalogs are total, so a dead identity's cell reads zero, not garbage.
  Intersect a range with the occupied set once, then read; never probe per element.
- **Prune-on-read — rejected.** One structure (e.g. an indexer) forgetting a dead identity alone when
  a read reaches it is harmful: it acts outside the barrier, mutates mid-traversal, and makes
  cheap undo depend on what someone happened to read. Acceptable variant: prune-on-write, when the
  structure is already being modified.

**Identity reuse is the load-bearing hazard.** A stale reference to a removed identity is detectable;
a stale reference to a *reused* identity silently points at a different live entity (ABA). And a
reused identity still holds the previous occupant's catalog values, since nothing clears them on
removal. So **forget must run before an identity is captured back.** Today the arity cannot tell it is
recycling: `ModularArityCatalog.truncate` sets `lastIndex = Math.max(capacity, -1)` and never touches
`occupied`. Fix: truncate unsigns what it drops, and a monotonic `everReached` (never lowered) lets
mint do `if(identity <= everReached) forget(identity)` unconditionally — enforced, not a caller
obligation. Without it the bug appears only on truncate-then-refill, rare enough to pass testing.

**Vacuum is the undo horizon — current solution.** Undo does not span a vacuum, matching Postgres
`VACUUM` and emptying a trash folder. Two notes kept for later:

- **Spanning is possible via a mapper interface**, not by editing journal records. In-place editing
  mutates history and pays for unreplayed records; a mapper is lazy, composes across vacuums (chain
  two), and is the same object an external consumer needs (e.g. a vector store keyed by identity).
  The mapping is rank/select over the dead set — `IdentitiesSetNegative*` already computes both sides
  (`elementAt` is position→identity, `exclusionSlot` is rank), so a vacuum keeping its pre-vacuum set
  can remap with existing code.
- **Restore ≠ undo.** Re-creating an entity with the same data under a new identity is restore, and
  should be named apart; undo returns the original identity. Conflating them hides dangling references.

### Persistence does not need the journal

The plan overreached. Snapshot and log answer different questions — ARIES has both. Over
structure-of-arrays a snapshot is close to a byte write; replaying a journal to the same state is
strictly more work. **Snapshot is the persistence path. The journal serves undo/redo, logging as a
reader, and optionally durability between snapshots** — so it can live in memory, bounded by the
undo depth or the last snapshot. Save/reload is also the natural moment to vacuum.

### Undo: plan, then act

Your idea, and correct. The plan phase consults the rules (acyclicity, tightness) and produces
primitive writes; the act phase consults nothing. Undo replays through act. The former state was
valid, so re-deriving rules on the way back is wasted and can be wrong (refusing a legitimate edge,
re-tightening differently). In database terms: physical over logical logging; ARIES keeps undo
logical only for concurrency, and there is a single writer here.

**Order stops mattering if primitives are absolute.** `addParent` under `isTight` undoing in *same*
order rather than reverse is a symptom of position-relative primitives — `removeInt(at)` shifts the
tail. Record slot, index, old, new: each write determines its own cell, so any order lands on the same
state. Falls out:

- **Redo symmetry answers itself** — undo writes old, redo writes new, same loop.
- **Abortable transactions and free dry-run** — refuse before the first write (e.g. a cycle).

**Caution:** neglecting rules must be a property of the act phase's shape, never a public flag like
`setTight(false)` that gets left off. The existing load-time escape hatch (clear flag, bulk load,
`tighten(ROOT)`) is a separate, legitimate client choice.

**Open:** you noted undo sometimes needs extra details — to be discussed.

### Logging modes: three, not two

| Mode | Recorded | When |
|---|---|---|
| Invert | absolute primitives | ordinary bounded edits |
| Snapshot | before-image of the region | one change touching many cells (`detach` on a wide node) |
| Note | only that it happened | derived state |

Slot 0's root-children list is the existing example. A structure should *declare* its derived state,
so the journal and snapshots skip it by construction and rebuild on load.

---

## Part 2 — the questions

### 1. Filtering

**Two kinds of leaf.** *Structural* leaves read shape without supplying meaning, so the engine ships
them: catalog comparisons (`eq ne lt lte gt gte between`) and reachability (`childOf parentOf
descendantOf ancestorOf siblingOf`). *Interpreting* leaves (`namedLike`, units, locale, similarity)
stay the client's. Same leaf interface; combinators do not know which is which.

**On the operators.**

- `CONTRADICTS` (disjoint?) and `IFOF` (equal?) look like **predicates returning boolean**, not set
  operations — already answerable via `intersectionCount == 0` and `containsAll` both ways. Separate
  them in the API. (If the meanings differ, the separation still applies to whichever do.)
- AND, OR, NOT plus a universe are complete; default the rest, as `IdentitiesSet` does.
- The universe is the occupied set. NOT over a negative-form set is a representation flip, not a scan.

**Restructuring from the tree alone**, beyond legacy's `isEnumeratePreferred()` + leftmost-wins (its
own javadoc: *"`And(broad, narrow)` drives off the broad side purely because it is leftmost"* —
confirmed in the code). Split into normalise → plan → execute; legacy fuses them in one recursive walk.

*Layer 0 — rewriting, no estimates:*

- **Negation normal form** (De Morgan). Interior NOT forces materialise-and-complement, the full scan
  the resolver avoids. `And(A, NOT B)` is `Subtract(A, B)`, so this subsumes Subtract.
- **Absorb NOT into leaves** via optional `negated()`: `NOT(x>5)` → `x<=5`. Reachability leaves
  usually can't, and say so.
- **Flatten to n-ary.** A binary tree encodes an order in its shape; this is what makes reordering
  possible at all.
- **Idempotence / annihilation / absorption**: `A∧A→A`, `A∧¬A→∅`, `A∨(A∧B)→A`. Legacy's records
  already give structural equality.
- **Interval subsumption per catalog**: `(x>5)∧(x>3)→(x>5)`, `(x>5)∧(x<3)→∅` with no data read.
  Possible only because the engine ships comparison leaves instead of opaque predicates.

*Layer 1 — ordering.* Replace the boolean with estimated size and cost-per-probe. AND drives off the
smallest estimate; probe the rest in ascending `cost / (1 − selectivity)` (Hellerstein & Stonebraker).
Needed because leaves differ on both axes (`namedLike`: selective, expensive; int range: unselective,
cheap). Estimates only need correct order: set `size()` is free; `ChildOf` is a child count;
`DescendantOf` wants cached subtree size; comparisons want min/max/count or a small histogram
(optional — unknown sorts last); `size(A∧B) ≈ min`, `size(A∨B) ≈ sum` capped at the universe.
`isEnumeratePreferred()` survives as a degenerate case.

*Layer 2 — push-down*, which legacy excluded on purpose. Thread the candidate set into leaves so they
produce only within it. Requires **gather** (read an arbitrary identity list) — the keystone of the
filtering work; build them together. Soundness: pushes through AND freely, through OR per branch, not
through NOT without care; wrong push-down over-matches silently.

*Layer 3 — representation.* All-Roaring operands: multi-way container intersection, ordered by
cardinality, beats driver-plus-probe. Ask "can all of you bulk together?" before picking a driver.

### 2. "Plugin" architecture

Agreed — Richards names microkernel and plug-in as one pattern (core plus plug-in modules; Eclipse is
the example). Don't write "the core is almost redundant", which invites scattering its five jobs.
Suggested: *"The core is thin by design: it holds the lists, mints identities, runs the barrier, owns
the transaction, and resolves arities for structures. Everything else is a plugin."*

### 3. `ArityComponent`, `StructureComponent`

Fixes the `java.lang.Module` collision. Caveat: in ECS (flecs, EnTT) a *component* is per-entity data
— a **catalog** here. So apply it to all three (`CatalogComponent` too) and add one line to `GOALS.md`
"The words, defined": *Component — a unit the core holds and drives through a lifecycle; not the ECS
sense, which this project calls a catalog.* That also closes the "Module — ambiguous" entry.
Alternative: no suffix (`Arity`, `Structure`, `Catalog`).

### 4. Client-supplied identity sets for computed arities

Works today, no contract change: `contains(i)` is `0 <= i && i < n`, `size()` is `n`, `identityAt(i)`
is `i` — stores nothing, inherits the algebra. Set-side twin of catalogs being interfaces so computed
values cost no memory.

- **Ship a dense-range set** — append-only arities are common; a zero-hole negative set carries hole
  machinery for nothing.
- **The perceptron weight arity is a product arity**: identity `i * outputs + j`, the arithmetic
  `LongChunkIntUniform` already does. Computed set plus computed index; worth naming if matrices are
  common.
- **Cautions:** `register`/`unsign` on a computed set must throw, not no-op. A free set does not mean
  free columns — catalogs still grow.
- **Test of the plugin claim:** if an unknown `IdentitiesSet` breaks anything, that names where the
  engine still assumes a shipped representation.

---

## Part 3 — file layout

`ontructure` becomes the live root (renamable; `kernel` suggested — ARCHITECTURE.md already says
"microkernel"; avoid `core`, spent by `legacy/core/` and `OntologyEngineCore`). Unwanted incubation
files stay where they are.

```
incubation/
  ApplicationData, OE_IntSet, OE_Catalog, IntSetLister*,      ← left untouched
  BidirectionalIndexingService, OntologyEngineCore
  ontructure/            (the core: registry, journal, barrier — at the root)
    sets/                IdentitiesSet and representations
    catalogs/            column contract, integer/, floating/, shared value vocabulary
    arity/               ArityComponent, ModularArityCatalog
    structures/          StructureComponent, plan type, graphs/, indexers/
    filters/             peer package — spans sets, catalogs and structures
```

- `structurer/` disappears, ending the structurer / structures / ontructure word collision.
- `sets/` and `catalogs/` leave the arity package: layers under it, and structures need `IdentitiesSet`.
- `PREFERRED_*`, `typeName` and the `exact*` conversions move off `ArityModule` into `catalogs/`;
  `CatalogModuleBase` re-declaring the constants goes away. `CatalogModuleBase` becomes top-level there.
- Listers move beside the chunked forms in `integer/` and `floating/`. As nested interface members
  they are implicitly public, despite looking package-private.
- The plan phase is a type (passed, inspected, discarded), beside the structure contract.
- `structures/indexers/` is empty, so untracked by git — give it a `package-info.java`.
- Delete `catalogs/LongIntCatalog` (unreferenced duplicate of `IntegerValueCatalog`).
- `ModularArityCatalog` is a table named "catalog" where catalog means column — rename candidate.

**Sets.** Byte and short widths are deleted; `IdentitiesSetPositiveInt` and `PositiveLong` are
written first, since positive exists only at the narrow widths today (details in `ARCHITECTURE.md`).
`firstIndex` is unneeded while the occupied set always starts at 0 — low priority.

---

## Sources

- [Software Architecture Patterns — microkernel / plug-in (O'Reilly)](https://www.oreilly.com/library/view/software-architecture-patterns/9781098134280/ch04.html)
- [ARIES: A Transaction Recovery Method Supporting Fine-Granularity Locking (Mohan et al.)](https://web.stanford.edu/class/cs345d-01/rl/aries.pdf)
- [ARIES: Logging and Recovery — UC Berkeley CS262a](https://people.eecs.berkeley.edu/~kubitron/courses/cs262a-F12/lectures/lec05-aries-rev.pdf)
- [Predicate Migration: Optimizing Queries with Expensive Predicates — Hellerstein & Stonebraker, SIGMOD 1993](https://dl.acm.org/doi/pdf/10.1145/170036.170078)
- [Optimization of Conjunctive Predicates for Main Memory Column Stores — VLDB](http://www.vldb.org/pvldb/vol9/p1125-kastrati.pdf)
