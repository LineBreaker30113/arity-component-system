# Viewed Questions

Questions viewed by human but didn't being put to decisions. May have doubts or better naming.
Human has the most authority. I while reviewing didn't read the meaning of the "Author" and used it instead.

Delete me! I forgot to place the section headers except the "Architecture", so move them carefully to decisions.

## Architecture

- **What does the core do?**
  - Author: "Right now the core feels too empty, like the arity modules and structure modules
    communicating does almost everything already."
  - Claude: it holds and drives the components — the component lists, minting, the sync barrier, the
    transaction boundary, and cross-arity resolution.
  - Human: The terms are not well-defined. Core handles creation-addition of plugins, delegations,
    journal: logging+undo/redo, persistence, filtering+analysis also telling how much RAM used by, etc.


- **Bring back proxies? If so, at what granularity?**
  - Author: "If we decide to bring back the proxy, the core may handle the proxies of classes."
  - Claude: one proxy at the transaction boundary (`runAtomically`), never per method.
  - Human: Undecided. Is journal enough? Proxies add one layer on each call that slows things down.


- **Final name for `ontructure/`?**
  - Claude: `kernel`, since the plan already says "microkernel". Avoid `core`, used by `legacy/core/`
    and `OntologyEngineCore`.
  - Human: the project name became arity component system, so we will use "acs-beta" right now.


- **Package layout under it?**
  - Claude: `ontructure/{sets, catalogs, arity, structures, filters}`, core types at its root.
    `structurer/` goes away. `sets/` and `catalogs/` are layers, not parts of the arity.
    `CatalogModuleBase` becomes top-level in `catalogs/`; `PREFERRED_*`, `typeName`, `exact*` move
    there too. Listers move beside the chunked forms. Rename `ModularArityCatalog` (it is a table, not
    a column). Give the empty `structures/indexers/` a `package-info.java`.
  - Human: I plan on making a separate file for every top tier interface, ArityComponent variants
    and StructureComponent variants, and identitySet. Then packages for implementations.
    I prefer packages of "identifiers", "arity", "structures.<x>ary".


- **Should catalogs also be components (`CatalogComponent`)?**
  - Claude: yes, and define the word: in ECS it means what this project calls a catalog.
  - Human: I accept it, but we must allow the client to decide to define catalogs without modules.
    I mean for each Arity we will have a table, and the client must be able to define table
    itself or use a modular table. Also for clarity we will need to name them like
    "ArityCatalogTable" or "ArityModularCatalogTable" or "ArityCatalogRowComponent"


- **How does a structure refer to its arities?**
  - Claude: by arity slots (ints the core resolves), not arity objects.
  - Human: This has its own results. We cannot "insert" arities or structures, they become somewhat
    independent of core (any other core with different structures can except it), using object
    refences makes them less dependent on the core. So I am open to suggestions, but for now ints are enough.


- **What does the `Structure` contract contain?**
  - Old plan: which arities it spans; growth and removal notification; per-element read and write of
    its own state; its op codes and codec.
  - Human: confirmed. Also, persistence, we forgot to talk about it.


- **New name for `SetHierarchyDirectedAcyclicIntGraphModule`?** It still ends in `Module`. Human: Replace with `Component`.


- **What do the names inside `src/` become, now that the project is Arity Component System?** The
  `OE_` prefix, `OntologyEngineCore`, and the package `org.halim.ontology` all carry the old name.
  - Claude: decide it together with `ontructure/`'s final name and the package layout, and do the one
    pass before the first push, so every import moves once. Leave `legacy/` alone — renaming there
    only makes the old commits harder to read.
  - Human: Confirm. "legacy" is read only. Now the name is "ACS", replace them with "ACS".


- **Can a client supply its own `IdentitiesSet`?**
  - Author: "Some arities may have elements up to a certain number easily computed, like say we have a
    single layer fully [connected] perceptron: its nodes are one arity, its weights are a different
    arity, and its count and range can be easily computed, so the client can implement a specific
    identity set if it wants. What do you think about that?"
  - Claude: yes.
  - Human: Right now the engine accepts such thing with ease so we just need to document it. Later
    we may need to make few additions for it to work.



