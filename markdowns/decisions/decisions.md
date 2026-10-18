# Decisions

The author's words. Rules: `CLAUDE.md` → Markdown files.

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
- [Is the GUI OntoDirectory?] Yes, but needs improvements.
- Right now we do not make access control or cyber security, out of the scope for MVP. *(2026-10-14)*
- ACS [`acse`] is the library, it will not be usable on its own.
- For the naming, acse replaces the acs, and acses replaces the acse. Now it sounds like access, which
  is cool.
- The ACS is a system, as it names; ACSE is its implementation, and ACSES uses it for [the] database
  service. *(2026-10-14)*
- [On renaming to ACC, Arity Component Composer:] In terms of ECS's naming, the appropriate name for
  the system would be ASP (Arity-Structure-Procedure), which is likely trademarked. ASPCC would be
  the full name of the system since it describes exactly what it does. We better go with acs, also
  do it as "OE_ -> ACS_". *(2026-10-14)*

Details: `layers-and-names.md`.

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

Details: `architecture.md`.

## Identities and removal

- Core is the only caller of removal requests (from outside), and arity is the one removing, that also
  delegates the message through the core. *(2026-10-15)*
- The component will depend less on the core; arities remove their elements on their own.
  *(2026-10-15)*
- Structures are responsible for representing the data optimally for memory and time, while the imv
  values can be removed or structures around them change. *(2026-10-15)*
- Journalling removed the necessity of separating forgetting from removal, so hereby data is
  forgotten (but journalled) the moment an element is removed. *(2026-10-15)*

Details: `identities-and-removal-1.md`, `-2.md`.

## Journal and undo

- I recently decided the "check and call checkless functions" pattern. Simply, there will be
  functions that resolve [a] client request into private reckless functions of unit operations.
- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary; multithreading as a single writer with readers on a snapshot. *(Old plan's
  wording, confirmed by the author.)*
- For every request about ACS, it must be able to store a comment, like the git commit messages, but
  for every action, because every action is made from a resource and later may be audited, which
  will require context.

Details: `journal-and-undo-1.md`, `-2.md`.

## Descriptions

- [The description:] it must describe each instance of [a] component of the core using a string value;
  it must be able to be read and set. It is there so the agent can state the purpose of each
  component. *(2026-10-11)*
- Also classes can have a description. For an example, there can be many different implementations
  of mathematically [the] same indexing logic (say two arities, one element from arity one is
  connected to one element from another arity or not) but different ways of representing them,
  performant for different cases. The agent [is] meant to choose between them; also the agent can
  decompile the class files to make its own descriptions. *(2026-10-14)*

Details: `descriptions.md`.

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

Details: `persistence.md`.

## Identity sets

Details: `identity-sets.md`.

## Filtering

- The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like "namedLike" or "childOf". Also this is essentially a query service.

Details: `filtering.md`.

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
