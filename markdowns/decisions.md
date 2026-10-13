# Decisions

The author's words. Rules: `CLAUDE.md` → Markdown files.

## Goal

- ACS is meant to be a database that can be managed with commands. Specifically, the client is meant
  to start the "ACS engine", then must use the "ACS requester" as: `acsr boot` (starts the acs
  application), `acsr loadDB ~/main as main` (loads the database in the path main and allows access
  to it with the name of main), `acsr components main` (shows the components of the database).
  [The names changed afterwards: see Layers and names.]
- "acsr" is meant to work by editing, appending [to] a file of requests and prompting the
  application, which it will read and do.
- Then we will develop a GUI for that application that uses the terminal input (later we can isolate
  it from the terminal); also we will build an AI Agent that will use acsr. That is the goal.
- [Who is it for?] Since this is a good personal project, we can document it for gaining reputation.
- [What is a "paragraph of knowledge"?] It is more complicated [than a passage from one book, or a
  paragraph written from several books]. The first agent will build the personal knowledge graph from
  the books, then an auditing agent will audit it and improve, then a third agent will build a new
  acs of paragraphs from that first acs, which better describes and [is] copyright free. Also,
  paragraphs must cite the resources and reasoning to be accurate. We can talk about it later.
  *(The author wrote "gap" and confirmed "graph".)*
- [Who builds the structure, and who audits it?] Must allow agents at every step, since I want to
  automate it. Must allow humans in every step, since I want to study the procedure of developing
  proper knowledge graphs.
- [Better than what?] In every aspect.
- This database architecture is good specifically for RAG, since it is the most expressive by itself.

## Vision

- I believe we can represent any system as data about the objects, the structures on them, and
  procedures about them. ArityComponent is responsible for the data of a group of objects, and the
  identities set is only responsible [for] matching an identity to memory. The structures are
  responsible for relations between identities and have nothing to know about data or memory; if
  [they did], that knowledge would be another arity and the order of the structure would [be]
  higher. There may be many structures that do the same thing but optimized in another way. The
  processes [procedures] are behaviors to change data or structure; they depend on some of the data
  and structure, and when run they change some of the data and some of the structure.
- My vision likely always was that we can express any database-like system (where data exists and
  filtering [is] done based on rules) as a few types of beings and structures across them.
- The structure must know only a dense set of identities each representing a single record, while
  the data about the identities are handled on tables named Arity. Since moving [the] rest of the
  records one block left each time we delete a record is unnecessary, the identities set handles
  identity<->memory mapping, where identity is a single record and memory is the address of memory
  where data related to it [is] stored.
- Some of the records actually can be about other records; those relations are a huge part of all
  the structures.
- Later I realized the addition of procedures would be cool and allow it to express more things,
  like feed forward and gradient descent backpropagation.
- [The NN example:] Two arities, one for (nodes) neurons and the other for the synapses
  (connections/weights). A structure that maps two identities from [the] arity of neurons to a single
  identity from [the] arity of synapses. A procedure that firstly clears every non input neuron's
  value by setting them to the bias (an identity from synapses that has two of the same identity from
  the neurons arity), then for each of the identities on the synapses arity multiplies the first
  neuron's value with the weight and adds it to the second neuron it points [to]; also when all the
  inputs of a neuron [are] calculated, its activation function can be calculated and replace the
  value on the neuron. There are other ways to describe a NN with this architecture; few of them are
  a bit better and more descriptive.
- This architecture can represent easily [a] database or a neural network, which makes me think this
  can represent anything; more importantly [it] does it with high expressiveness (because of the
  contracts), so we can mathematically reason about them.
- My architecture is FCA4J but as [a] database and multi-arity. *(2026-10-10)*

## Layers and names

*(Names as spoken at the time. The entry "For the naming, acse replaces the acs" sets the current
ones: the library is `acse`, the service program is `acses`; since 2026-10-13, ACS names the project
holding both. Before that entry, `asca`, `ASCA` and the service `acse` mean `acses`; `ACS` and `ASC`
mean `acse`; `acsr`, `asrc` and `ascar` are the requester.)*