- **Does persistence go through the journal?**
  - Author: "Persistence" saving to files is just writing some data, no need of journal overhead.
    What do you think?"
  - Claude: no; saving writes the data directly, no need of overthinking.


- **How are pending removals held?**
  - Author: "Why not an ordered list?"
  - Claude: a list; switch to a full sweep past a size threshold.
  - Human: I think we do not need to make pending removals, the identity sets handle it.


- **How is "forget before capture-back" enforced?**
  - Claude: a monotonic `everReached` field; minting runs forget on any identity at or below it.
  - Human: Identity set must allow a way of capturing the lowest positive excluded value.
    I mean there is be an IntArrayList of excluded, first element if the list is not empty,
    or just increasing the borders. So the identities set contract is responsible.


- **What does `truncate` do to the occupied set?**
  - Claude: it unsigns what it drops.
  - Human: We do not need to make a "truncate" right now. But later we can make this.
    Also, we are able to the vacuum (may rename to "compress") actually can simply swap the last
    few entries with the death ones and truncate out the death ones.


- **What are the "extra details" undo sometimes requires?**
  - Author: "Undo" in "Undo/redo" sometimes requires some extra details, more on that later."
  - Human: Sometimes a cluster of operation is steps of a client request, and undoing may take a
    different approach. The example was adding a parent may cause many remove parent calls, but
    requesting undo on those prior to undoing the add parent is auto rejected.
    We have two options, we can have request resolver functions that calls the atomic operations
    after recording the beginning of the operations to the journal, or making them tell the inverse.
    First options is likely better, so we are separating the client side of jobs from inside
    operations.


- **How does undo handle a compound change?**
  - Author: ""addParent" under isTight cannot be inverted by undoing all in reversed order; it can be
    reverted by undoing all in the same order; also, it can be undone by closing the isTight
    temporarily."
  - Author: "I think all of them can be handled by making the structures plan the change by rules,
    then act and record it while neglecting the rules temporarily; since the former state was valid,
    the inner steps don't matter. What do you think?"
  - Claude: agrees, and records primitives absolutely (slot, index, old, new), so undo order does not
    matter.
  - Human: I no longer agree,  I recently decided the "check and call checkless functions" pattern,
    ask if you do not get it. Simply there will be functions that resolve client request into
    private reckless functions of unit operations.


- **Redo: do records carry old and new, or does redo re-execute?** Human: yes, simple, re-execute all.


- **Undoing a remove after a vacuum: part of undo, or its own operation?**
  - Author: "Undoing a remove after vacuum would make it hard, but we can append a new identity that
    has the same data as the removed one; we just need to have a check for the vacuum in the journal."
  - Claude: its own operation, named "restore" (new identity, same data).
  - Human: Out off scope right now, vacuum planned to be the horizon of undo.


- **Undo of the DAG's free list (`freeSlots`, `multiData` length)?**
  - Old plan: journal it, or make slot allocation deterministic on replay.
  - Human: The slot allocation will likely be deterministic, and it feels like it won't matter.


- **Which logging modes exist?**
  - Claude: invert, snapshot, and note-only (for derived state, declared by the structure).



## Identity sets and catalogs


- **In what order do the identity-set changes land?**
  - Claude: write `IdentitiesSetPositiveInt` and `PositiveLong` before deleting the narrow variants,
    since the positive byte and short sets are the only positive ones today.


- **Ship a dense-range `IdentitiesSet` for append-only arities?**
  - Claude: yes.
  - Human: I do not get it, missing context, likely not worth for keeping.


- **What is the manual (judgment-call) existence method named?**
  - Claude: a name that looks deliberate, such as `identityAtUnchecked`.
  - Human: yes, this naming convention is good, but out of scope, this is boilerplate polishing. 


