# Architecture plan

What the engine is meant to be, what is decided, and what is still open. Written at the end of the
session of 2026-09-08/09; nothing below is committed, all of it is working-tree state.

This is a *plan*, not a description. Where the code already matches, it says so. Where it does not,
the gap is named as a gap rather than written up as if it existed.

---

## The model

**An engine is a list of arities and a list of structures over them.**

- An **arity** is a set of identities plus the data hanging off them — the occupied set and its
  columns. `incubation/structurer/ArityModule` is this, and `ModularArityCatalog` is its general
  implementation.
- A **structure** is a relation, over one arity or between several. A hierarchy is unary. An indexer
  is binary. Higher arities are the point of the project and are not built yet.
- Neither list is fixed in length or in element type. That is the whole difference from what came
  before.

This is the microkernel reading of the engine, and it is the answer to what `legacy/` could not do.
The distinction is not layering — legacy has more layers than incubation does — it is that **the core
knows only identities and their lifecycle, and everything that gives them meaning attaches from
outside.** Legacy's core knew what a class and an element were: `newClass`, `newElement`,
`classIdentity`, `elementIdentity` appear in its interface signatures, and
`OntologyServicesFactorySegmentation` enumerates seven fixed getters. Adding a third sort meant
editing interfaces. There was no list of anything.

`OntologyEngineCore` in `incubation/` is the previous attempt at this exact model — `entityClusters`
beside `indexingServices`, two lists — and it fails the same way: the structure list is typed to one
concrete structure. The list has to be typed to a contract, not to a class.

---

## Layers as they stand

| Layer | Package | State |
|---|---|---|
| Identity representation | `structurer/sets/` | Seven encodings — negative at byte/short/int/long, positive at byte/short only, Roaring. Narrowing to five: see below. |
| Data per identity | `structurer/catalogs/` | Column contract, integral and floating families, lister and chunked storage, six widths. Done. |
| The arity | `structurer/ArityModule`, `ModularArityCatalog` | Occupied set plus columns, cell addressed `(catalog, identity)`. Done. |
| Structures | `structurer/structures/` | One unary structure. `indexers/` is an empty directory. |
| The engine | — | Does not exist. No list of arities, no list of structures. |
| Cross-cutting concerns | — | Does not exist. See below. |

The one structure that exists is
`structurer/structures/graphs/SetHierarchyDirectedAcyclicIntGraphModule`: a set hierarchy as a DAG
over int identities, with an inline-or-spill encoding, root at element 0 with its children in slot 0,
and a `tighten` that maintains the covering relation.

---

## Decided

### The core knows lifecycle, not meaning

The arity mints identities. Everything attached follows. That contract is already written twice — on
`CatalogModuleBase` and on `ArityModule` — and it is the shape the `Structure` contract should copy.

### Bulk over per-element, everywhere

Addressing a column by parameter rather than by receiver puts a dispatch inside the callee, so range
accessors matter more in the table form, not less. Defaults are the per-element loop; the value is the
override point, and every storage form takes it. This rule extends to structures: a structure that
cannot be read in ranges will be the slow one.

### Nothing narrows silently

Conversion is exact or it throws. `UnsupportedOperationException` means the question was wrong;
`ArithmeticException` means the value did not fit. Floating narrowing is the deliberate exception,
because a rounded measurement is still the value it claims to be and a truncated identity is not.

### `truncate` is the wrong operation for a structure

A catalog has a ceiling and can lower it. A graph does not — you want to remove *particular*
elements. So the `Structure` contract should not demand `truncate`; it should demand a way to be told
which identities went away.

### Synchronisation is direct for growth, deferred for removal

Not a generic per-identity listener. Legacy had those — `OntologyHierarchyChangeListener` and
`OntologyClassificationChangeListener`, with prior/post pairs — and they are exactly what puts a
virtual call per identity per subscriber on the path this engine spent its whole design avoiding.

Instead:

- **Growth** is a direct call from the arity over its structure list, the same way it drives its
  catalogs. It already holds them; no registration is needed.
- **Removal** accumulates into a pending set on the arity, drained at a barrier — a `sync()`. A
  structure like the DAG wants to fix up many edges at once, not one at a time, and this composes with
  `tighten`: reconcile, then tighten once. This is what flecs does with deferred operations.

### Cross-cutting concerns are a journal, not a proxy

Legacy applied logging, undo/redo and locking by intercepting method calls — a
`java.lang.reflect.InvocationHandler` (`legacy/core/OntologyServiceProxy`) wrapping every segment,
driven by `@OntologyTransaction` and `@BypassCrossCuttingConcerns`. That cannot survive this
architecture for two independent reasons: a new structure would need new annotated interfaces for the
proxy to wrap, and a reflective interface call at every entry point is the opposite of the rule above.