- If we think about it, it [ACS] is a library. There are four layers: 1 library, 2 database
  application, 3 requester, 4 GUI or 4 Agent.
- The ACS is a library, the archiver (hereby "asca") is a database service, [the] requester (hereby
  "ascar") is an adaptor, [and] the GUI and agents will use the adaptor.
- Since asca is a service, it must have a way to manage it, while the requests can go through the
  ascar.
- The reason we need a separate application to request is because asca needs to load a lot of things
  to memory in order to make any change, so it will likely not be used as `asca components ~/db3`; in
  other words, it is likely the asca will be open for a time, so it cannot be used like
  `asca <command>` unless we make the main function check if the service is already open and move
  into the "command" mode, which is also a valid approach, but a separate application can serve as a
  protocol.
- I realised the idea of the main function of "asca" deciding to act as ascar if there is already an
  asca process running is actually nice, we can do it to reduce our work.
- I guess the new plan is to make a parent project that will compile all other projects in order to
  develop our applications.
- For now, let's make this project only two modules, ASC [the library] and ASCA [the service] (which
  can handle requests in many ways).
- We can also make modules for the OntoDirectory and alike.
- [Is the GUI OntoDirectory?] Yes, but needs improvements. Also we can make another GUI application
  that focuses on the RAG structure I planned.
- My suggestion for the ASCA, it must allow many ways of accepting requests, also it must be modular,
  being able to load .class files with ease, which is possible since the core is too thin.
- By "plugins" I meant components like arity, catalog or structure components, since they will have
  well defined contracts.
- Loading is simply loading classes as accessible components. Loading a class and using [it] are
  separate things, so loading folder and class methods must exist.
- Also if the command `asca boot` becomes the working instance, that command line cannot be used for
  anything else, so it must launch a separate process of asca.
- The expected use case is the "acse" [the service] calls will launch [the database engine] if and
  only if (boot is the command and there is not anyone running), otherwise the acse is a terminal
  application for bridging client and database engine for requests and responses.
- ACS is the library, it will not be usable on its own. The ACSE [the service] will implement few of
  the ACS Components itself, outside of the ACS module.
- For the naming, acse replaces the acs, and acses replaces the acse. Now it sounds like access, which
  is cool.
- [Every rename the handoff lists:] rename all. *(2026-10-13)*
- I will keep the name ACS for this project, the ACSES will be a (maven) module of this project, and
  ACSES will also be another sub module. Do not do it yet. I want to upload the messy version of this
  code base first for keeping its identity.

## Licence

- I would like a licence that is like a bomb and forces every one responsible to open source their
  code.
- I want people to be allowed to develop open source using this, but it must be commercial for the
  commercial use.
- [What is the cost of not using a well-known licence?] If it is near nothing, go with AGPL-3.0-only
  with a commercial licence. *(Claude's comparison and the result: `inferred-decisions.md` → The
  licence.)*
- [AGPL-3.0-only with the Universal FOSS Exception, plus a commercial licence:] Good, I want to use
  the first suggestion of you.
