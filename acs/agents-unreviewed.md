# acs: entries to review

# Unreviewed

- A structure holds only the connections, never data of its elements: whatever a connection means
  beyond the connection is an entity, so it gets an identity in an arity, which is why
  `ACS_NodeEdgeComponentBase` exists. A unary structure is a directed graph on one arity's identities,
  each kind a restriction of it. *(Author, 2026-10-19: "structures meant to hold only structure data,
  not the data of the elements of a structure. And the ACS assumes every entity we care about belongs
  to one single identity and if the connections represent anything else then the structre, they must
  belong to an Arity, that's why the ACS_NodeEdgeComponentBase is created."; "About the unary
  structures, they are always a graph, only restrictions can exist, for an example a sequence is a
  directed graph with the limitation of each node having a "from" and a "to".")* Complements:
  "Structure — data about relations, over one arity or several, under any rule" (root 56)
  gains what a structure may not hold; "recording the relation is enough" (storing a structure, acs 27) and
  "optimally for memory and time" (acs 18) agree. No conflict found in the summaries.
  Section: Architecture
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
  the application's responsibility" (acs 36, root 47). Complements: "registers its op codes
  and a codec" gains registered requests beside the op codes. No conflict found in the summaries.
  Section: Journal and undo
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
  Section: Journal and undo
- No CQRS (Command Query Responsibility Segregation: writes and reads on separate paths) for now,
  since every action returns a result. *(Author, 2026-10-19: "Every action returns a result, so we do
  not need CQRS right now.")* No related entry found in the summaries.
  Section: Architecture
- An actor's `call` takes a `DataInputStream`: objects cost too much on a database of 2^25 or more
  records, and a method takes any number of inputs of any type. The author is open to a better
  solution. *(Author, 2026-10-19: "About the call function, make it take a "DataInputStream"."; "Objects
  introduce overhead, for a 2^25+ record data base this is unacceptable. The method require any number
  of inputs of any types, formalizing this would be hard. The DataInputStream is well designed for
  that."; "you know the saying "If you find a good solution, find a better solution" and I am open to
  suggestions.")* No related entry found in the summaries.
  Section: Architecture
- Requests do the checks and call checkless methods; for now the public methods keep their checks.
  *(Author, 2026-10-19: "One important detail, there must be checkless methods and requests must do the
  checks and call those methods."; 2026-10-20: "keep the check in the public methods for now, in
  development extra error check doesn't harm.")* Commons: "the "check and call checkless
  functions" pattern" (acs 36).
  Section: Journal and undo
- A read through a request is recorded in the journal as a query, and `addCatalogRow` takes a row class
  by its full name. *(Author, 2026-10-19, on reads written to the journal as queries: "Yes, record
  them"; picking Claude's option "Full class name" for how `addCatalogRow` picks its row class.)*
  Section: Journal and undo / Details
