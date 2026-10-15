# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Markdown
files.

# Confirmed 

### Urgent work — the old `ARCHITECTURE.md` handoffs

The task wording is Claude's; the facts in it were checked 2026-09-17. *(Author: "The handoffs are
things that are decided to be done and urgent.")*

- Design the binary structure (class ↔ element indexer) in `structures/indexers/`. Read
  `legacy/core/segments/manager/DoubleArrayedIndexer` first: `ArrayList<BitSet>` one way,
  `ArrayList<IntArrayList>` the other, with tombstone lists.
- Resolve `catalogs/LongIntCatalog`, an unreferenced near-duplicate of `IntegerValueCatalog`.
- DAG tests in `src/test/` (the author writes them). Coverage: encoding transitions, root list after
  every phase, cycle refusal, slot reuse, reduction preserving reachability.
- Here are the author's statements:
  - Catalog components to handle data stored in files, in many ways. 
    Like a catalog component instance serving as multiple components (rows) that store all the data
    on the files. A catalog row storing every column's data on a single file. And many other.
  - An Indexer that uses RoaringBitset where each possible connection maps to an index and it's existence
    on the RoaringBitset claiming if the connection exists.

## Unreviewed

### The goal, the layers and the names — read from the chat of 2026-10-07/08

Claude's readings, each quoting the author. "All your readings are right" approves no particular
sentence.