- [The Universal FOSS Exception:] Yes, add the exception.
- The exception is Oracle's Universal FOSS Exception, Version 1.0, referred to by name and by Oracle's
  link, which Oracle allows when referring to it, rather than an equivalent of our own: `LICENSE`
  holds the AGPL text alone, `NOTICE` the rest. *(Claude's wording; the author: "Do what you want,
  likely you know the legal side better than me.")* *(2026-10-13)*

## Process

- This is the plan: we code the classes to design; later we will add the boilerplate, like bulk
  access to columns in many types (int, long, double).
- We can later organize those [markdowns] into folders. We do not need it right now.
- For this project, urgent things are not urgent by time; they are urgent by being the requirements
  to other processes. They can become obsolete by decisions.

## Architecture

- Arities are the first type of element; structures are the second.
- "Component" replaces "Module" as the word for plugins, in every name.
- No more "Module", we already settled to "Component", so "Modular" becomes "Composable"; just be
  clear what composable means. *(2026-10-13)*
- I guess we better rename the architecture to "plugin" based, since the core can be excluded.
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
- The core handles creation-addition of plugins, delegations, journal (logging + undo/redo),
  persistence, filtering + analysis, also system analytics like how much RAM is used by/where, etc.
- [For `ontructure/`:] the project name became Arity Component System, so we will use "acs_beta"
  right now. [Since then: "acse replaces the acs", see Layers and names.]
- [Under `acs_beta`,] I plan on making a separate file for every top tier interface, ArityComponent
  variants and StructureComponent variants, and identitySet [`IdentitiesSet`]. Then packages for
  implementations. I prefer packages of "identifiers", "arity", "structures.<x>ary".
- [The `OE_` prefix, `OntologyEngineCore` and the package `org.halim.ontology`:] now the name is
  "ACS" [now `acse`], replace them with "ACS" [`acse`].
- [The ACS rename, now to `acse`:] do the one pass before the first push, so every import moves
  once. *(Claude's wording, confirmed by the author.)* [Since then: the messy version is uploaded
  first, see Layers and names.]
- "legacy" is read only. Renaming should not reach /legacy, but the `package <path>` header can
  change, no problem; for now we can keep renaming for the `incubation.ontructure`.
- [The `Module` at the end of `SetHierarchyDirectedAcyclicIntGraphModule`:] replace with
  `Component`.
- [Catalogs as components, `CatalogComponent`:] I accept it, but we must allow the client to decide
  to define catalogs without modules. I mean for each Arity we will have a table, and the client
  must be able to define [the] table itself or use a modular table. Also, for clarity, we will need
  to name them like "ArityCatalogTable" or "ArityModularCatalogTable" or "ArityCatalogRowComponent".
  [Since then: "Modular" becomes "Composable".]
- The `Structure` contract holds which arities it spans; removal notification and alike;
  per-element read and write of its own state; its op codes and codec. *(Old plan's wording,
  confirmed by the author.)*
- [The] Structure contract must allow a way of storing and loading the data via data streams; the
  core can manage files and alike.
- [Structures referring to their arities by ints, which the core resolves:] this has its own
  results. We cannot "insert" arities or structures, they become somewhat independent of core (any
  other core with different structures can accept it), using object references makes them less
  dependent on the core. So I am open to suggestions, but for now ints are enough.
- [The old plan's "growth is propagated directly from the arity to everything attached" is
  declined:] "different arity" == "different thing".
- Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. Floating narrowing rounds instead. *(Old
  plan's wording, confirmed by the author.)*
- [Gather, and resolving a catalog's name to its slot:] "scattered" is unlikely for the assumption,
  and [the] naming method is not necessary right now.
- We need to define many Structure Components. A ternary example would be assuming an arity is [the]
  cartesian [product] of two other arities; another would be assuming an arity is some transitions
  from the one arity to another.
- Not all the catalogs are meant to be pure array lists; they can be calculations using the identity
  in the arity.
- The structures ensure their rules themselves. *(2026-10-10)*
- Most importantly, I plan on adding a third type of components, the "procedures", which are
  functions about ACS that benefit being close to the system core.
- Procedures may and will change catalog data; it is like their goal, because they are meant to be
  close to the ACS. We need a way to store changes on catalog data, which is not hard, just extending
  the arity operations list.
- [Procedures, only for changes or also for reads:] it must allow a way of reasoning, or at least
  state they do not otherwise.
- [The synapse structure:] it is just a type of 2 arity structure where two elements from a single
  arity [are] matched to a single element on the second arity. All of the neurons on all the layers
  are placed on arity 1, and every weight is placed on arity 2; [the] structure resembles a category
  (in [the] mathematical sense), since there [are] two sets and [the] second set binds to [the]
  cartesian of the first set with [the] first set.
- There [are] many ways for two arity structures to exist, where a single arity structure is always a
  type of graph.

## Identities and removal

- I plan on using [the vacuum idea] where the index of an identity is converted to identity at the
  ArityModule's identitiesSet.
- If the identity number rather than the index is given (which is handled by the identities set of
  the ArityComponent), it auto skips the removed ones; we just need to specify that the intended
  behavior is using the arity component like that.
- When execution (through the multi-arity structures, and maybe few other cases I do not remember
  right now) reaches identities, they check if they exist, when necessary.
- [On the above:] the important detail is that the structures are also better off not shifting data,
  so they point at the index rather than the identity; by "they check when necessary" I mean we need
  to handle this phenomenon.
- [Index there is the memory address where the record's data sits, which does not shift on removal:]
  Yes, it is [that], but [it] would not cause any problem; the functions just need to transform the
  identity number before using it.
- I have a belief the structures do not need to check if an Identity exists.
- [Claude's wording, approved:] Make the core the only caller of removals.
- Structures, if they need to remove an identity on another arity, need to call ArityComponent's
  remove and [the] forget of [the] core on that identity; that is the recursive step: 0 original
  arity -> 1 related structures -> 2 start from (0) on each identity needed to [be] removed.
- Forget needs to be undoable.
- We will not need a function like "swap" or "captureNotForget" because of what an arity represents;
  more on that later.
- [Storing a structure:] recording the relation is enough. Also at the core level storing can be done
  by temporarily storing, then vacuum, then final storage. [That] is not bound to be the only
  [sequence]; allow [the] option of direct storing. Also each time [storing] needs to create a new
  file, so there needs [to be] a pointer.
- [Calculated arities:] the calculated arities must be dependent on a structure; this is better
  since being directly dependent [on] another Arity can actually raise errors.
- [Claude's `long captureIdentity(int arity, String comment)` on the core:] go with it.
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
- I think we do not need to make pending removals; the identity sets handle it.
- [For forget before capture-back:] identity set must allow a way of capturing the lowest positive
  excluded value. I mean there is an IntArrayList of excluded [identities]: [its] first element if
  the list is not empty, or just increasing the borders. So the identities set contract is
  responsible.
- We do not need to make a "truncate" right now. But later we can make this. Also, the vacuum (may
  rename to "compress") actually can simply swap the last few entries with the dead ones and
  truncate out the dead ones.
- [Undoing a remove after a vacuum is] out of scope right now; vacuum [is] planned to be the horizon
  of undo.
- [Naming the judgment-call existence method so it looks deliberate, like `identityAtUnchecked`:]
  this naming convention is good, but out of scope; this is boilerplate polishing.

## Journal and undo

- Some actions have an inverse that is among the other actions. Call it "invertible".
- Some actions recording the change is enough for calculating the undoing.
  Call it "inverse calculatable".
- Some actions are not undoable, snapshot taking is expensive se we need to allow "break undo history".
- "Persistence", saving to files, is just writing some data; no need of journal overhead.
- Sometimes a cluster of operations is [the] steps of a client request, and undoing may take a
  different approach. The example was: adding a parent may cause many remove parent calls, but
  requesting undo on those prior to undoing the add parent is auto rejected. We have two options:
  we can have request resolver functions that call the atomic operations after recording the
  beginning of the operations to the journal, or making them tell the inverse. [The] first option is
  likely better, so we are separating the client side of jobs from inside operations.
- I recently decided the "check and call checkless functions" pattern. Simply, there will be
  functions that resolve [a] client request into private reckless functions of unit operations.
- "addParent" under isTight cannot be inverted by undoing all in reversed order; it can be reverted
  by undoing all in the same order; also, it can be undone by closing the isTight temporarily.
- [Redo:] simple, re-execute all.
- [The DAG's free list, `freeSlots` and `multiData` length:] the slot allocation will likely be
  deterministic, and it feels like it won't matter.
- They [journal primitives] are meant to be the action and its parameters with an inverse.
- [The old plan's write barrier is declined; it] needs more details, explained here: for journal
  actions with checks and multi steps, a record at the beginning and end must be done, [and it]
  must contain a name for logging and alike; for steps, each must just contain parameters, itself
  and [its] inverse. More details can be added.
- A structure registers its op codes and a codec for its journal records. *(Old plan's wording,
  confirmed by the author.)*
- An operation declares its own logging mode; the client does not. *(Old plan's wording, confirmed
  by the author.)*
- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary; multithreading as a single writer with readers on a snapshot. *(Old plan's
  wording, confirmed by the author.)*
- [Legacy's `runAtomically`, one lock and one undo entry per client request:] now we will handle
  this with [the] journal; this is important.
- [Recording positions, so undo restores the order of parents:] no need, we are recording only
  relations.
- [Over proxies:] yes, journal wins.
- I plan on making the journal contract alongside the core contract.
- For every request about ACS, it must be able to store a comment, like the git commit messages, but
  for every action, because every action is made from a resource and later may be audited, which
  will require context.
- The comment will be given by the client, which is "asrc" [the requester], which can only request;
  steps are the application's responsibility.
- While your [Claude's] decision of throwing [when undo is called] during a request is sound, we also
  need to add an abort option.
- [Adding identities:] Yes, journal adding identities; I forgot to tell because it is assumable.
- [`storeAll`:] I have a small feeling it is appropriate here, but databases often use a "journal
  before" [write-ahead log] type of action; we can do it later.
- "storeAll" can be a JSON or something better, you can choose; it [is] meant to store every
  transaction as a log. Just one rule: each transaction is stored [in] its own section, not the list
  each.
- [For the "until" methods refusing the wrong direction:] we can add a checkout method for ease of use.
- [`abortRecordingActivity()`:] approved.
- For the boolean data of the journal we need to use bitset or roaring bitset, since [an] actual boolean
  array costs about 1 byte instead of 1 bit. *(2026-10-12)*
- [Catalog setters:] it is meant to be logged, and instead of storing [the] former change with the new
  one we can simply store the difference. If the client wants to store former and new versions for
  higher clarity, they can implement the Journal themselves, or we can make a separate Journal
  implementation that stores that way. *(2026-10-13)*
- [Since `new - old` does not restore a double exactly:] your [Claude's] "old ^ new" method [over the
  raw bits] solves the current problem with the journal.
- [The setter shape, and the logging mode for bulk writes:] not open, simply log the change using the
  method you gave [`old ^ new`].
- We can make the journal's creation independent of the core: `indexer.bin` names the journal's class
  beside the core's, a load builds the journal from that class through the folder's jar as it builds
  the core, and the core takes that journal instead of building `ACSE_JournalBasic` itself. *(Claude's
  wording, confirmed by the author.)*

## Descriptions

- [The description:] it must describe each instance of [a] component of the core using a string value;
  it must be able to be read and set. It is there so the agent can state the purpose of each
  component. *(2026-10-11)*
- It [the description] is somewhat of a journal itself, so no need of journalling, except while
  creating and adding we can require the requests take another string parameter for the description.
- Descriptions are few strings, and do not take much space; we can represent them in the easiest way.

## Persistence

- I want you to properly handle the persistence using methods in the core (and contracts where
  necessary) that take a target folder and store everything in their files. *(2026-10-11)*
- [Instead of a no-argument constructor and fields that stop being final:] make a public static
  method, and an indexer.bin that tells each class. *(2026-10-12)*
- [The numbered folders 000001/, 000002/, …] are cool, but it shouldn't [be] the responsibility of
  this class [the core]; make an "ACSE_PersistenceManager" class that loads or stores in new folders
  each time.
- [The persistence manager:] it would be so cool if it was able to store all the classes as a jar file
  for easier loading.
- If space is not concerning, adding the jar for each save for locality of behaviour is a good idea;
  it is what Gradle does.
- [A class without a static loadFrom:] leave it out, because if it has not a method of saving, likely
  it doesn't need to persist. Just warn about it.
- We can make a persistence manager contract, and the persistence manager class will [be] in the root
  folder.
- We can make the persistence manager also variable. *(2026-10-13)*
- [Linking unchanged files between stores:] that is version control, you sure we should do it? I
  wanted to keep the database somewhat simple.
- I think we will not change this database once it is done, I mean since I am not planning to provide
  this as a database library in near time, I guess we can handle everything about the class updates
  with remembering to bump the FORMAT; after all, if we will support former versions we will need to
  store all of the former version classes, which is hard for version control.
- I gave up the idea for [the] ability to change base classes; if [a] user wants to use a different
  class, it can just decide to.
- The expected use case won't have multiple programs. One program that will be open as a service,
  [the] rest are requests.
- Let's include Roaring as a dependency.
- Now the new plan is acses will work as a bootstrapper when loading databases, I mean even the base
  classes will move to the target workspace; the acses will be responsible for loading them using the
  convention.
- The acses is thin: if there is already a process of acses running it will only be a requester; if
  not it will become the process.
- When requested to load a database, acses will use only classes from the database; acses only knows
  the contracts, so [the] database is the authority about implementation.
- When acses is going to create a new database, the acses will provide the base class files only
  once, during [the] creation of the database.
- [The two cases:]
  > Acses given a task to create a database in some path: ACSES creates (by copying from the device
  > config or equals) the acse-persistence.jar file, loads it. Then [it] loads the base implementation
  > classes from [the] device config file (or whatever it is called). Then [it] creates an empty core,
  > then saves everything with persistence.
  >
  > Acses given a task to load a database: it loads the acse-persistence.jar file on the target path.
  > [The] rest is handled by the persistence manager instance found there: the persistence manager
  > finds the current folder from the acse-persistence.data (hereby, for less confusion). It loads the
  > jar on the target folder, and creates the core instance. [The] core instance loads all the
  > necessary instances using the static load methods of the classes in the jar. [The] persistence
  > manager returns the core object. The acses service records the core with a name; if [the] request
  > didn't give it with "--calledBy <name>", the name of the folder that contains the
  > acse-persistence.jar file is the name.

## Identity sets

- Add PositiveLong and [Positive]Int, also deleting the narrow variants, to the plan.
- Right now the identitiesSet doesn't need to hold a "firstIndex"; doesn't really matter though.
- Some arities may have elements up to a certain number easily computed, like say we have a single
  layer fully [connected] perceptron: its nodes are one arity, its weights are a different arity,
  and its count and range can be easily computed, so the client can implement a specific identity
  set if it wants.
- Right now the engine accepts such a thing with ease, so we just need to document it. Later we may
  need to make a few additions for it to work.
- I think we need to find ways to define arity sizes; for example, [in] the "cartesian" instance I
  told, the arity that represents the cartesians can have its size calculated.

## Filtering

- The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like "namedLike" or "childOf". Also this is essentially a query service.
- Right now the filtering service can only provide the logical operators, and filters on data depend
  on the client. This is somewhat intended, since we allowed the data to vary, but some
  filtering modules to simplify the client's experience must be done.
- Cool names for "OR" variants like "IMPLIES", all are redundant. Keep logic minimal right now.
- They [the OR variants] are boilerplate, meant to be handled last.



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

## Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **Identities are dense and local to an arity**, minted from 0, so their values stay small. Ends if
  identities become global across arities or carry generation counters.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
- **Modularity the fullest** The engine must be able to express anything.
- [The DAG is] optimized for the case where the leaves are more than branch nodes, and parent*child
  count mostly does not exceed a constant. *(Javadoc of `SetHierarchyDirectedAcyclicIntGraphModule`.)*
