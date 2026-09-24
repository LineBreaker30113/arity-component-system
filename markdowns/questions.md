# Open questions

The step before a decision. Each question lists its options and who holds each. Rules: `CLAUDE.md` →
Markdown files.

## Architecture

- **What does the core do?**
  - Author: "Right now the core feels too empty, like the arity modules and structure modules
    communicating does almost everything already."
  - Claude: it holds and drives the components — the component lists, minting, the sync barrier, the
    transaction boundary, and cross-arity resolution.
- **Bring back proxies? If so, at what granularity?**
  - Author: "If we decide to bring back the proxy, the core may handle the proxies of classes."
  - Claude: one proxy at the transaction boundary (`runAtomically`), never per method.
- **Final name for `ontructure/`?**
  - Claude: `kernel`, since the plan already says "microkernel". Avoid `core`, used by `legacy/core/`
    and `OntologyEngineCore`.
- **Package layout under it?**
  - Claude: `ontructure/{sets, catalogs, arity, structures, filters}`, core types at its root.
    `structurer/` goes away. `sets/` and `catalogs/` are layers, not parts of the arity.
    `CatalogModuleBase` becomes top-level in `catalogs/`; `PREFERRED_*`, `typeName`, `exact*` move
    there too. Listers move beside the chunked forms. Rename `ModularArityCatalog` (it is a table, not
    a column). Give the empty `structures/indexers/` a `package-info.java`.
- **Should catalogs also be components (`CatalogComponent`)?**
  - Claude: yes, and define the word: in ECS it means what this project calls a catalog.
- **How does a structure refer to its arities?**
  - Claude: by arity slots (ints the core resolves), not arity objects.
- **What does the `Structure` contract contain?**
  - Old plan: which arities it spans; growth and removal notification; per-element read and write of
    its own state; its op codes and codec.
- **New name for `SetHierarchyDirectedAcyclicIntGraphModule`?** It still ends in `Module`.
- **Can a client supply its own `IdentitiesSet`?**
  - Author: "Some arities may have elements up to a certain number easily computed, like say we have a
    single layer fully [connected] perceptron: its nodes are one arity, its weights are a different
    arity, and its count and range can be easily computed, so the client can implement a specific
    identity set if it wants. What do you think about that?"
  - Claude: yes.
- **Does persistence go through the journal?**
  - Author: ""Persistence" saving to files is just writing some data, no need of journal overhead.
    What do you think?"
  - Claude: no; saving writes the data directly.

## Removal and undo

- **How are pending removals held?**
  - Author: "Why not an ordered list?"
  - Claude: a list; switch to a full sweep past a size threshold.
- **How is "forget before capture-back" enforced?**
  - Claude: a monotonic `everReached` field; minting runs forget on any identity at or below it.
- **What does `truncate` do to the occupied set?**
  - Claude: it unsigns what it drops.
- **What are the "extra details" undo sometimes requires?**
  - Author: ""Undo" in "Undo/redo" sometimes requires some extra details, more on that later."
- **How does undo handle a compound change?**
  - Author: ""addParent" under isTight cannot be inverted by undoing all in reversed order; it can be
    reverted by undoing all in the same order; also, it can be undone by closing the isTight
    temporarily."
  - Author: "I think all of them can be handled by making the structures plan the change by rules,
    then act and record it while neglecting the rules temporarily; since the former state was valid,
    the inner steps don't matter. What do you think?"
  - Claude: agrees, and records primitives absolutely (slot, index, old, new), so undo order does not
    matter.
- **Redo: do records carry old and new, or does redo re-execute?**
- **Undoing a remove after a vacuum: part of undo, or its own operation?**
  - Author: "Undoing a remove after vacuum would make it hard, but we can append a new identity that
    has the same data as the removed one; we just need to have a check for the vacuum in the journal."
  - Claude: its own operation, named "restore" (new identity, same data).
- **Undo of the DAG's free list (`freeSlots`, `multiData` length)?**
  - Old plan: journal it, or make slot allocation deterministic on replay.

## Journal

- **Which logging modes exist?**
  - Claude: invert, snapshot, and note-only (for derived state, declared by the structure).

## Identity sets and catalogs

- **In what order do the identity-set changes land?**
  - Claude: write `IdentitiesSetPositiveInt` and `PositiveLong` before deleting the narrow variants,
    since the positive byte and short sets are the only positive ones today.
- **Ship a dense-range `IdentitiesSet` for append-only arities?**
  - Claude: yes.
- **What is the manual (judgment-call) existence method named?**
  - Claude: a name that looks deliberate, such as `identityAtUnchecked`.
- **Gather and name → slot: urgent or deferred?** The old plan marked both "deliberately deferred";
  both are also among the urgent handoffs.

## Filtering

- **Which data-dependent filters does the engine ship?**
  - Claude: structural leaves — catalog comparisons (`eq`, `lt`, `between`, …) and reachability
    (`childOf`, `descendantOf`, …). Interpreting leaves (`namedLike`) stay the client's.
- **How to restructure AND / OR / NOT trees for the fastest evaluation?**
  - Author: "Say we give the client the "AND", "OR", "NOT": can we find a way to better restructure
    those claims for the fastest computation just by looking at them, other than the
    "isEnumeratePreferred" workaround in the legacy?"
  - Claude: normalise, plan, execute — push NOT to the leaves, flatten to n-ary, merge ranges on the
    same catalog, order by estimated size and cost, push candidate sets down (needs gather).
- **Are `CONTRADICTS` and `IFOF` set operations or boolean predicates?**

## Docs

- **Subfolders for `markdowns/`?**
  - Author: "We can later organize those into folders."
- **Does the quote test cover `README.md`?** Its goal, "what it is not", and application text was
  migrated by Claude from `GOALS.md`.
- **What licence does the repository carry?** There is no `LICENSE` file and no `<licenses>` block in
  `pom.xml` (checked 2026-09-25), so the default is all rights reserved — which sits badly with a
  README that invites people to reimplement the idea in their own language. The choice also decides
  what this project may take in from elsewhere, and what others may do with it.

## Old plan — confirm, change, or reject

Positions of the old `ARCHITECTURE.md`, written largely by Claude.

- Bulk over per-element everywhere; structures must also support range reads.
- Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. Floating narrowing rounds instead.
- Growth is propagated directly from the arity to everything attached.
- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary (`runAtomically`); multithreading as a single writer with readers on a snapshot.
- A structure registers its op codes and a codec for its journal records.
- Journal primitives are ints in an `IntArrayList`, not objects; `UndoableAction` only for
  client-supplied compounds.
- Record positions, not just identities, so undo restores order.
- An operation declares its own logging mode; the client does not.
- Write barrier: `journal.touch(this, element)` in private mutators, policy in one class.
