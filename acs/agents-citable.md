# acs: citable

## Architecture

- **1.** Arities are the first type of element.
- **2.** The "Structure" interface meant to be the second type of elements (the relations
  around sorts of elements (arity)) in the "OntologyEngineCore".
- **3.** The core handles the data base side; even though the components are able to delegate themselves,
  they must pass it through the core for the future plans. *(2026-10-15)*
- **4.** The core handles creation-addition of components, delegations, journal (logging + undo/redo),
  persistence, filtering + analysis, also system analytics like how much RAM is used by/where, etc.
  *(The author wrote "plugins"; renamed to "components" at the author's request.)*
- **5.** [Structures referring to their arities by ints, which the core resolves:] this has its own
  results. We cannot "insert" arities or structures, they become somewhat independent of core (any
  other core with different structures can accept it), using object references makes them less
  dependent on the core. So I am open to suggestions, but for now ints are enough.
- **6.** One idea raises: the components can use object pointers to each other instead of ints. We do not go
  with that idea because the journalling side must be handled with the core, but more importantly, in
  the future we may want to add features that reason about the multiple of arity, structure and
  procedure instances. *(2026-10-15)*
- **7.** [The old plan's "growth is propagated directly from the arity to everything attached" is
  declined:] "different arity" == "different thing".
- **8.** Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. Floating narrowing rounds instead. *(Old
  plan's wording, confirmed by the author.)*
- **9.** The structures ensure their rules themselves. *(2026-10-10)*
- **10.** Most importantly, I plan on adding a third type of components, the "procedures", which are
  functions about ACS that benefit being close to the system core.
- **11.** Procedures may and will change catalog data; it is like their goal, because they are meant to be
  close to the ACS. We need a way to store changes on catalog data, which is not hard, just extending
  the arity operations list.
- **12.** [Procedures, only for changes or also for reads:] it must allow a way of reasoning, or at least
  state they do not otherwise.
- **13.** The procedures must be so clear they can be automatically reasoned with our ACS system.
  *(2026-10-15)*

### Details

- **14.** The core must also have contracts about delegation; we can (right now) declare an interface [for
  them].
- **15.** [The] Structure contract must allow a way of storing and loading the data via data streams; the
  core can manage files and alike.

## Identities and removal

- **16.** Core is the only caller of removal requests (from outside), and arity is the one removing, that also
  delegates the message through the core. *(2026-10-15)*
- **17.** The component will depend less on the core; arities remove their elements on their own.
  *(2026-10-15)*
- **18.** Structures are responsible for representing the data optimally for memory and time, while the imv
  values can be removed or structures around them change. *(2026-10-15)*
- **19.** Journalling removed the necessity of separating forgetting from removal, so hereby data is
  forgotten (but journalled) the moment an element is removed. *(2026-10-15)*

### Details

*("vacuum" in this section is renamed to "compacten" at the author's request, 2026-10-15.)*

- **20.** When execution (through the multi-arity structures, and maybe few other cases I do not remember
  right now) reaches identities, they check if they exist, when necessary.
- **21.** [On the above:] the important detail is that the structures are also better off not shifting data,
  so they point at the index rather than the identity; by "they check when necessary" I mean we need
  to handle this phenomenon.
- **22.** [Index there is the memory address where the record's data sits, which does not shift on removal:]
  Yes, it is [that], but [it] would not cause any problem; the functions just need to transform the
  identity number before using it.
- **23.** I have a belief the structures do not need to check if an Identity exists.
- **24.** Structures, if they need to remove an identity on another arity, need to call ArityComponent's
  remove on that identity; that is the recursive step: 0 original arity -> 1 related structures -> 2
  start from (0) on each identity needed to [be] removed.
- **25.** Arity removes the identity and calls the core forget for delegating it. *(2026-10-15)*
- **26.** Forget needs to be undoable.
- **27.** [Storing a structure:] recording the relation is enough. Also at the core level storing can be
  done by temporarily storing, then compacten, then final storage. [That] is not bound to be the only
  [sequence]; allow [the] option of direct storing.
- **28.** [Calculated arities:] the calculated arities must be dependent on a structure; this is better
  since being directly dependent [on] another Arity can actually raise errors.
- **29.** Structures are not responsible for the element count; it is just the structure add-on.
- **30.** I think the compacten will be a rare call, on storing to disk and reloading from it.
- **31.** We must make a "forget" call that removes the associated data (like the connections on the indexer)
  but doesn't shift all the identities. Delegations over that call will be handled by the core.
- **32.** No structure compactens a dead identity it reaches on its own.
- **33.** We need to allow client to skip the validation of identities, when they likely know it.
- **34.** The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. If checking is requested by client.
- **35.** [Undoing a remove after a compacten is] out of scope right now; compacten [is] planned to be the
  horizon of undo.

## Journal and undo

- **36.** I recently decided the "check and call checkless functions" pattern. Simply, there will be
  functions that resolve [a] client request into private reckless functions of unit operations.
- **37.** Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary; multithreading as a single writer with readers on a snapshot. *(Old plan's
  wording, confirmed by the author.)*
- **38.** For every request about ACS, it must be able to store a comment, like the git commit messages, but
  for every action, because every action is made from a resource and later may be audited, which
  will require context.

### Details

- **39.** Some actions have an inverse that is among the other actions. Call it "invertible".
- **40.** Some actions recording the change is enough for calculating the undoing.
  Call it "inverse calculatable".
- **41.** Some actions are not undoable, snapshot taking is expensive se we need to allow "break undo history".
- **42.** "Persistence", saving to files, is just writing some data; no need of journal overhead.
- **43.** Sometimes a cluster of operations is [the] steps of a client request, and undoing may take a
  different approach. The example was: adding a parent may cause many remove parent calls, but
  requesting undo on those prior to undoing the add parent is auto rejected. We have two options:
  we can have request resolver functions that call the atomic operations after recording the
  beginning of the operations to the journal, or making them tell the inverse. [The] first option is
  likely better, so we are separating the client side of jobs from inside operations.
- **44.** [The old plan's write barrier is declined; it] needs more details, explained here: for journal
  actions with checks and multi steps, a record at the beginning and end must be done, [and it]
  must contain a name for logging and alike; for steps, each must just contain parameters, itself
  and [its] inverse. More details can be added.
- **45.** A structure registers its op codes and a codec for its journal records. *(Old plan's wording,
  confirmed by the author.)*
- **46.** An operation declares its own logging mode; the client does not. *(Old plan's wording, confirmed
  by the author.)*
- **47.** [Recording positions, so undo restores the order of parents:] no need, we are recording only
  relations.
- **48.** I plan on making the journal contract alongside the core contract.
- **49.** While your [Claude's] decision of throwing [when undo is called] during a request is sound, we also
  need to add an abort option.
- **50.** [Adding identities:] Yes, journal adding identities; I forgot to tell because it is assumable.

## Descriptions

- **51.** [The description:] it must describe each instance of [a] component of the core using a string value;
  it must be able to be read and set. It is there so the agent can state the purpose of each
  component. *(2026-10-11)*
- **52.** Also classes can have a description. For an example, there can be many different implementations
  of mathematically [the] same indexing logic (say two arities, one element from arity one is
  connected to one element from another arity or not) but different ways of representing them,
  performant for different cases. The agent [is] meant to choose between them; also the agent can
  decompile the class files to make its own descriptions. *(2026-10-14)*

### Details

- **53.** It [the description] is somewhat of a journal itself, so no need of journalling, except while
  creating and adding we can require the requests take another string parameter for the description.

## Persistence

### Details

- **54.** I want you to properly handle the persistence using methods in the core (and contracts where
  necessary) that take a target folder and store everything in their files. *(2026-10-11)*
- **55.** We can make the persistence manager also variable. *(2026-10-13)*

## Identity sets

### Details

- **56.** I think we need to find ways to define arity sizes; for example, [in] the "cartesian" instance I
  told, the arity that represents the cartesians can have its size calculated.

## Filtering

- **57.** The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like "namedLike" or "childOf". Also this is essentially a query service.

### Details

- **58.** Right now the filtering service can only provide the logical operators, and filters on data depend
  on the client. This is somewhat intended, since we allowed the data to vary, but some
  filtering modules to simplify the client's experience must be done.
- **59.** Cool names for "OR" variants like "IMPLIES", all are redundant. Keep logic minimal right now.
- **60.** They [the OR variants] are boilerplate, meant to be handled last.
