# Response — on the plan's open points, and the four questions

Written 2026-09-16, answering a read of `ARCHITECTURE.md`. Opinions, not decisions. Where I disagree
with the plan I say so; where the plan was wrong I say that rather than defending it.

---

## Part 1 — the objections to the plan

### "The core knows lifecycle, not meaning" — the core feels too empty

An empty core is the success condition of this architecture, so the feeling is expected. But *empty*
and *thin* are different, and the plan undersold what is left. Five jobs cannot be pushed out to the
components, because each of them is about the components' relationship to each other:

1. **The registry.** The two lists, typed to contracts. Only the core knows what exists.
2. **Identity minting and the growth broadcast.** The arity mints; the core is what makes every
   structure over that arity follow in the same call.
3. **The sync barrier.** Deferred removal has to be drained *somewhere*, and it cannot be any one
   component's business because the point of deferring is that several of them settle at once.
4. **The transaction boundary and the journal.** One lock, one log, one place policy lives.
5. **Cross-arity resolution.** A structure names the arities it is over; something has to turn that
   name into an arity. Only the core holds both lists.

Your instinct that *delegation-like actions belong in the core* is item 5, and it is the one that
most deserves the name "core". A binary structure between arity 2 and arity 7 cannot resolve either
one itself without holding references that make the two packages mutually dependent. If the structure
holds **arity slots — ints the core resolves** — then the dependency runs one way, the structure stays
readable on its own, and the resolution is a lookup the core does once per operation rather than a
pointer each structure carries. That is the same decision already made for catalogs, which are
addressed by slot and not by name or reference.

**On the core holding proxies: partly agree, with one distinction that decides it.** The plan killed
proxies for a reason that is about *granularity*, not about proxies as such — a reflective
`InvocationHandler` on every segment puts a reflective call at every entry point, on the path this
engine spent its design avoiding. That objection is fatal to a per-method proxy and says nothing
about a per-transaction one.

So: a proxy at the `runAtomically` boundary is one reflective hop per transaction, amortised over
every mutation inside it, and it buys you the same thing AOP was buying — the concern stays in one
place. A proxy per component method is the thing to keep refusing. If the core grows a proxy, it
should wrap the boundary, not the components.

### "Synchronisation is direct for growth, deferred for removal" — the holes matter

Agreed, and I think the holes are more load-bearing than the plan noticed.

**Correction, after your push-back: the accumulation should be a list.** I over-generalised. An append
to a `LongArrayList` is amortised O(1) with no hashing, no shifting and no container logic, where
`register` on a positive lister is a sorted insert and on Roaring is container work. The pending
collection is pure write-path overhead — nothing reads it until the drain — so the cheapest possible
accumulation is the right one.

The part I missed is your reason it is *safe*: duplicates and already-gone identities cost nothing,
because the occupied set is the authority on existence and a structure that asks it gets the right
answer however many times an identity appears in the list. The list does not need set semantics
because the set semantics already live somewhere else, and paying for them twice is the mistake.

Where a set is still wanted is the drain, and only for some structures. The DAG detaches each removed
element from its parents and children — irreducibly per identity, and set algebra buys it nothing. A
bitmap-backed indexer clearing rows wants `row.andNot(removed)`, which is container-wise and far
faster than per-identity bit clears. So the representation should not be chosen once in the core.

**The resolution is a pattern already in the repository.** `OntologyFilterResolverSegmentation`'s
`Materialized` holds an `IntArrayList` and derives a `BitSet` on first probe, because the list is what
was produced and the index is what only some consumers need. Pending removals want exactly that: the
list is primary and free, a set view is built lazily on first request, and every structure that asks
afterwards shares it. One conversion, amortised over all consumers, paid only if someone wants it.

#### Sync as an engine-wide vacuum

This is the better idea, and I think it supersedes the pending list rather than decorating it.

If removal unsigns from the occupied set immediately, there is no pending collection at all. What is
"pending" is just the difference between what the occupied set says and what the structures still
hold, and `sync()` becomes a **vacuum**: find what the structures hold that no longer exists, and drop
it. That is the Postgres sense of the word, and giving the operation a name people already understand
is worth something for a plugin API.