- **Gather and name → slot: urgent or deferred?** The old plan marked both "deliberately deferred";
  both are also among the urgent handoffs.
  - Human: I do not get it again, let's keep it.


## Filtering

- **Which data-dependent filters does the engine ship?**
  - Claude: structural leaves — catalog comparisons (`eq`, `lt`, `between`, …) and reachability
    (`childOf`, `descendantOf`, …). Interpreting leaves (`namedLike`) stay the client's.
  - Human: We can ship many leaves, the "equals" for integer like ones or "near" for floating types are cool. Is there a concern with making "String" filtering ways provided by the engine?


- **How to restructure AND / OR / NOT trees for the fastest evaluation?**
  - Author: "Say we give the client the "AND", "OR", "NOT": can we find a way to better restructure
    those claims for the fastest computation just by looking at them, other than the
    "isEnumeratePreferred" workaround in the legacy?"
  - Claude: normalise, plan, execute — push NOT to the leaves, flatten to n-ary, merge ranges on the
    same catalog, order by estimated size and cost, push candidate sets down (needs gather).
  - Human: Your suggestions are cool, please better define them as plan, since they are not urgent.
    I do think we can make few things, like if there is a "Not" inside an "And" the other leave can be enumerated then the ones satisfying not are removed.
    But that is a topic with a lot of things that can be done. One of most important is that we need to make a temporal "isEnumeratePrefered" method that calculates it using recursion on each filter node. That was the former design in the legace, read and bring it to me for us to discuss.


- **Are `CONTRADICTS` and `IFOF` set operations or boolean predicates?**
  - Human: Cool names for "OR" variants like "IMPLIES", all are redundant. Keep logic minimal right now.


## Docs

- **Subfolders for `markdowns/`?**
  - Author: "We can later organize those into folders."
  - Human: we do not need it right now.


- **Does the quote test cover `README.md`?** Its goal, "what it is not", and application text was
  migrated by Claude from `GOALS.md`.
  - Human: I did not get it, if you get it bring it out.


- **What licence does the repository carry?** There is no `LICENSE` file and no `<licenses>` block in
  `pom.xml` (checked 2026-09-25), so the default is all rights reserved — which sits badly with a
  README that invites people to reimplement the idea in their own language. The choice also decides
  what this project may take in from elsewhere, and what others may do with it.
  - Human: That's a real question, I would like a lisence that is like a bomb and forces every one responsible to open source their code. Research this well and later we can discuss.


## Old plan — confirm, change, or reject

I didn't get few parts but didn't find it worth to remove, so I covered them with `(=(<thing>)=)` likely misses context.
Things added later are in `(!(<thing>)!)`

#### Human Confirms:

- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary (=( (`runAtomically`) )=); multithreading as a single writer with readers on a snapshot.


- Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. (=(Floating narrowing rounds instead.)=)

- A structure registers its op codes and a codec for its journal records.

- Journal primitives are (=( ints in an `IntArrayList`, not objects; `UndoableAction` only for
  client-supplied compounds. )=) (!( they meant to be the action and it's parameters with an inverse. )!)

- Record positions, not just identities, so undo restores order. (!(I didn't get it, contextless)!)

- An operation declares its own logging mode; the client does not.

#### Human Declines


- Bulk over per-element everywhere; structures must also support range reads. (!(What does it even mean?)!)
- Growth is propagated directly from the arity to everything attached. (!("different arity" == "different thing")!)
- Write barrier: `journal.touch(this, element)` in private mutators, policy in one class. (!(Needs more details, explained here:
for journal actions with checks and multi steps a record at the beginning and end must be done,
must contain a name for logging and alike, for steps each must just contain parameters, itself and inverse.
More details can be added.
)!)


# Open questions

The step before a decision. Each question lists its options and who holds each. Rules: `CLAUDE.md` →
Markdown files.

## Architecture

## Removal and undo

## Journal

## Identity sets and catalogs

## Filtering

## Docs

## Old plan — confirm, change, or reject

Positions of the old `ARCHITECTURE.md`, written largely by Claude.