- The library is `acse` (E = engine; long name "Arity Component System Engine") and the service
  program is `acses` (S = service). *(Author: "acse replaces the acs, and acses replaces the
  acse.")*
- "ASC" and "ASCA" in "only two modules, ASC and ASCA" are the library `acse` and the service
  `acses`. *(Author: "let's make this project only two modules, ASC and ASCA".)*
- The requester is not a module: `acses` itself acts as the requester when a service is already
  running, so `acsr boot` becomes `acses boot`, and likewise `loadDB` and `components`. *(Author:
  "the acse is a terminal application for bridging client and database engine for requests and
  responses"; "we can do it to reduce our work".)*
- `boot` is the only command that starts the service; any other command with none running starts
  nothing and reports that none is open (the wording is Claude's). *(Author: "will launch if and only
  if (boot is the command and there is not anyone running)".)*
- The parent project lists two modules, `acse` and `acses`; OntoDirectory and the agents are not in
  this build yet. *(Author: "For now, let's make this project only two modules"; "We can also make
  modules for the OntoDirectory and alike.")*
- "It will not be usable on its own" means no working database comes from `acse` alone, since `acses`
  runs it, but a Java program can still use `acse` directly. *(Author: "(q1 a)"; the other reading
  was that `acse` holds only the contracts and the core.)*
- "The application" in "steps are the application's responsibility" is the ACS side as a whole, the
  library included, because the core holds the journal; so each request function of the library takes
  a comment, and the journal stores it with the request. *(Author: "The comment will be given by the
  client, which is "asrc", which can only request"; `decisions.md`: "The core handles … journal".)*
- The file of requests is one way of accepting requests among many, not the channel. *(Author: "(which
  can handle requests in many ways)".)*

### The licence — read from the chat of 2026-10-08

- Read as: the well-known licence unless a custom one gains much for little. It does not: a custom
  noncommercial licence needs a lawyer, is untested, is not open source and is closed to GPL code,
  and its extra reach is small or unenforceable. So: AGPL-3.0-only plus a sold commercial licence.
  *(Author: "If it is near nothing go with AGPL-3.0-only with a commercial licence, record it.")*
- It covers everything tracked here, both modules and the markdowns; the scope is Claude's.
  *(Author: "go with AGPL-3.0-only".)*
- Selling the commercial licence needs the right to relicense every line, so outside contributions
  need a contributor agreement first. *(FSF FAQ #ReleaseUnderGPLAndNF: "If you are the copyright
  holder for the code, you can release it under various different non-exclusive licenses".)*
- The full licence is `AGPL-3.0-only WITH Universal-FOSS-exception-1.0` (its SPDX form) plus the
  commercial licence: open-source projects may link ACSE and keep their own licences, closed ones
  comply or pay. *(Author: "yes, add the exception and record it"; the SPDX form is Claude's.)*

### The journal's transactions — read from the chat of 2026-10-11

- A transaction is a client request together with every request nested in it, the thing one undo or
  redo moves; `units` in `ACSE_JournalBasic` is renamed `transactions` on this reading. *(Author:
  "each transaction is stored [in] its own section"; "the client … knows only the requests it sends".)*

### Persistence and descriptions — read from the chat of 2026-10-11 to 2026-10-13

- Every stored class declares `public static <its class> loadFrom(ACSE_Core, ACSE_Journal,
  DataInputStream)`; the contracts keep only `saveTo`, and a load finds the method by name.
  *(Author: "make a public static method".)*
- `indexer.bin` names the core's class and every class the folder's files use, numbered; a file
  writes a part's number, not its name. *(Author: "an indexer.bin that tells each class"; chose "Every
  class used" for "What should indexer.bin hold?".)*
- An arity's catalog rows and identity set go inside the arity's file, and every generation is kept.
  *(Author chose "Inside the arity's file" and "Keep all".)*
- A left-out component or row loads back empty; an identity set cannot be left out, so its arity
  fails the store; `store` returns the warnings as a list. *(Author: "Leave it out … Just warn about
  it", choosing the option that said an identity set can't be left out.)*
- Each generation's `classes.jar` holds every class the folder uses, and what those reach from the
  same places, except the contracts (the engine's public interfaces), the JDK and the libraries.
  *(Author: "even the base classes will move to the target work space"; "acses only knows the
  contracts".)*
- A load takes every class the folder's jar holds from that jar, and only the rest from the program;
  no class files are compared. *(Author: "data base is the authority about implementation"; "We do not
  need to compare class files, do we?")*
- The core is built through its public no-argument constructor, then `load(folder)`;
  `ACSE_PersistenceManager.loadCore(folder)` does both. *(Author: "Rest can be confirmed", on Claude's
  list.)*
- The root holds `acse-persistence.data` - "ACSE", the format, the manager's class name, then the
  manager's own data, for the basic manager the generation number - and `acse-persistence.jar`;
  without the jar, `open` uses the program's manager. *(Author: "make it acse-persistence.bin as
  binary"; "the acse-persistence.data (hereby, for less confusion)". The fallback is Claude's.)*
- The journal's booleans use `java.util.BitSet` with a count, not Roaring: they are dense and added in
  order. *(Author: "we need to use bitset or roaring bitset".)*
- Descriptions live outside the journal: undo leaves them alone, a removed component keeps its own,
  and class descriptions are keyed by class name. *(Author: "no need of journalling"; "represent them
  in the easiest way".)*

### The renames and the modules — read from the chat of 2026-10-13

- The first ACSES is the library: the project ACS holds two modules, `acse` and `acses`. *(Author:
  "the ACSES will be a (maven) module of this project, and ACSES will also be another sub module".)*
- "Rename all" says which renames, not when: they wait for the messy version's upload like the
  modules, so nothing is renamed before the first push. *(Author: "Rename all."; "Do not do it yet. I
  want to upload the messy version of this code base first for keeping its identity.")*

### Handoff — work decided to be done and not done, moved from `handoff.md` on 2026-10-14

Claude's wording, moved unchanged when `handoff.md` was deleted; only the last entry gained its quote.

- Now that the messy version is pushed, every rename: the `OE_` prefix becomes `ACS_`, and
  `OntologyEngineCore` and the package `org.halim.ontology` become `acse`; `naming-fix.md` names the project, which keeps the name ACS,
  and the file stays; and the parent project ACS takes the modules `acse` and `acses`, with
  `src/` and `pom.xml` moving into the library's module (`decisions.md` → Layers and names).
- The commercial licence's text, drafted by a lawyer before it is sold, and a contributor agreement
  before the first outside contribution.
- `README.md` → The application and The goal were written before the goal was stated (`decisions.md`
  → Goal, 2026-10-08): they present OntoDirectory's filing-based RAG, not the database service with
  requests, a GUI and the agent pipeline. Rewriting them is the author's (the README is handwritten);
  where they differ, `decisions.md` wins. The Claude chat responses pasted while stating the goal are
  kept verbatim, gitignored, in `reference/chats/claude-chat-acs-agent-goal--2026-10-07_1007.md`.
- `acses`, later: `boot` launches a separate process, any other command bridges to it, and
  loading-folder and loading-class methods must exist (`decisions.md` → Layers and names). Creating a
  database copies `acse-persistence.jar` and the base classes from the ACSES application's own side
  (a config folder is optional), builds an empty core and stores it; loading opens the folder's manager and records the core under
  `--calledBy <name>`, else the folder's name (`decisions.md` → Persistence). The library side exists.
- Later: the urgent code work in `inferred-decisions.md` → Urgent work (unreviewed, so not started),
  the procedure contract (`decisions.md` → Architecture, Vision), and more structure components, the
  cartesian (ternary) and the transitions arity among them (`decisions.md` → Architecture).
- The folder on disk need not be renamed for pushing. If it is renamed, between sessions, Claude
  updates everything that points at its path (listed in Claude's memory).
- The novelty study is running in another session (2026-10-13); its launcher is in Claude's folder
  (see Claude's memory).
- Delete `reference/msgfilter.py` once the author says so; it is gitignored, so it cannot be published
  by accident. The pre-purge bundle was deleted on 2026-10-13.
- Introduce the "String" type to the catalogs, a string subversion of the Object one. The allowed
  types are not in one place: the constants (`ACS_ArityComponentBase:90`, copied at
  `ACS_ArityCatalogRowBase:26`), `typeName`, one default accessor per type in both bases, and the
  journaled writes' type check and `bitsOf` (`ACSE_ComposableArityCatalog:151`, `:168`). *(Author,
  2026-10-14: "Right now we have object as a valid catalog option, we still need to make a string
  subversion of it. The problem with it is the allowed types of those catalogs are not in one place".)*