It fits an invariant you have already committed to. *"Partiality lives in exactly one place"* — a
catalog is total, existence is the occupied set's question **and only its question**. Extended to
structures, that says a structure holding a reference to a removed identity is not in an inconsistent
state, because it was never the authority on existence. Staleness stops being a bug and becomes
garbage, which is a much weaker thing to have to clean up and can be cleaned up whenever convenient.

Three consequences worth having deliberately:

- **It is idempotent and interruptible.** Vacuuming twice is harmless, and vacuuming half the
  structures now and the rest later is a legal state rather than a broken one. A pending-list drain
  needs bookkeeping to avoid double-draining; a vacuum needs none.
- **It can be incremental.** Bounded work per call — one structure, or N slots — which is what keeps a
  file manager's UI from freezing on a large deletion. Postgres autovacuum is exactly this.
- **It composes with `tighten`.** Reconcile, then tighten once, which is what the plan wanted anyway.

**The cost model is the one real objection, and it is the garbage-collection trade-off exactly.** A
pending list is a write barrier with a remembered set: it costs on every mutation and makes collection
proportional to the *removals*. A vacuum is a mark-sweep: it costs nothing on mutation and makes
collection proportional to the *structure*. Removing three identities from a graph of ten million,
the list wins overwhelmingly; removing four million, the vacuum wins and the list was wasted work.

Every mature collector resolves this the same way — do both, and choose by magnitude. Accumulate the
list while removals are few; past some fraction of the structure, stop accumulating, mark the
structure dirty and vacuum it wholesale. You already have this idiom in the codebase: the `isTight`
escape hatch is *"past a certain volume, stop doing this incrementally and do it once at the end"*,
which is the same decision under a different name.

**Two things that fall out that I do not think the plan noticed.**

*Undo of a removal becomes nearly free — until the vacuum.* If removal is only an unsign, undoing it
before the vacuum runs is re-registering the identity: the structures still hold every edge, because
nothing has dropped them yet. After the vacuum it is expensive, and the journal has to have recorded
what was dropped. So the vacuum is not only a cleanup point, it is **the boundary at which cheap undo
becomes expensive undo** — which is a good argument for it being a deliberate, scheduled operation
rather than something that happens opportunistically, and for it never running inside an open
transaction.

*Lazy vacuum is sound only because identities are never reused — and `truncate` currently breaks
that.* This is the load-bearing dependency and it is invisible, so it should be written down. A
structure holding identity 42 after 42 is removed holds a reference to something that does not exist,
which the occupied set can detect. If identities were ever reused, that same stale edge would point at
a **different, live entity** and nothing could detect it — the ABA problem, and lazy vacuum would be
silently wrong rather than merely late. `GOALS.md` already lists dense, non-reused identities as a
load-bearing assumption; this is a second thing that breaks if it stops holding, and a sharper one
than the storage-width argument, because the failure is a wrong answer rather than a memory cost.

The hole is in `truncate`. `ModularArityCatalog.truncate` lowers every column and sets
`lastIndex = Math.max(capacity, -1)`, so the next mint reuses identities in the freed range — and it
never touches `occupied`, so between the truncate and whatever the caller does next, the occupied set
still claims identities the columns no longer address, which contradicts totality. Either it is an
undocumented caller obligation or it is a gap, but on the vacuum design it is specifically the one
path where staleness can become a wrong answer. The fix is small: truncate unsigns the range it drops,
and forces a vacuum before anything is minted back into it.

#### Settled in discussion

**Vacuum is the undo horizon.** Undo does not span a vacuum. This is what comparable systems do —
Postgres `VACUUM` does not roll back, emptying a trash folder is not undoable — so it matches what a
user already expects, and it costs nothing to implement because it is a refusal rather than a
mechanism.

What that leaves is **restore**, which is a different feature and should be named apart from undo.
Restore re-creates an entity with the same data under a *new* identity, and is honest about it. Undo
returns the original identity. Conflating them is where dangling references come from; separating
them makes the difference visible to the client instead of surprising.

