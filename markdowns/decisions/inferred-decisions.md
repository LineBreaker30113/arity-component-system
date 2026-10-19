My better wording for the inferences:


The ACS Project:
- Vision of the ACS related projects and the goal, also all the Important ACS projects.
  ACS: The contract interfaces, since they elegantly formalize thing into modules.
  ACSE: Few good implementations of the ACS contracts for solidifying the project.
  ACSES: A client side ACSE implementation for managing ACS based databases. ACSE contains few default implementations.
  ACS-alHikmet: Agents for building and auditing an ACSES database of paragraphs.
  ACSAH viewer: A GUI application like OntoDirectory to analyze the product of ACS-alHikmet.
  OntoDirectory: Where it all started, aimed to be integrated with ACSE instead of ontology package int it's source code, later can be implemented with AI agents like ACS-alHikmet.

# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Decisions.
Reduced on 2026-10-18 at the author's request ("please reduce them"): done, replaced and duplicated
items were removed; the version before is in Claude's results folder.

# Confirmed

### Handoff — decided, not done yet

Claude's wording unless quoted; facts checked 2026-10-18. *(Author: "The handoffs are things that are
decided to be done and urgent.")* The last five, and the reading below, were confirmed on 2026-10-18.
*(Author: "I read and confirm it.")*

- DAG tests in `acse/src/test/` (the author writes them): encoding transitions, root list after every
  phase, cycle refusal, slot reuse, reduction preserving reachability.
- [The `OE_` prefix, `OntologyEngineCore` and the package `org.halim.ontology`:] now the name is
  "ACS" [now `acse`], replace them with "ACS" [`acse`, except the `OE_` prefix, which becomes `ACS_`:
  see Layers and names, 2026-10-14]. — `OE_` is gone. Done for the live work, 2026-10-18;
  `OntologyEngineCore.java` and `org.halim.ontology` remain in `deprecatedCode/`.
- [The `Module` at the end of `SetHierarchyDirectedAcyclicIntGraphModule`:] replace with
  `Component`. — Still so named, in `structurer_depr/`.
- [Naming the judgment-call existence method so it looks deliberate, like `identityAtUnchecked`:]
  this naming convention is good, but out of scope; this is boilerplate polishing. — Not named yet.
- Test the README's "2^28 files of records … a few GiB" claim; nothing measures it. *(Author: "Record
  it, we need to test it.")*
- Move the decisions into the Citable (`CLAUDE.md` → Decisions): `decisions.md` and its topic files
  joined into `citable.md`, words unchanged; this file joined in, Unreviewed at its end as
  Unconfirmed; summaries cut to about a quarter, plus `summaries/overview.md`; the summary check run.
  Where the handoff goes is the author's call.
- Later: the procedure contract, and more structure components, the cartesian (ternary) and the
  transitions arities among them (`decisions.md` → Architecture).
- A "String" catalog type, a string subversion of the Object one. Its allowed types are spread over
  the type constants of `ACS_ArityComponentBase` (copied in `ACS_ArityCatalogRowBase`), `typeName`,
  both bases' default accessors, and the journaled writes' type check and `bitsOf` in
  `ACSE_ComposableArityCatalog`. *(Author, 2026-10-14: "Right now we have object as a valid catalog
  option, we still need to make a string subversion of it. The problem with it is the allowed types
  of those catalogs are not in one place.")*
- The commercial licence's text, drafted by a lawyer before it is sold, and a contributor agreement
  before the first outside contribution.
- Delete `reference/msgfilter.py` once the author says so; it is gitignored.

### Readings

- "It will not be usable on its own" means no working database comes from `acse` alone, since `acses`
  runs it, but a Java program can still use `acse` directly. *(Author: "(q1 a)".)*

## Unreviewed

Agents append new inferences here (`CLAUDE.md` → Decisions).

- Later, in `acses`: commands that interact with the arities and that query data, and custom class
  files handled properly, with an option mechanism beside a list function. *(Author, 2026-10-18, in
  `plan.md` → P2: "Since the database is really modular, and we made the Composable Arity instance, we
  must implement a way of interacting with the arities. Also, there must be methods of querying
  data."; answering P2: "since we plan allowing custom class files, we need to handle it properly,
  but later. Certainly an option mechanism along side a list function necessary.")*
- The interfaces in `acs` start with `ACS_`, the core, journal, persistence manager and description
  manager included. *(Author, 2026-10-18: "Hereby interfaces in the acs start by ACS_ instead.")*
  Conflicts: `layers-and-names.md` → [The prefixes:] (2026-10-14) would lose "except the core,
  journal and most importantly the persistence manager". Commons: "do it as "OE_ -> ACS_""
  (`decisions.md`). Complements: "ACS: The contract interfaces" (atop this file). `persistence.md`'s
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
  "Structure: data about relations over one arity or several, under any rule" (`summaries/words.md`)
  gains what a structure may not hold; "recording the relation is enough" (storing a structure) and
  "optimally for memory and time" (`identities-and-removal`) agree. No conflict found in the summaries.
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
  ACS_Actor")* Commons: "Check-and-call-checkless pattern" and "can only request; steps are the
  application's responsibility" (`summaries/journal-and-undo.md`). Complements: "registers its op codes
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
  Complements: "break undo history" (`journal-and-undo-1.md`), which an inspection cannot move past.
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
