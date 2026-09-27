# Decisions

The author's words. Rules: `CLAUDE.md` → Markdown files.

## Process

- This is the plan: we code the classes to design; later we will add the boilerplate, like bulk
  access to columns in many types (int, long, double).

## Architecture

- Arities are the first type of element; structures are the second.
- "Component" replaces "Module" as the word for plugins, in every name.
- I guess we better rename the architecture to "plugin" based, since the core is almost redundant.
- The "Structure" interface meant to be the second type of elements (the relations
  around sorts of elements (arity)) in the "OntologyEngineCore".
- We need to clarify each component is a component, so rename things like "ArityComponent" and
  "StructureComponent".
- The delegation-like actions should be done there [in the core].
- The core must also have contracts about delegation; we can (right now) declare an interface [for
  them].
- The ontructure package can be renamed but must be used, because incubation contains many unwanted
  elements that are not worth removing.
- The former version's missing features were critical for the application. I need the modularity and
  "Logging, Undo-Redo, ..." at the same time.

## Identities and removal

- I plan on using [the vacuum idea] where the index of an identity is converted to identity at the
  ArityModule's identitiesSet.
- If the identity number rather than the index is given (which is handled by the identities set of
  the ArityComponent), it auto skips the removed ones; we just need to specify that the intended
  behavior is using the arity component like that.
- When execution (through the multi-arity structures, and maybe few other cases I do not remember
  right now) reaches identities, they check if they exist, when necessary.
- The removal and binary search [over the hole list] likely will never get that slow.
- We should allow a judgment call method alongside auto method (does what you say) for the client.
- Structures are not responsible for the element count; it is just the structure add-on.
- Consider there is an indexer (2-arity structure) that reached a dead
  identity, it should not vacuum it alone. That would be harmful.
- I think the vacuum will be a rare call, on storing to disk and reloading from it.
- We must make a "forget" call that removes the associated data (like the connections on the indexer)
  but doesn't shift all the identities. Delegations over that call will be handled by the core.
- Importantly, if a removed identity gets captured back, a forget call must run on it prior to capture.
- The forget is necessary at the capture back because the data of the former identity is still there.
- Make "Vacuum is the undo horizon." is the currently used solution.
- No structure vacuums a dead identity it reaches on its own.
- Bulk catalog reads intersect the range with the occupied set once, then read, instead of checking
  each element.
- We need to allow client to skip the validation of identities, when they likely know it.
- The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. If checking is requested by client.

## Journal and undo

- Some actions have an inverse that is among the other actions. Call it "invertible".
- Some actions recording the change is enough for calculating the undoing.
  Call it "inverse calculatable".
- Some actions are not undoable, snapshot taking is expensive se we need to allow "break undo history".

## Identity sets

- Add PositiveLong and [Positive]Int, also deleting the narrow variants, to the plan.
- Right now the identitiesSet doesn't need to hold a "firstIndex"; doesn't really matter though.

## Filtering

- The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like "namedLike" or "childOf". Also this is essentially a query service.
- Right now the filtering service can only provide the logical operators, and filters on data depend
  on the client. This is somewhat intended, since we allowed the data to vary, but some
  filtering modules to simplify the client's experience must be done.



## Words

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

## Terms used in questions and inferences

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
- **note-only** — a logging method: the journal records that something changed, but not the old values,
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

## Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **Identities are dense and local to an arity**, minted from 0, so their values stay small. Ends if
  identities become global across arities or carry generation counters.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
- **Modularity the fullest** The engine must be able to express anything.
- [The DAG is] optimized for the case where the leaves are more than branch nodes, and parent*child
  count mostly does not exceed a constant. *(Javadoc of `SetHierarchyDirectedAcyclicIntGraphModule`.)*