**If undo ever does need to span a vacuum, the remap is not a table.** Compaction shifts every
survivor down by the number of dead identities below it, so the mapping is rank and select over the
dead set: one direction is `oldId − holesBelow(oldId)`, the other is the n-th live identity. Both are
already computed in `IdentitiesSetNegative*` — `elementAt(i)` returns `firstIndex + i + low` where
`low` is the holes at or below that position, which is exactly position→identity, and `exclusionSlot`
is the rank side. So a vacuum that keeps its pre-vacuum occupied set can remap with the class that
already exists, storing one set rather than a per-reference table.

**The remap is delivered as a mapper, not by editing the journal.** Rewriting identities inside the
journal in place would mutate history while it is being held, which is the one thing a log must not
do; it also pays for every record whether or not it is ever replayed. A mapper is a pure function
handed to the replay, and it has three properties the in-place edit does not: it is lazy, so only
records actually replayed are translated; it composes, so two vacuums are two mappers chained rather
than a second rewrite of already-rewritten data; and it is the same interface an *external* consumer
needs — a vector store keyed by identity has exactly the same problem as the journal and can be
handed exactly the same object.

**Vacuum and compact are two operations, and only one of them is routine.**

- **`forget(identity)`** — drop the identity's own data and the connections structures hold for it.
  No renumbering. This is the routine one, it is incremental, and it needs no mapper because nothing
  moves.
- **`compact()`** — renumber the living to close the gaps. Rare, and externally visible: anything
  that persisted an identity outside the engine is invalidated unless it is handed the mapper. Save
  and reload is the natural place for it, since that pass touches everything anyway and hands back a
  collapsed hole list and a restored memory profile.

**Capture-back must be enforced in the mint path.** A recycled identity still holds the previous
occupant's values, because catalogs are total and nothing clears them on removal — so `forget` has to
run before the number is handed out again. The arity cannot currently tell that it is recycling:
`lastIndex` is the only high-water mark and `truncate` lowers it, so after `truncate(500)` the mint of
501 is indistinguishable from a fresh one. One monotonic field — `everReached`, never lowered — lets
the mint ask and forget unconditionally, which turns a caller obligation into something that cannot be
skipped. Without it the bug fires only on truncate-then-refill, which is rare enough to survive
testing.

**Prune-on-read is rejected.** Dropping a dead edge at the moment a read discovers it looks free, but
it destroys the cheap-undo window unpredictably — whether an undo is cheap would depend on which
folder someone happened to open — and it mutates a structure mid-traversal. What survives is
prune-on-write: when a structure is already being modified for another reason, dead entries it passes
can go, since the row is being touched anyway and no reader is surprised.

**The delegation belongs to the core, and the barrier is when it runs.** Those compose rather than
compete. The core owns the drain because it is the only thing holding both lists: no arity can route
a removal to a structure spanning three arities, and no structure can be asked to notice a removal it
was never told about. And the barrier is the only moment the whole engine is quiescent, which is what
makes that routing safe to do there and unsafe anywhere else — growth is direct because a new identity
cannot invalidate anything, removal is deferred because it can.

That makes `sync()` a larger piece of the design than the plan treated it as. It is not a flush; it is
the core's scheduled opportunity to do every cross-component thing that is unsound at any other
moment, and the drain is one instance of that rather than the whole of it.

### "Cross-cutting concerns are a journal" — persistence does not need the journal

**You are right and the plan overreached.** "Persistence — the same records, which collapses two
subsystems into one" was a tidy sentence that costs real performance.

Snapshot and log are different mechanisms answering different questions, and ARIES — the source the
plan borrowed "physiological" from — has both: checkpoints *and* a log, because neither replaces the
other. A full save to file is a snapshot. Your storage is structure-of-arrays over primitive backing
arrays, so a snapshot is very close to writing each chunk out as bytes — about the cheapest
persistence a layout can have. Reconstructing that same state by replaying a journal is strictly more
work, strictly more code, and strictly more that can go wrong.