So mutations become **data**. One journal, and every concern is a consumer of it:

- **Undo/redo** — replaying the journal backwards or forwards.
- **Logging** — a reader on the journal.
- **Persistence** — the same records, which collapses two subsystems into one.
- **Locking** — one lock at the transaction boundary. Legacy's `runAtomically(String, Supplier)` is
  already the right-shaped API; keep it and drop the proxy underneath.
- **Multithreading** — a single writer draining a queue, readers on a stable snapshot.

A structure registers its op codes and a codec for its own records, and undo, logging and persistence
then work for structures the core has never heard of. That is the plug-in contract doing real work.

### Undo is by decomposition, not by inverting compounds

A compound operation is hard to invert; its primitives are not. `addParent` under `isTight` is a
sequence — the edge, then whatever `tighten` removed — and undoing the sequence in reverse gives the
compound inverse for free.

The primitive set is small and closed: set `hierarchy[e]`, insert a parent at an index, remove a
parent at an index, push a child, remove a child at an index, take a slot, give a slot back.

Two constraints that follow:

- **Encode primitives as ints, not objects.** An `UndoableAction` object per primitive makes a
  `tighten` over a wide cone allocate thousands of small objects. Opcode plus operands in an
  `IntArrayList`, with an "action" being a range of that journal. `UndoableAction` as an interface
  then earns its place for *client-supplied* compounds the engine cannot decompose.
- **Record the position, not just the identity.** `removeParent` does `removeInt(at)`, which shifts
  the tail; re-adding inserts at the end of the parent run. The relation is restored but the order is
  not, and order is observable through `parentAt`/`childAt`. One extra int makes the inverse exact.

### Reversible and irreversible are both allowed

Invert where it is cheap, snapshot where it is not — physiological logging, in ARIES terms. `detach`
on a node with ten thousand children is ten thousand primitive records where one before-image of its
entry is a few thousand ints and one allocation. The operation should declare its own logging mode;
the client should not have to know which of its edits were which.

### The write barrier is the AOP seam

Where a before-image is taken, it is taken by one call at each mutation site — `journal.touch(this,
element)` — with all policy (capture once per element per transaction, buffer, undo stack, nesting) in
one class. That preserves what AOP was actually buying: the concern stays in a single place. Put the
barrier in the private mutators, not the public API, so anything added later is journalled by
construction rather than by remembering.

### The set widths narrow to int and long

The byte and short representations go, and the positive family gains the two widths it never had. The
end state is five encodings — negative at int and long, positive at int and long, Roaring — where
today there are seven, and the two that exist on the positive side are the two being removed.

The saving they were built for does not survive contact with where the representations are actually
used. The storage is genuinely unsigned — `MAX_VALUE = 0xFFFFL`, reads masked with `& 0xFFFFL`, and
`checkFits` refusing anything outside `[0, 65535]` — so a short-wide set really does carry 65,536
distinct values, and its arithmetic ceiling really is 128 KiB against an int. But that ceiling sits
exactly where the form is the wrong one. A positive set reaches it only by holding every value in its
range, which is when the negative form would have cost two bounds and an empty hole list; a negative
set reaches it only when nearly every identity is a hole, which is when the positive form was wanted.
In the band where each form is the right choice the saving is single-digit KiB — a negative set
carrying four thousand holes saves eight.

Those are per-set figures besides, and `occupied` is the only thing in the tree that holds an
`IdentitiesSet` at all — one per arity, and arities are few. The package-info's "forty identities
under 256 comfortably" was imagining many small sets, and nothing holds many small sets. Against a
saving that size, the four narrow classes are 1,210 lines and a pairing every algebra fast path has to
consider.

The order matters: **`IdentitiesSetPositiveInt` and `IdentitiesSetPositiveLong` are written before
`IdentitiesSetPositiveByte` and `IdentitiesSetPositiveShort` are deleted.** Positive exists only at
those two widths today, so deleting them first removes the representation entirely and leaves negative
and Roaring — and positive is the form for small or scattered sets, which is precisely where a
negative set degenerates worst. The negative pair can go whenever.

This is the first decision here that removes a premise rather than adding one. Storage width bounding
identity *values* is still true and still the reason the wide forms exist; what stops being true is
that the narrow end of that range was worth a class.

### Edit speed is the cheap resource

Edits are important enough to wait for. Reads are not. Every trade above spends write time to buy read
speed or exactness, deliberately.

---

## Open

### Naming

Three collisions, unresolved:

- `ArityModule` is an interface named "module" that *is* the arity, while structures are also called
  modules. One of the two has to give.
- `SetHierarchyDirectedAcyclicIntGraphModule` still ends in `Module` while the package is now
  `structures/`.
- `Module` collides with `java.lang.Module` (JPMS).

Candidates considered, none chosen: `Component` (ECS term of art, and what flecs calls exactly this —
data you attach and detach freely), `Extension` (JUnit 5, Eclipse), `Provider`/`Service` (JDK
`ServiceLoader`), `Bundle` (OSGi), `Facet`. There is no single Java standard. `Component` is the one a
reader arriving from an ECS background will recognise.

### The `Structure` contract

Not written. What the discussion says it needs:

- Which arity or arities it is over, so the engine can drive the right ones.
- The lifecycle half that is not `truncate` — growth, and being told which identities are gone.
- Per-element read/write of its own state, for the journal's before-images and for persistence. These
  are the same method; it is the structure's own serialisation, not a concern leaking in.
- Its op codes and their codec.

### Redo symmetry

Undecided: whether a primitive record carries both old and new (symmetric, a few more ints, redo as
dumb as undo) or whether redo re-executes the operation.

### Free list and derived state under undo

`promote`/`demoteIfInline` mutate `freeSlots` and `multiData`'s length, so restoring rows alone is not
enough. Either journal `freeSlots` as part of the transaction image or make slot allocation
deterministic on replay — journalling it is simpler and it is one small list. Slot 0's root-children
list is derived and self-heals through its dirty flag, so it needs nothing.

### The whole legacy feature set

`legacy/` has, and `incubation/` does not: locking, undo/redo, logging, persistence sessions,
filtering, analytics, in-place walking, and the service layer over all of it. These were judged
critical for the application. The plan above is how they come back; none of it is built.

---

## Handoffs

Written for someone with no memory of this session.

- **`structurer/structures/indexers/` is an empty directory.** The binary structure — the class ↔
  element indexer — has no incubation implementation. `legacy/core/segments/manager/DoubleArrayedIndexer`
  is the previous one: `ArrayList<BitSet> elements` and `ArrayList<IntArrayList> classes`, both
  directions stored, with tombstone lists. Read it before designing the replacement.

- **`structurer/structures/graphs/` has no `package-info.java`.** Every other package in `incubation/`
  has one. Deliberate omission by the assistant, not an oversight to fix blindly — the prose in these
  files is the author's and was not written for them.

- **`catalogs/LongIntCatalog` is a near-duplicate of `catalogs/IntegerValueCatalog`.** Same
  `getData`, same three widened views, same `PREFERRED_LONGI`. Known, unowned; decide whether it is a
  deliberate second name or dead weight.

- **No gather in the catalogs.** They read contiguous ranges. Closure over an occupied set wants a
  read of an arbitrary identity list. Known gap, deliberate — it was scoped out, not forgotten. Noted
  in `structurer/package-info.java`.

- **Catalogs are addressed by slot and nothing resolves a name to one.** The previous module class had
  a commented-out `getCatalog(String)`. Deliberate deferral.

- **`isTight` costs a descendant-cone walk per attachment.** The escape hatch is to clear the flag,
  load, and call `tighten(ROOT)` once. Verified equivalent: the same eight edges built loose then
  tightened, and built tight from the start, produce identical parent sets.

- **The DAG's tests are not in the repo.** Behaviour was verified by a 108-check probe run from the
  scratchpad, not by anything under `src/test/`. Per `CLAUDE.md` the user writes the tests, so the
  probe was deliberately not landed. What it covers: the encoding's transitions, root-list correctness
  against the encoding's own truth after every phase, cycle refusal, slot reuse, `isTight` in both
  directions, loose-then-`tighten(ROOT)` equivalence, and that reduction preserves all 36 reachability
  pairs. If tests are wanted in `src/test/`, that list is the specification.

- **Measured, at working-tree state, uncommitted:** `mvn test` — 182 tests, 0 failures. That figure is
  the pre-existing legacy suite plus `OEIntSetListerTest`; none of it exercises `structurer/structures/`.

---

## Corrections to claims made during the session

- The assistant said `addParent`-with-`tighten` was hard to invert. That was a statement about the
  compound operation; decomposed into primitives it is straightforward, and the decomposition is the
  design above. The original claim was wrong in the way that mattered.
- The assistant first read "microkernel" as a question about `legacy/` and answered that legacy is a
  layered service architecture with AOP by proxy. True, but not the question. The question was whether
  the new version is legacy's microkernel — and it is, in shape: see the top of this file.
