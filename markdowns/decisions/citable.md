# Citable

The single source of truth (`CLAUDE.md` → Decisions). Above Unconfirmed: the author's words, never
cut, and the inferences the author confirmed. Under Unconfirmed, at the end: the agents' inferences.

## Goal

- ACS is meant to be a database that can be managed with commands. Specifically, the client is meant
  to start the "ACS engine", then must use the "ACS requester" as: `acses boot` (starts the acs
  application), `acses loadDB ~/main --calledBy main` (loads the database in the path main and allows
  access to it with the name of main), `acses components main` (shows the components of the
  database). [The names changed afterwards: see Layers and names.] *(The author wrote "acsr"; renamed
  to "acses" at the author's request. The author wrote "as main"; changed to "--calledBy main" at the
  author's request, 2026-10-15, to match Persistence.)*
- Then we will develop a GUI for that application that uses the terminal input (later we can isolate
  it from the terminal); also we will build an AI Agent that will use acses. That is the goal.
  *(The author wrote "acsr"; renamed to "acses" at the author's request.)*
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
- This database architecture is good specifically for RAG, since it is the most expressive by itself.
- I have a vision of building a huge data base of nearly 512GB text using agents and ACSES.
  *(2026-10-18)*
- [ACS-alHikmet:] it is not the house of wisdom but ACS of it, also it actually doesn't contain the
  entire 512GB text data base but the methodology of building it. *(The author wrote "ACS al-Hitmek".)*
  *(2026-10-18)*

## Vision

- I believe we can represent any system as data about the objects, the structures on them, and
  procedures about them. ArityComponent is responsible for the data of a group of objects. Primary
  goal of the Identities set is to match an identity to memory, while being also responsible for set
  operations. The structures are responsible for relations between identities and have nothing to
  know about data or record data; if [they did], that knowledge would be another arity and the order
  of the structure would [be] higher. There may be many structures that do the same thing but
  optimized in another way. The processes [procedures] are behaviors to change data or structure;
  they depend on some of the data and structure, and when run they change some of the data and some
  of the structure. *(The author wrote "memory" and replaced it with "record data", 2026-10-15. The
  author restated the identities set's part, replacing "only responsible [for] matching", 2026-10-18.)*
- My vision likely always was that we can express any database-like system (where data exists and
  filtering [is] done based on rules) as a few types of beings and structures across them.
- The structure must know only a mostly contiguous set of identities each representing a single
  record, while the data about the identities are handled on tables named Arity. Since moving [the]
  rest of the records one block left each time we delete a record is unnecessary, the identities set
  handles identity<->memory mapping, where identity is a single record and memory is the address of
  memory where data related to it [is] stored. *(The author wrote "dense", meaning "mostly full with
  few gaps", 2026-10-15.)*
- Some of the records actually can be about other records; those relations are a huge part of all
  the structures.
- Later I realized the addition of procedures would be cool and allow it to express more things,
  like feed forward and gradient descent backpropagation.
- [The NN example:] Two arities, one for (nodes) neurons and the other for the synapses
  (connections/weights). A procedure that firstly clears every non input neuron's value by setting
  them to the bias (an identity from synapses that has two of the same identity from the neurons
  arity), then for each of the identities on the synapses arity multiplies the first neuron's value
  with the weight and adds it to the second neuron it points [to]; also when all the inputs of a
  neuron [are] calculated, its activation function can be calculated and replace the value on the
  neuron. There are other ways to describe a NN with this architecture; few of them are a bit better
  and more descriptive.
- This architecture can represent easily [a] database or a neural network, which makes me think this
  can represent anything; more importantly [it] does it with high expressiveness (because of the
  contracts), so we can mathematically reason about them.
- My architecture is FCA4J but as [a] database and multi-arity. *(2026-10-10)*

## Layers and names

*(Names as spoken at the time. The entry "For the naming, acse replaces the acs" sets the current
ones: the library is `acse`, the service program is `acses`; since 2026-10-14, ACS names the system,
`acse` implements it and `acses` uses it for the database service. Before that entry, `asca`, `ASCA` and the service `acse` mean `acses`; `ACS` and `ASC`
mean `acse`; `acsr`, `asrc` and `ascar` are the requester.)*

- There are four layers: 1 library, 2 database application, 3 requester, 4 GUI or 4 Agent.
- The archiver (hereby "asca") is a database service, [the] requester (hereby "ascar") is an
  adaptor, [and] the GUI and agents will use the adaptor.
- We killed the ascr [the requester] name and [the] requesting through a file idea. *(2026-10-14)*
- I guess the new plan is to make a parent project that will compile all other projects in order to
  develop our applications.
- [The project ACS has] six submodules: ACS [the contracts module], ACSE, ACSES, OntoDirectory,
  ACS-alHikmet, ACSAH viewer (a new project, which is like OntoDirectory but viewing the results of
  ACS-alHikmet) [: the other GUI application that focuses on the RAG structure I planned].
  *(2026-10-18)*
- My better wording for the inferences: The ACS Project: *(2026-10-18)*
  - Vision of the ACS related projects and the goal, also all the Important ACS projects.
    ACS: The contract interfaces, since they elegantly formalize thing into modules.
    ACSE: Few good implementations of the ACS contracts for solidifying the project.
    ACSES: A client side ACSE implementation for managing ACS based databases. ACSE contains few default implementations.
    ACS-alHikmet: Agents for building and auditing an ACSES database of paragraphs.
    ACSAH viewer: A GUI application like OntoDirectory to analyze the product of ACS-alHikmet.
    OntoDirectory: Where it all started, aimed to be integrated with ACSE instead of ontology package int it's source code, later can be implemented with AI agents like ACS-alHikmet.
- [Is the GUI OntoDirectory?] Yes, but needs improvements.
- Right now we do not make access control or cyber security, out of the scope for MVP. *(2026-10-14)*
- ACS [`acse`] is the library, it will not be usable on its own.
- "It will not be usable on its own" means no working database comes from `acse` alone, since `acses`
  runs it, but a Java program can still use `acse` directly. *(Author: "(q1 a)".)* *(Claude's
  reading, confirmed by the author on 2026-10-18: "I read and confirm it.")*
- For the naming, acse replaces the acs, and acses replaces the acse. Now it sounds like access, which
  is cool.
- The ACS is a system, as it names; ACSE is its implementation, and ACSES uses it for [the] database
  service. *(2026-10-14)*
- [On renaming to ACC, Arity Component Composer:] In terms of ECS's naming, the appropriate name for
  the system would be ASP (Arity-Structure-Procedure), which is likely trademarked. ASPCC would be
  the full name of the system since it describes exactly what it does. We better go with acs, also
  do it as "OE_ -> ACS_". *(2026-10-14)*

### Details

*(Old names: see the note under Layers and names.)*

- Since asca is a service, it must have a way to manage it, while the requests can go through the
  ascar.
- Asca needs to load a lot of things to memory in order to make any change, so it will likely not be
  used as `asca components ~/db3`; in other words, it is likely the asca will be open for a time.
- My suggestion for the ASCA, it must allow many ways of accepting requests, also it must be modular,
  being able to load .class files with ease, which is possible since the core is too thin.
- Loading is simply loading classes as accessible components. Loading a class and using [it] are
  separate things, so loading folder and class methods must exist.
- [Which ways of accepting requests may carry the management and loading requests:] right now there
  is not a privilege system on the access, so the answer is the acses. *(2026-10-14)*
- Also if the command `asca boot` becomes the working instance, that command line cannot be used for
  anything else, so it must launch a separate process of asca.
- The expected use case is the "acse" [the service] calls will launch [the database engine] if and
  only if (boot is the command and there is not anyone running), otherwise the acse is a terminal
  application for bridging client and database engine for requests and responses.
- If not booted and not boot requested, the engine fails loud and fast. *(2026-10-14)*
- [The prefixes:] [The] package becomes acse but the interfaces start with ACS, except the core,
  journal and most importantly the persistence manager. ACS is the idea, engine is the database
  side. *(2026-10-14)*

## Licence

- I would like a licence that is like a bomb and forces every one responsible to open source their
  code.
- I want people to be allowed to develop open source using this, but it must be commercial for the
  commercial use.
- [What is the cost of not using a well-known licence?] If it is near nothing, go with AGPL-3.0-only
  with a commercial licence. *(Claude's comparison and the result: `inferred-decisions.md` → The
  licence, in git at `6f32a25`.)*
- [AGPL-3.0-only with the Universal FOSS Exception, plus a commercial licence:] Good, I want to use
  the first suggestion of you.
- The exception is Oracle's Universal FOSS Exception, Version 1.0, referred to by name and by Oracle's
  link, which Oracle allows when referring to it, rather than an equivalent of our own: `LICENSE`
  holds the AGPL text alone, `NOTICE` the rest. *(Claude's wording; the author: "Do what you want,
  likely you know the legal side better than me.")* *(2026-10-13)*

## Process

- This is the plan: we code the classes to design; later we will add the boilerplate, like bulk
  access to columns in many types (int, long, double).
- [How to organize `decisions.md`: general entries stay in `decisions.md`, which is always read;
  niche ones go to `decisions/<topic>.md`, read when working on that topic:] I prefer that for
  clustering. *(2026-10-15)* [Since then: one Citable file, see `CLAUDE.md` → Decisions.]
- I want the inferred decisions and [the] rest of the decisions to stay under the same path
  [`markdowns/decisions/`]. *(2026-10-18)*
- For this project, urgent things are not urgent by time; they are urgent by being the requirements
  to other processes. They can become obsolete by decisions.

## Architecture

- Arities are the first type of element.
- No more "Module", we already settled to "Component", so "Modular" becomes "Composable"; just be
  clear what composable means. *(2026-10-13)*
- Composable is the system's vocabulary; plugin based is an architecture we left because [the] core
  is too thin for calling those components as plugin[s]. *(2026-10-15)*
- The "Structure" interface meant to be the second type of elements (the relations
  around sorts of elements (arity)) in the "OntologyEngineCore".
- The core handles the data base side; even though the components are able to delegate themselves,
  they must pass it through the core for the future plans. *(2026-10-15)*
- Right now the core is almost redundant since arities and structures can work on their own, but for
  the future purposes core stays; it also makes the code more organized. The ACS doesn't have a core,
  the ACSE has the core. *(2026-10-15)*
- The former version's missing features were critical for the application. I need the modularity and
  "Logging, Undo-Redo, ..." at the same time.
- The core handles creation-addition of components, delegations, journal (logging + undo/redo),
  persistence, filtering + analysis, also system analytics like how much RAM is used by/where, etc.
  *(The author wrote "plugins"; renamed to "components" at the author's request.)*
- "legacy" is read only. Renaming should not reach /legacy, but the `package <path>` header can
  change, no problem; for now we can keep renaming for the `incubation.ontructure`.
- [Structures referring to their arities by ints, which the core resolves:] this has its own
  results. We cannot "insert" arities or structures, they become somewhat independent of core (any
  other core with different structures can accept it), using object references makes them less
  dependent on the core. So I am open to suggestions, but for now ints are enough.
- One idea raises: the components can use object pointers to each other instead of ints. We do not go
  with that idea because the journalling side must be handled with the core, but more importantly, in
  the future we may want to add features that reason about the multiple of arity, structure and
  procedure instances. *(2026-10-15)*
- [The old plan's "growth is propagated directly from the arity to everything attached" is
  declined:] "different arity" == "different thing".
- Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. Floating narrowing rounds instead. *(Old
  plan's wording, confirmed by the author.)*
- We need to define many Structure Components. A ternary example would be assuming an arity is [the]
  cartesian [product] of two other arities; another would be assuming an arity is some transitions
  from the one arity to another.
- The structures ensure their rules themselves. *(2026-10-10)*
- Most importantly, I plan on adding a third type of components, the "procedures", which are
  functions about ACS that benefit being close to the system core.
- Procedures may and will change catalog data; it is like their goal, because they are meant to be
  close to the ACS. We need a way to store changes on catalog data, which is not hard, just extending
  the arity operations list.
- [Procedures, only for changes or also for reads:] it must allow a way of reasoning, or at least
  state they do not otherwise.
- The procedures must be so clear they can be automatically reasoned with our ACS system.
  *(2026-10-15)*

### Details

- The core must also have contracts about delegation; we can (right now) declare an interface [for
  them].
- [Under `acs_beta`,] I plan on making a separate file for every top tier interface, ArityComponent
  variants and StructureComponent variants, and identitySet [`IdentitiesSet`]. Then packages for
  implementations. I prefer packages of "identifiers", "arity", "structures.<x>ary".
- [Catalogs as components, `CatalogComponent`:] I accept it, but we must allow the client to decide
  to define catalogs without modules. I mean for each Arity we will have a table, and the client
  must be able to define [the] table itself or use a modular table. Also, for clarity, we will need
  to name them like "ArityCatalogTable" or "ArityModularCatalogTable" or "ArityCatalogRowComponent".
  [Since then: "Modular" becomes "Composable".]
- The `Structure` contract holds which arities it spans; removal notification and alike; per-element
  read and write of its own state. *(Old plan's wording, confirmed by the author.)*
- [The] Structure contract must allow a way of storing and loading the data via data streams; the
  core can manage files and alike.
- [Gather, and resolving a catalog's name to its slot:] "scattered" is unlikely for the assumption,
  and [the] naming method is not necessary right now.
- Not all the catalogs are meant to be pure array lists; they can be calculations using the identity
  in the arity.
- [The synapse structure:] it is just a type of 2 arity structure where two elements from a single
  arity [are] matched to a single element on the second arity. All of the neurons on all the layers
  are placed on arity 1, and every weight is placed on arity 2; [the] structure resembles a category
  (in [the] mathematical sense), since there [are] two sets and [the] second set binds to [the]
  cartesian of the first set with [the] first set.
- There [are] many ways for two arity structures to exist, where a single arity structure is always a
  type of graph.

## Identities and removal

- Core is the only caller of removal requests (from outside), and arity is the one removing, that also
  delegates the message through the core. *(2026-10-15)*
- The component will depend less on the core; arities remove their elements on their own.
  *(2026-10-15)*
- Structures are responsible for representing the data optimally for memory and time, while the imv
  values can be removed or structures around them change. *(2026-10-15)*
- Journalling removed the necessity of separating forgetting from removal, so hereby data is
  forgotten (but journalled) the moment an element is removed. *(2026-10-15)*

### Details

*("vacuum" in this section is renamed to "compacten" at the author's request, 2026-10-15.)*

- I plan on using the compacten idea where the index of an identity is converted to identity at the
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
- Structures, if they need to remove an identity on another arity, need to call ArityComponent's
  remove on that identity; that is the recursive step: 0 original arity -> 1 related structures -> 2
  start from (0) on each identity needed to [be] removed.
- Arity removes the identity and calls the core forget for delegating it. *(2026-10-15)*
- Forget needs to be undoable.
- We will not need a function like "swap" or "captureNotForget" because of what an arity represents;
  more on that later.
- [Storing a structure:] recording the relation is enough. Also at the core level storing can be
  done by temporarily storing, then compacten, then final storage. [That] is not bound to be the only
  [sequence]; allow [the] option of direct storing.
- [Calculated arities:] the calculated arities must be dependent on a structure; this is better
  since being directly dependent [on] another Arity can actually raise errors.
- The removal and binary search [over the hole list] likely will never get that slow.
- Structures are not responsible for the element count; it is just the structure add-on.
- I think the compacten will be a rare call, on storing to disk and reloading from it.
- We must make a "forget" call that removes the associated data (like the connections on the indexer)
  but doesn't shift all the identities. Delegations over that call will be handled by the core.
- No structure compactens a dead identity it reaches on its own.
- Bulk catalog reads intersect the range with the occupied set once, then read, instead of checking
  each element.
- We need to allow client to skip the validation of identities, when they likely know it.
- The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. If checking is requested by client.
- I think we do not need to make pending removals; the identity sets handle it.
- [Capturing back:] identity set must allow a way of capturing the lowest positive
  excluded value. I mean there is an IntArrayList of excluded [identities]: [its] first element if
  the list is not empty, or just increasing the borders. So the identities set contract is
  responsible.
- We do not need to make a "truncate" right now. But later we can make this. Also, the compacten (may
  rename to "compress") actually can simply swap the last few entries with the dead ones and
  truncate out the dead ones.
- [Undoing a remove after a compacten is] out of scope right now; compacten [is] planned to be the
  horizon of undo.

## Journal and undo

- I recently decided the "check and call checkless functions" pattern. Simply, there will be
  functions that resolve [a] client request into private reckless functions of unit operations.
- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary; multithreading as a single writer with readers on a snapshot. *(Old plan's
  wording, confirmed by the author.)*
- For every request about ACS, it must be able to store a comment, like the git commit messages, but
  for every action, because every action is made from a resource and later may be audited, which
  will require context.

### Details

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
- "addParent" under isTight cannot be inverted by undoing all in reversed order; it can be reverted
  by undoing all in the same order; also, it can be undone by closing the isTight temporarily.
- [The old plan's write barrier is declined; it] needs more details, explained here: for journal
  actions with checks and multi steps, a record at the beginning and end must be done, [and it]
  must contain a name for logging and alike; for steps, each must just contain parameters, itself
  and [its] inverse. More details can be added.
- A structure registers its op codes and a codec for its journal records. *(Old plan's wording,
  confirmed by the author.)*
- An operation declares its own logging mode; the client does not. *(Old plan's wording, confirmed
  by the author.)*
- [Recording positions, so undo restores the order of parents:] no need, we are recording only
  relations.
- I plan on making the journal contract alongside the core contract.
- The comment will be given by the client, which is "acses" [the requester], which can only request;
  steps are the application's responsibility. *(The author wrote "asrc"; renamed to "acses" at the
  author's request.)*
- While your [Claude's] decision of throwing [when undo is called] during a request is sound, we also
  need to add an abort option.
- [Adding identities:] Yes, journal adding identities; I forgot to tell because it is assumable.
- [`storeAll`:] I have a small feeling it is appropriate here, but databases often use a "journal
  before" [write-ahead log] type of action; we can do it later.
- "storeAll" can be a JSON or something better, you can choose; it [is] meant to store every
  transaction as a log. Just one rule: each transaction is stored [in] its own section, not the list
  each.
- For the boolean data of the journal we need to use bitset or roaring bitset, since [an] actual boolean
  array costs about 1 byte instead of 1 bit. *(2026-10-12)*
- [Catalog setters:] it is meant to be logged, and instead of storing [the] former change with the new
  one we can simply store the difference. If the client wants to store former and new versions for
  higher clarity, they can implement the Journal themselves, or we can make a separate Journal
  implementation that stores that way. *(2026-10-13)*
- Decided to store `new ^ old` for catalog rows that do not change in the number of bits.
  *(2026-10-15)*
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
- Also classes can have a description. For an example, there can be many different implementations
  of mathematically [the] same indexing logic (say two arities, one element from arity one is
  connected to one element from another arity or not) but different ways of representing them,
  performant for different cases. The agent [is] meant to choose between them; also the agent can
  decompile the class files to make its own descriptions. *(2026-10-14)*

### Details

- It [the description] is somewhat of a journal itself, so no need of journalling, except while
  creating and adding we can require the requests take another string parameter for the description.
- Descriptions are few strings, and do not take much space; we can represent them in the easiest way.

## Persistence

- I think we will not change this database once it is done, I mean since I am not planning to provide
  this as a database library in near time, I guess we can handle everything about the class updates
  with remembering to bump the FORMAT; after all, if we will support former versions we will need to
  store all of the former version classes, which is hard for version control.
- The expected use case won't have multiple programs. One program that will be open as a service,
  [the] rest are requests.
- Let's include Roaring as a dependency.
- When requested to load a database, acses will use only classes from the database; acses only knows
  the contracts, so [the] database is the authority about implementation.

### Details

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
- I gave up the idea for [the] ability to change base classes; if [a] user wants to use a different
  class, it can just decide to.
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
- [The device config:] I mean the initial implementation classes we are creating here will be stored
  for the ACSES application; the user will run the ACSES application to create a database anywhere it
  wants, then the ACSES application will copy class files on its side; using the "config" folder is
  not obligatory. *(2026-10-14)*

## Identity sets

### Details

- Add [Positive]Int, also deleting the narrow variants, to the plan.
- Some arities may have elements up to a certain number easily computed, like say we have a single
  layer fully [connected] perceptron: its nodes are one arity, its weights are a different arity,
  and its count and range can be easily computed, so the client can implement a specific identity
  set if it wants.
- Right now the engine accepts such a thing with ease, so we just need to document it. Later we may
  need to make a few additions for it to work.
- I think we need to find ways to define arity sizes; for example, [in] the "cartesian" instance I
  told, the arity that represents the cartesians can have its size calculated.
- [May identities be long?] Long imv is out of scope, but yes they can. *(2026-10-15)*

## Filtering

- The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like "namedLike" or "childOf". Also this is essentially a query service.

### Details

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
- **Arity** (noun) — one identity space: the occupied set plus its columns. As a number, how many
  identity spaces a structure spans.
- **Catalog** — one value per identity; a column. Total: an unwritten slot reads as zero, not absent.
- **Structure** — data about relations, over one arity or several, under any rule.
- **Component** — an arity, structure or procedure the core holds (`ArityComponent`,
  `StructureComponent`).
- The identity is [the] element identity; it is represented by an int named value, [and] its index
  is [the] number of identities that have less value than its. We no longer use the words "identity"
  or "index", since they are not distinctive enough; [the] names are "Identity's uniqueness index"
  (iui for short) and "Identity's memory value" (imv for short). To memorize them easily we record
  the notation "iui <= imv". Structures use the imv since it is stable, and sets map the iui to imv
  since iui is the actual identity. Vacuum
  (hereby compacten) makes all the iui equal their imv. *(2026-10-15)* *(The author wrote "compress"
  and corrected it to "compacten".)*
- Hereby we use "compacten" (even if illegal grammatically), which stands for removing unnecessary
  space consumption without changing the structure of data. *(2026-10-15)*

## Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **iui are dense and imv are local to an arity**, minted from 0, so their values stay small.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
- **Modularity the fullest** The engine must be able to express anything.

## Unconfirmed

Agents append new inferences here (`CLAUDE.md` → Decisions).

- Later, in `acses`: commands that interact with the arities and that query data, and custom class
  files handled properly, with an option mechanism beside a list function. *(Author, 2026-10-18, in
  `plan.md` → P2: "Since the database is really modular, and we made the Composable Arity instance, we
  must implement a way of interacting with the arities. Also, there must be methods of querying
  data."; answering P2: "since we plan allowing custom class files, we need to handle it properly,
  but later. Certainly an option mechanism along side a list function necessary.")*
- The interfaces in `acs` start with `ACS_`, the core, journal, persistence manager and description
  manager included. *(Author, 2026-10-18: "Hereby interfaces in the acs start by ACS_ instead.")*
  Conflicts: `citable.md` → Layers and names → [The prefixes:] (2026-10-14) would lose "except the core,
  journal and most importantly the persistence manager". Commons: "do it as "OE_ -> ACS_""
  (`citable.md` → Layers and names). Complements: "ACS: The contract interfaces" (`citable.md` → Layers
  and names). `citable.md` → Persistence's
  "make an "ACSE_PersistenceManager" class" keeps the name it had.
- A structure holds only the connections, never data of its elements: whatever a connection means
  beyond the connection is an entity, so it gets an identity in an arity, which is why
  `ACS_NodeEdgeComponentBase` exists. A unary structure is a directed graph on one arity's identities,
  each kind a restriction of it. *(Author, 2026-10-19: "structures meant to hold only structure data,
  not the data of the elements of a structure. And the ACS assumes every entity we care about belongs
  to one single identity and if the connections represent anything else then the structre, they must
  belong to an Arity, that's why the ACS_NodeEdgeComponentBase is created."; "About the unary
  structures, they are always a graph, only restrictions can exist, for an example a sequence is a
  directed graph with the limitation of each node having a "from" and a "to".")* Complements:
  "Structure — data about relations, over one arity or several, under any rule" (`citable.md` →
  Words)
  gains what a structure may not hold; "recording the relation is enough" (storing a structure) and
  "optimally for memory and time" (`citable.md` → Identities and removal) agree. No conflict found in the summaries.
- Every component is an actor: a separate interface, named like `ACS_Actor`, replaces
  `TraceableForUndoingRedoing`, and exposes requests to the outside through `call` and its bindings,
  steps to the journal through `doTraceableStep`. *(Author, 2026-10-19: "Looks like the interface
  contracts for components must allow a way to sign their allowed methods with bindings, and a call
  method. Because if you think about it, the ACS contracts are for the management of ACS not its data.
  So if a component allows things outside the responsibilities of ACS it must allow it through a "call"
  like method. This likely fixes all."; "The "doTraceableStep" is similar to what we want. The
  difference is, instead of steps the requests are registered. Then the class name better become
  "TraceableActor" since the classes that implement it are actors that expose some methods to outside
  and some other methods to Journal. This is elegant, we need to make the `TraceableForUndoingRedoing`
  a separate interface with its own name like "ACS_Actor""; "The `doTraceableStep` will be part of
  ACS_Actor")* Commons: "the "check and call checkless functions" pattern" and "can only request; steps are
  the application's responsibility" (`citable.md` → Journal and undo). Complements: "registers its op codes
  and a codec" gains registered requests beside the op codes. No conflict found in the summaries.
- The journal is an actor too: its `call` likely holds the inspection refusal, and its
  `doTraceableStep` throws an illegal-action error saying the feature is out of MVP. Being an actor
  allows a multi-branch timeline journal later; that, and the journal's movement handling, are out of
  scope. *(Author, 2026-10-19: "even the journal can implement it, since it exposes some methods to the
  user."; "The journal is movement handling is about multi branching and right now out of scope.";
  "The Journal becoming an actor which is trackable is an abstraction that means we can develop a multi
  branch timeline journal, but not necessary right now, completely out of scope."; "The refuse because
  of "inspection" will likely also be handled inside of the journal's call method"; "the Journal will
  simply make it return an illegal action error stating (This feature is out of MVP).")* No related
  entry found in the summaries.
- No CQRS (Command Query Responsibility Segregation: writes and reads on separate paths) for now,
  since every action returns a result. *(Author, 2026-10-19: "Every action returns a result, so we do
  not need CQRS right now.")* No related entry found in the summaries.
- During an inspection (the rollback `inspectAt`) the journal refuses a write loudly; a command
  waiting for the return answers with the current activity index and the count of activities after
  it. *(Author, 2026-10-19: "Refuse write loudly, and add a command to "wait" for returning to the
  former version, the command returns the current activity index and how many activities are
  pending."; pending picked among Claude's options: "Activities after the current point".)*
  Complements: "break undo history" (`citable.md` → Journal and undo), which an inspection cannot move past.
- The return from an inspection takes no argument and is `endInspection`. *(Author, 2026-10-19:
  "Int in "uncheck" was a mistake."; `endInspection` picked among Claude's options.)*
- `storeDB toPath=` stores a copy elsewhere and is never refused for an inspection; a plain `storeDB`
  is refused during one unless `+ignoreInspection`. *(Author, 2026-10-19: "the "storeDB" command must
  have an optional "--toPath" parameter for specifying the store path, so an agent can store a
  checkout version somewhere else if it wants."; "Yes, refuse the plain storeDB except if there is
  "--ignoreCheckOut"."; "The --toPath is a copy".)*
- `migrate` moves a database to a new folder, with `newName=`, `+WithFormerVersions`,
  `+WithOtherFiles` (every other entry at the root) and `+deleteOld` (Claude's name); it is refused
  during an inspection unless `+ignoreInspection`, and the database then uses the core loaded back
  from the new folder. *(Author, 2026-10-19: "we can make a "migrate" command, which has an optional
  flag for "--AllFiles" (files on the folder root not recognized by ACS) and "--newName <newName>"
  for giving it a new name."; on deleting the old folder and copying all or the current state only:
  "Make it a command flag"; "I want a "--WithFormerVersions" and an "--WithOtherFiles" flag that does
  what they say."; "Every file at the root of the database."; the loaded-back core picked among
  Claude's options.)* Conflicts: `plan.md` → P9's `--AllFiles` and `--currentOnly`, replaced; the
  newer is taken.
- ACSES passes requests to the core; those commands are planned in another session. *(Author,
  2026-10-19: "as the goal it gives commands to the Core with the request comment and returns what
  Core returns."; "I am planning them in another session.")* Commons: "Later, in `acses`: commands
  that interact with the arities" (above).
- The rollback is `inspectAt`, and the wait splits in two: `inspectionState` probes at once,
  `waitInspectionReturn` holds until the return. *(Author, 2026-10-19: "The name "wait" is vauge and
  can do two different things, I want two commands with better names."; "The checkout command (used
  to) mean rolling back to a certain state of a database, and I found the name inspectAt better than
  it since it is a serious reroll."; "If the goal is to probe state of inspection, go with 2";
  `waitInspectionReturn` picked as the spelling.)*
- `beginSession` sends nothing until `Commit`, which sends the queue together; the first refusal
  stops the rest unless `+continueOnRefusal`; values may be quoted with `"`, `` ` ``, ```` ``` ```` or
  `"""`, the last two across lines. *(Author, 2026-10-19: ""beginSession" (which means the terminal
  keep recieving until "Commit" is called, like a python terminal idle)"; "The session doesn't send
  anything until commit, then all request will be queued togather."; "Make the stop default and go
  on a command flag."; ````"The commands must allow value names typed in `"` or "`" and "```" (multi
  line) or `"""`."````; Commit ending the prompt only and the flag's name picked among Claude's
  options.)*
- `acquire` gives a key that every request it covers must give as its first argument, for
  operational safety, held in the daemon's memory only; its scope and `+changesOnly` are flags, and
  it ends with `release`, or `release +force` without the key. *(Author, 2026-10-19: "a "lock"
  command wich returns a key and for the rest of the requests will require the first argument to be
  the key, not for security but operational safety."; "Make all of the options available as command
  flags."; "b: good, we assume the requester has good intentions, with only a change to
  malfunction"; "c: No, locks are not mandotary, if the data base fails mean time the lock better get
  reaccuired"; "d: Yes, other solutions will be for other cases"; "I got the "lock" from mutex
  instances."; acquire and release picked among Claude's options.)*
- `loadDB` of a folder another name holds needs `+Duplicate`; given where nothing else holds the
  folder, it only warns. *(Author, 2026-10-19: "a: make it mandotary to have the flag called
  "duplicate", if there is duplicate but not another instance pointing on it, only warn.")*
- Switches take the shape `+Name` and valued flags `name=value`, so no name is mistaken for a flag.
  *(Author, 2026-10-19: "we need to ensure no name starts with "--" or find a better flag prefix.";
  "flags cannot become parameters but they can take the shape +Duplicate, and parameter flags can
  become toPath=, so cool.")* Conflicts: `plan.md` → P5's `--` flags, replaced; the newer is taken.
- An actor's `call` takes a `DataInputStream`: objects cost too much on a database of 2^25 or more
  records, and a method takes any number of inputs of any type. The author is open to a better
  solution. *(Author, 2026-10-19: "About the call function, make it take a "DataInputStream"."; "Objects
  introduce overhead, for a 2^25+ record data base this is unacceptable. The method require any number
  of inputs of any types, formalizing this would be hard. The DataInputStream is well designed for
  that."; "you know the saying "If you find a good solution, find a better solution" and I am open to
  suggestions.")* No related entry found in the summaries.
- ACSES splits into two packages: `acsed`, the daemon, which gets raw data on the input channels (such
  as sockets) that its boot parameters open, maybe only one; and `acsess`, the command line tool,
  which converts the CLI to raw data, owns the communication, and boots acsed, so it chooses the
  channels and uses them. *(Author, 2026-10-19: "the daemon will be called ACSED and the command line
  tool will be called ACSESS. So the ACSES project is split in two packages, acsed that gets raw data,
  and its inputs are specified during the booting session, and the acsess that converts the CLI to raw
  Data."; "by inputs I mean things like sockets."; "You thought `acsess writes by the same layout the
  daemon reads by.`, which is correct, the communication becomes the responsibility of acsess. The
  ACSED will take in parameters when it is booted up to open input channels (or maybe only one channel,
  whichever preferred). The ACSESS will bootup the ACSED for usage, so it can specify the channels
  itself, and can use them itself.")* Commons: "There are four layers: 1 library, 2
  database application, 3 requester, 4 GUI or 4 Agent" and "the GUI and agents will use the adaptor"
  (`citable.md` → Layers and names), acsess being the requester; "it must launch a separate process"
  and "will launch [the database engine] if and only if (boot is the command and there is not anyone
  running)", now acsess launching acsed. Complements: "ACSES uses it for [the] database service"
  (`citable.md` → Layers and names) keeps ACSES as the project
  holding both; "many ways of accepting requests" gains that boot chooses them. No conflict found.
- A request is described by a line in a small string grammar, written by reflection from the methods the
  implementation marks, so no name is a literal; acsed gathers a database's lines into `acse-language.txt`
  at its root, which acsess reads. *(Author, 2026-10-20: "Go with (b), the string grammar"; "Only
  problem with b is, we need to automatically read the method names from the code instead of string
  literals."; "The implementation meant to define the requestables. I suggest a
  "getRequestables(DataOutputStream dos)" that describes the available request options."; "The file
  that describes commands can be in the database root named as "acse-language.*" or alike.")*
- acsess translates names into codes; a probe answers where an actor's class sits in the file, which is
  rewritten only when a new class comes; acsed reads no name to pick a method. *(Author, 2026-10-20: "I
  suggest a "probe" request which make the instance load its method data from the file. So file is
  rewritten only when a new class is imported."; "I decided the daemon is not responsible of translating
  the commands, they come as raw data, and the ACSESS translates commands to data."; ["The method name is
  a huge No." means] "the fact ACSED is not responsible with reading names to check which method.")*
  Commons: "can only request" (`citable.md` → Journal and undo), acsess being the client.
- A request's address is plain ints: the subject, a database's index or acsed; within a database the
  type, 0 the core, -128 the journal, 1 an arity, 2 a structure, 4 a procedure, 9 an arity row.
  *(Author, 2026-10-20: "0 means core, -128 means journal, 1 is for arity, 2 for structures, 4 for
  procedures, 9 for arity rows"; "we can simply use positive values for database index and negative
  values for ACSED commands."; "Also commands can be long, we do not need to limit ourselves to bytes.
  Why optimize the command length for bytes of data?")*
- A database is an index, listed with its name and folder in a file acsed writes; acsess turns a name
  into its index; a database's name holds no colon. *(Author, 2026-10-20: "Data base must also be an
  index, so a file listing data base names with paths with their indexes are the right choice."; "So the
  acsess reads the data base name and gives the index to the acsed. ACSED meant to be a pure server as I
  said before, and ACSESS is the command line tool to connect to it locally."; "Instead we forbid the
  users to use `:` on the database name")*
- acsed's own commands are not journaled and do not go through `ACS_Actor`. *(Author, 2026-10-20: "ACSED
  will not be journalled."; "ACSED commands do not need to go through the ACS_Actor, right?")*
- Requests do the checks and call checkless methods; for now the public methods keep their checks.
  *(Author, 2026-10-19: "One important detail, there must be checkless methods and requests must do the
  checks and call those methods."; 2026-10-20: "keep the check in the public methods for now, in
  development extra error check doesn't harm.")* Commons: "the "check and call checkless
  functions" pattern" (`citable.md` → Journal and undo).
- A row a request makes keeps its file at the database's root, `arity-<k>-row-<j>.bin`, or
  `arity-<k>-row-<j>plus.bin` for one file of several rows; a store keeps its path relative to the root,
  and a load copies the stored file to the loaded database's root. *(Author, 2026-10-19: "Use a path
  relative to database root, created automatically, calculated by arity and row index like data.";
  "Having a separate file for every type of record is better in general, but we still allowed a single
  file for multi row option, we can append its name "plus" as in `arity-<k>-row-<j>plus.bin`.";
  2026-10-20: "the file of the file row moves to the version on save, so the root also serves as a
  temporal place. Thus copy them to the database root on load.")*
- A removed component leaves a tombstone rather than shifting the pointers, which also allows a
  compacting load. *(Author, 2026-10-20: "in case of an Component removal, we need to either shift the
  pointers or to keep tombstones. I will go with tombstones, it also allows loading with a compacting.
  Lastly, since in the intended (for the project vision) use case there will be at most 2^20 arities (of
  2^(22 to 28) identities), it is not going to be a huge problem.")*
- Which row a file content row takes its paths from is ACSE's to find, not an input of the request.
  *(Author, 2026-10-20: "The "pathRow" is the responsibility of the ACSE, so it shouldn't leak to the
  command.")*
- A client gets a new record's index by capturing it; `registerIf` is no request. *(Author, 2026-10-20:
  "`registerIf` must go, and client must call capture which returns an index.")* Commons: "Core is the
  only caller of removal requests (from outside)" (`citable.md` → Identities and removal).
- The core and the journal are addressed by the database's name, `main:`, and also as `main:core` and
  `main:journal`. *(Author, 2026-10-20: "I thought both of them [the core and the journal] can be
  expressed as the name of the database"; "Also allow the `main:core` and `main:journal` actions for
  clarity.")*
- Separate projects for ACSESS and ACSED are a goal. *(Author, 2026-10-20: "making a separate maven
  module for the daemon is one of the (future) goals."; "I meant separate projects for ACSESS and ACSED
  is a goal.")*
- A read through a request is recorded in the journal as a query, and `addCatalogRow` takes a row class
  by its full name. *(Author, 2026-10-19, on reads written to the journal as queries: "Yes, record
  them"; picking Claude's option "Full class name" for how `addCatalogRow` picks its row class.)*
- Every binary structure is a connector, each kind a restriction of it: node-edge is a connector whose
  every edge connects to two nodes of one arity at once, and from-via-to is a connector whose connections
  matter, an order higher as node-edge is to the unary graph. *(Author, 2026-10-21: "every binary
  structure is a connector, and the node-edge is the restriction of always two elements from the same
  arity connecting to one in the second arity at the same time."; "You see we have the "unary graph" and
  a second order for "node-edge". Same logic with the "from-via-to", it is a "Connector" where
  connections matter. So same thing with Connectors. If we didn't record there can be many types of
  connectors based on the restrictions.")* Commons: "two elements from a single arity [are] matched to a
  single element on the second arity" ([The synapse structure:], `citable.md` → Architecture).
  Complements: "There [are] many ways for two arity structures to exist" (`citable.md` → Architecture)
  gains what those ways are; "some transitions from the one arity to another" (`citable.md` →
  Architecture) is from-via-to; the 2026-10-19 entry above, on a structure holding only connections,
  gives the order step. No conflict found in the Citable; the connector contract's "no order, no
  weight, no count" is code, asked in the session.
- Structures are just connections, and the number of arities only changes how many approaches they can
  have: node-cluster is a connector with graph rules on its primary arity, node-edge one of its kinds; a
  connector may force both sides to be graphs, and the most formal ternary structure connects a few
  elements from each of three arities, any of which may be forced to be a graph. *(Author, 2026-10-21:
  "Structures are just connections, number of arities only change how many approaches they can have. We
  can also make a connector that forces both sides to be a graph."; "Make a formal "node-cluster" super
  interface for the node-edge interface, it is simply a connector which has graph rules for the primary
  arity."; "About the ternary, in its most formal form it connects few elements from any three, we can
  force any of the arities to be graphs, not necessary right now just shows the general shapes of
  structures.")* Commons: the 2026-10-21 entry above, "every binary structure is a connector"; the
  2026-10-19 entry, a structure holding only the connections. Complements: "a single arity structure is
  always a type of graph" (`citable.md` → Architecture) is the rule a connector may force on a side; "some
  transitions from the one arity to another" (`citable.md` → Architecture) is one ternary shape. No
  conflict found in the Citable.
- Clusters are symmetric by nature, and direction is a restriction on them, because a structure without
  direction is the easier one to represent there. *(Author, 2026-10-21, on how a cluster gives direction:
  "Clusters are symmetric by nature. Hereby the direction is a restriction. Record the reason of the
  decision: it is easier to represent directionless here.")* Complements: the entry above, node-cluster
  being a connector with graph rules on its primary arity. No conflict found in the Citable; unary stays
  directed at 0 with symmetric as its restriction, from the same session, unrecorded.