What the journal is actually for is narrower than the plan claimed:

- **Undo/redo** — genuinely needs it. Nothing else reconstructs "the state before this operation".
- **Logging** — a reader on it, free.
- **Durability between snapshots** — optional, and only if you ever want crash recovery rather than
  explicit save. For a personal file manager, "save on exit and every N minutes" may be the whole
  requirement, and then the journal never touches disk at all.

So the correction: **snapshot is the primary persistence path; the journal is an undo/redo structure
that can optionally be made durable.** That also means the journal can live in memory with a bounded
size, which removes a whole class of problem — you are not obliged to keep every record forever, only
back to the last snapshot or the depth of the undo stack, whichever you choose.

### "Undo is by decomposition" — your plan/act idea

This is the best idea in your message and I think it is correct. Let me restate it, then sharpen one
part, because I believe the order problem you hit dissolves rather than needing to be worked around.

**The idea.** A structure separates *planning* a change from *applying* it. The plan phase consults
the rules — acyclicity, tightness, whatever the structure enforces — and produces a list of
primitive writes. The act phase consults nothing and performs them. The journal records the
primitives. Undo replays them through the act phase, which has no rules to neglect because it never
had access to them.

**Why it is right.** The invariant is a *forward* concern. Undo returns to a state that was already
valid, so re-deriving the invariant on the way back is both wasted work and actively wrong — it can
refuse an edge that legitimately existed, or re-tighten to a different result than the one that was
there. Your sentence "since the former state was valid the inner steps don't matter" is the whole
justification and it holds.

This is the policy/mechanism split, and in database terms it is the choice of **physical logging over
logical logging**. Logical logging records the operation and replays it (rules and all); physical
logging records the cells and restores them. ARIES makes redo physical for exactly this reason — the
redo pass repeats history without needing to know transaction status — and keeps undo logical only
because it has concurrency to worry about. You have a single writer, so you can have both halves
physical, which is simpler than what ARIES needs.

**The sharpening.** Your observation that `addParent` under `isTight` undoes in the *same* order
rather than reverse order is a symptom, and the disease is that the primitives are position-relative.
`removeParent` does `removeInt(at)`, which shifts the tail; "insert a parent at an index" means a
different thing depending on what happened before it. That is what makes order significant.

Record the primitives **absolutely** — slot, index, old value, new value — and order stops mattering
at all. Reverse, forward, or shuffled, applying a set of absolute cell writes lands on the same
state, because each one fully determines its own cell. The question "which order do I undo in" then
has no answer because it has no content. `ARCHITECTURE.md` already says "record the position, not
just the identity" for a narrower reason; this is the same fix taken one step further, and it is what
makes the plan/act split pay off.

Two things fall out for free:

- **Redo symmetry — your open question — answers itself.** A record carrying both old and new is
  undone by writing old and redone by writing new. Undo and redo become the same loop run in
  opposite directions over the same records, and redo is as dumb as undo. The cost is a few more ints
  per record, which against an `IntArrayList` journal is nothing.
- **Transactions become abortable, and validation becomes free.** If the plan phase produces the
  primitive list without touching anything, you can refuse the whole operation before the first write
  — which is what you want for an edge that would create a cycle, and what you want for a client that
  asked for something illegal. You also get dry-run for nothing: plan, inspect, discard.

**One caution.** "Neglect the rules temporarily" must not become a flag. If there is a public
`setTight(false)` that clients toggle around their own edits, it will be left off, and the structure
will hold state its own invariant says is impossible. In the plan/act split it is not a flag at all —
the act phase structurally has no access to the rules, so there is nothing to forget to turn back on.
Keep the temporariness a consequence of the shape rather than a thing anyone has to remember.

(The existing escape hatch — clear the flag, bulk load, `tighten(ROOT)` once — is a different and
legitimate thing: it is a *client* choosing to defer a rule at load time, not the undo path evading
one. Worth keeping, worth keeping separate.)

### "Reversible and irreversible are both allowed" — sometimes just note it

Agreed, and it makes a third mode rather than a variation on the second. The taxonomy:

| Mode | What is recorded | When it is right |
|---|---|---|
| **Invert** | the primitives, absolutely | ordinary edits; small, bounded change |
| **Snapshot** | a before-image of the affected region | one change touching very many cells — `detach` on a wide node |
| **Invalidate** | that it happened, and nothing else | **derived state** |

The third is the one you are pointing at, and you already have an instance of it: slot 0's
root-children list is derived, self-heals through its dirty flag, and journalling it would be
recording something that can be recomputed from what is already recorded. The general rule is that
**derived state is journalled as an invalidation, never as data** — and the way to make that reliable
is for a structure to *declare* which of its state is derived, so the journal skips it by
construction rather than because someone remembered to.

That declaration is also what makes persistence cheaper, since a snapshot should skip derived state
for the same reason and then rebuild it on load.

---

## Part 2 — the questions

### 1. The filtering service

The split you have is the right one and I would not collapse it. The engine supplies no
interpretation, so it cannot supply `namedLike`. But "the client supplies all data-dependent filters"
is giving away more than you have to, because **not every data-dependent filter is an interpreting
one.**

Two kinds of leaf:

- **Structural leaves — the engine can and should ship these.** Comparison against a value on a
  catalog (`eq`, `ne`, `lt`, `lte`, `gt`, `gte`, `between`) is meaningful over any integral or
  floating catalog *without knowing what the values mean*. Ordering is a property of the number, not
  of the interpretation. Likewise reachability on a structure — `childOf`, `parentOf`, `descendantOf`,
  `ancestorOf`, `siblingOf` — is meaningful over any hierarchy without knowing what the hierarchy
  classifies. Neither supplies meaning; they read the shape that is already there.
- **Interpreting leaves — the client's, permanently.** `namedLike` needs a string catalog *and* a
  matching rule, and both are the client's. Same for anything involving units, locale, or a domain
  notion of similarity.

Both implement the same leaf interface, and the combinators do not know which is which. That is the
plugin contract doing real work, and it is a much smaller thing to ask of a client than "write your
own filters": they write only the ones that genuinely encode their meaning.

**On the operator list, three notes.**

*It is not all one return type.* `AND`, `OR`, `NOT`, `XOR`, `SUBTRACT`, `NAND`, `NOR`, `IMPLIES`,
`NIMPLIES` all produce sets. But if `CONTRADICTS` means "these two sets are disjoint" and `IFOF` means
"these two sets are equal", those are **predicates returning a boolean**, not set operations — and
both are already answerable on `IdentitiesSet` today (`intersectionCount(other) == 0`, and
`containsAll` both ways). Worth separating them in the service, because a client who expects a set and
gets a boolean has hit an API seam rather than a filter. If I have guessed either meaning wrong, the
separation still applies — just to a different pair.

*Most of them are derived.* `AND`, `OR`, `NOT` plus a universe is complete; everything else follows.
Implement three and default the rest, which is exactly the idiom `IdentitiesSet` already uses — supply
`contains` and `identityAt`, inherit the algebra. A new filter representation should gain the whole
operator set the same way.

*The complementing operators need a universe,* and it is the occupied set — finite, known, and
already the authority on existence. Worth noting the fit: **your negative representation is a
complement form, so `NOT` on it is a representation flip rather than a scan.** A `NOT` over a dense
arity of a billion is three fields and the hole list changing places, which is the kind of thing that
justifies having built the negative form at all.

**The performance shape, which matters more than the operator list.** A naive combinator tree
materialises both sides and then intersects. Do not — instead order the leaves by selectivity and
evaluate cheapest-first, carrying the survivors forward as a candidate list. You already have the two
things that make this possible without materialising anything: `size()` is O(1)-ish on every
representation, and `intersectionCount` answers overlap without building the result.

That candidate list is **exactly the gather that `ARCHITECTURE.md` lists as a known gap** — a read of
an arbitrary identity list rather than a contiguous range. The filtering service is the use case that
justifies building it, and I would treat the two as one piece of work rather than two. Without gather,
every non-leading leaf in a filter chain reads ranges it has already excluded.

#### What can be restructured by looking at the tree alone

Legacy's answer is `isEnumeratePreferred()` plus leftmost-wins, and its own javadoc names the cost:
*"there is deliberately no cost estimation, so `And(broad, narrow)` drives off the broad side purely
because it is leftmost."* That is a real weakness and it is fixable, in three layers that are worth
keeping separate because the first needs no estimates at all.

**Layer 0 — algebraic rewriting, no cost model.** These are pure syntax and they pay before anything
is measured.

- **Negation normal form.** Push `NOT` down to the leaves by De Morgan, cancelling double negations on
  the way. This matters more than it looks: an interior `NOT` forces its subtree to be materialised and
  then complemented against the universe, which is the exact full scan the whole resolver is built to
  avoid. At a leaf it usually costs nothing. Note that this subsumes legacy's `Subtract` — `And(A,
  NOT B)` *is* `Subtract(A, B)`, and legacy's "drive off `kept`" is one hand-rolled instance of the
  general rule.
- **Absorb negation into the leaf.** `NOT (x > 5)` is `x <= 5` — same cost, no complement at all. Give
  the leaf interface an optional `negated()` that returns a leaf or nothing; comparison leaves invert
  exactly, reachability leaves mostly cannot and honestly say so.
- **Flatten to n-ary nodes.** `And(And(a, b), c)` → `And(a, b, c)`. This is the change that unlocks
  every later one: a binary tree *encodes* an order in its shape, which is precisely why legacy is
  stuck with leftmost-wins. It cannot reorder what its own representation has already committed to.
- **Idempotence, annihilation, absorption.** `A∧A→A`, `A∧¬A→∅`, `A∨(A∧B)→A`. Cheap to detect, and
  they delete whole subtrees. Legacy already has the machinery for this without knowing it — the AST
  is records, so structurally equal nodes are equal, which is what its memo relies on. The same
  equality drives the rewrite.
- **Interval subsumption on one catalog.** `(x>5)∧(x>3)` → `(x>5)`; `(x>5)∧(x<3)` → `∅` with no data
  touched at all. Group the conjuncts by catalog slot and intersect their intervals. This is the one
  that a generic boolean optimiser cannot do and you can, and the reason is the decision in the
  section above: because the engine *ships* the comparison leaves, it can reason about them instead of
  treating them as opaque client predicates. Legacy could never do this — its leaves were all
  hierarchy-shaped.

**Layer 1 — ordering, which needs a ranking rather than accurate estimates.** Replace the one boolean
with two numbers per node: estimated result size, and cost per probe. Then `AND` drives off the
smallest estimate and probes the rest, instead of driving off whatever happened to be written first.

The optimal order for the probed remainder is known: ascending `cost / (1 − selectivity)`, the
conditional-rank result from Hellerstein and Stonebraker's predicate migration work. It matters here
because your leaves genuinely differ in both dimensions at once — a `namedLike` over a string catalog
is selective but expensive per candidate, a range check on an int column is unselective but nearly
free, and a single boolean cannot express that difference.

The estimates can be crude, because only the *order* has to be right:

- a set-backed leaf already answers `size()` in O(1) — exact and free;
- `ChildOf` is the parent's child count, one read; `DescendantOf` wants a cached subtree size;
- a comparison leaf needs per-catalog statistics — min, max, count, or a small equi-depth histogram.
  This is the only place that needs new machinery, and it is optional: a leaf that cannot estimate
  says so and sorts last;
- for the combinators, `size(A∧B) ≈ min` and `size(A∨B) ≈ sum, capped at the universe` are enough.

Note this is not a repudiation of legacy's idea but a widening of it: `isEnumeratePreferred()` is
recoverable as the degenerate case "my enumerate cost is finite and my probe cost is high". One bit
becomes two numbers, and leftmost-wins disappears because there is now something better to sort by.

**Layer 2 — restriction push-down, which legacy excluded on purpose.** Its javadoc is explicit:
*"no candidate set is threaded down the recursion. Narrowing happens by filtering a driver's result."*
That is the conservative choice and it costs exactly what you would expect — a leaf beneath an `AND`
materialises its whole domain even when only a few hundred candidates can possibly survive. Threading
the candidate set downward means each leaf produces only within it.

This is the third time the same missing piece has come up, which is worth noticing: push-down needs
**gather**, a read of an arbitrary identity list. It is the keystone for the whole filtering story, not
a nice-to-have. One soundness caution — a candidate set pushes through `AND` freely, through `OR` only
as the union of what each branch may contribute, and through `NOT` not at all without care. Getting
that wrong over-matches silently, which is the worst failure shape available here.

**Layer 3 — let the representation answer when it can.** When every operand is Roaring, do not
decompose into driver-plus-probe at all: container-wise multi-way intersection beats it outright, and
wants ordering by cardinality rather than by the rank above. So the planner should ask "can all of you
bulk together?" before it picks a driver. And as noted earlier, `NOT` over a negative-form set is a
representation flip rather than a scan, so the complement that Layer 0 works to avoid is free on
exactly the representation most likely to be holding a dense arity.

**The structural recommendation underneath all of this:** legacy fuses planning and execution into one
recursive walk — `enumerateClasses` decides *and* does, in the same method. Split it into normalise →
plan → execute. That is the same plan/act separation you proposed for structures, arriving for the
same reason: once the decisions are a value rather than control flow, you can inspect the plan, test
it against expected orderings, and the executor becomes dumb. Worth noticing that the idea generalises
— it is probably the strongest single idea in this whole discussion.

### 2. Renaming the architecture to "plugin"

**Agreed, and it is a relabel rather than a redesign.** Richards' *Software Architecture Patterns*
names the microkernel pattern and the plug-in architecture pattern as the same thing, and describes it
as exactly your two lists: a core system plus plug-in modules, with the application logic in the
plug-ins. Eclipse is the canonical example, and your plugin contract — a structure registers its op
codes and a codec — is the same shape as an Eclipse extension point. So the two names are
interchangeable in the literature and "plugin" is the more widely recognised one. Take it.

**But I would not write "the core is almost redundant" into the document.** It is thin, which is the
goal, and the five jobs above are not nothing — in particular cross-arity resolution can only live
there. "Redundant" invites the next reader to delete it and scatter those five jobs into the
components, which is the failure mode this architecture exists to avoid. *"The core is thin by
design: it holds the lists, mints identities, drains the barrier, owns the transaction, and resolves
arities for structures. Everything else is a plugin."* says the same thing without the invitation.

### 3. `ArityComponent` and `StructureComponent`

The uniform marker is a reasonable thing to want, and it fixes the `java.lang.Module` collision the
plan flagged. One real concern, then a recommendation.

**The concern is that ECS already spent the word, on a different thing.** In flecs and EnTT, a
*component* is data attached to an entity — which in your vocabulary is a **catalog**, not an arity.
A reader arriving from ECS will read `ArityComponent` and expect a column. That is the one audience
the plan explicitly hoped `Component` would serve, so it is worth getting right rather than
inheriting the confusion.

**The recommendation: take `Component`, but take it for all three, and define it.** If it is
`ArityComponent`, `StructureComponent` and `CatalogComponent`, then "component" uniformly means *a
unit the core holds and drives through a lifecycle*, and the ECS reader is briefly surprised once
rather than persistently misled. The failure mode is only using it on two of the three, because then
the word means "plugin" in two places and "column" in the third.

And the fix for the collision costs one line: **`GOALS.md` already has a "The words, defined" section
that exists precisely to reclaim words that mean something else elsewhere** — it does this for
Ontology, Entity, Arity, Catalog and Structure. Adding *Component — a unit the core holds and drives;
not the ECS sense, which is what this project calls a catalog* settles it permanently, and it also
discharges the "`Module` — currently ambiguous, a known open naming problem" entry that is sitting
there now.

The alternative, for completeness: no suffix at all — `Arity`, `Structure`, `Catalog` — with the
package saying they are plugins. That is what the JDK and Spring do, and it reads well. I prefer your
version if and only if the definition line goes into `GOALS.md` with it.

### 4. Client-supplied identity sets for computed arities

**This already works, and that is the interesting part of the answer.** It needs no contract change,
no new interface, and no core support. `IdentitiesSet` is an interface; a client writes one whose
`contains(i)` is `0 <= i && i < n`, whose `size()` is `n`, and whose `identityAt(i)` is `i`. That is
about ten lines, it stores nothing, and every default in the algebra works on it immediately.

It is the **set-side twin of a decision you already made**: catalogs are interfaces rather than
classes precisely so that a value computed from the identity costs no memory. A computed occupied set
is the same reasoning applied to the other half of an arity. The perceptron case is a good one —
weights are `inputs × outputs`, the count is known before anything is minted, and no identity is ever
removed, so storing the set at all would be storing a fact that is already implied.

Three things worth adding to it:

**Ship a dense-range set in `sets/`.** Append-only arities are the common case, not an exotic one, and
today the nearest thing is a negative set with zero holes — which works, but carries the hole
machinery for nothing. A dedicated form makes the common case explicit and makes `size()` genuinely
free rather than nearly free.

**The perceptron case is deeper than a range: it is a product arity.** The weight arity is the
Cartesian product of two others, and its identity is `i * outputs + j` — row-major decomposition,
which is the same arithmetic `LongChunkIntUniform` already does for chunk and offset. So a product
arity is a computed set *plus a computed index*, and you have written the index half already. If
product arities are going to be common — and for anything matrix-shaped they are — that pairing is
worth naming as a thing rather than rediscovering per client.

**Two cautions.** `register` and `unsign` on a computed set must **refuse**, not silently succeed or
silently no-op, which is the same "nothing narrows silently" rule applied to membership: a computed
set is immutable by construction and a caller who tried to mutate it has a bug, not a preference. And
a free set does not imply free columns — `ensureCapacity` on the arity still has to grow every
catalog, so the saving is the set, not the arity.

Lastly, this is a good test of whether the plugin claim is true. If a client can supply an
`IdentitiesSet` the engine has never heard of and every structure, filter and catalog keeps working,
the contract is doing real work. If something breaks, it will name the place where the engine is
still assuming a representation it shipped — and that is worth knowing before the contract is
written down as settled.

---

## Part 3 — what this changes about the file layout

Three of the above have a shape consequence, on top of the tree from the previous message:

- **`filters/` is a peer package, not a child of anything.** It is over sets, catalogs *and*
  structures, so it cannot honestly live inside any one of them — the same argument that moved
  `structures/` out from under the arity.
- **The plan phase needs a type**, since a planned-but-unapplied change is a value that gets passed
  around, inspected, and possibly discarded. It belongs with the `Structure` contract, because what
  is in it is the structure's own primitives.
- **The journal is core, not plugin.** Each structure supplies op codes and a codec; the journal
  itself is one of the core's five jobs, so it sits at the root beside the registry rather than in a
  package of its own.

Nothing here argues against the earlier tree; it adds one peer package and two types.

---

## Sources

- [Software Architecture Patterns — microkernel / plug-in (O'Reilly)](https://www.oreilly.com/library/view/software-architecture-patterns/9781098134280/ch04.html)
- [ARIES: A Transaction Recovery Method Supporting Fine-Granularity Locking (Mohan et al.)](https://web.stanford.edu/class/cs345d-01/rl/aries.pdf)
- [ARIES: Logging and Recovery — UC Berkeley CS262a](https://people.eecs.berkeley.edu/~kubitron/courses/cs262a-F12/lectures/lec05-aries-rev.pdf)
- [Predicate Migration: Optimizing Queries with Expensive Predicates — Hellerstein & Stonebraker, SIGMOD 1993](https://dl.acm.org/doi/pdf/10.1145/170036.170078)
- [Optimization of Conjunctive Predicates for Main Memory Column Stores — VLDB](http://www.vldb.org/pvldb/vol9/p1125-kastrati.pdf)
