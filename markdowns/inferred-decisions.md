# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Markdown
files.

## Unreviewed

### Urgent work — the old `ARCHITECTURE.md` handoffs

The task wording is Claude's; the facts in it were checked 2026-09-17. *(Author: "The handoffs are
things that are decided to be done and urgent.")*

- Design the binary structure (class ↔ element indexer) in `structures/indexers/`. Read
  `legacy/core/segments/manager/DoubleArrayedIndexer` first: `ArrayList<BitSet>` one way,
  `ArrayList<IntArrayList>` the other, with tombstone lists.
- Write `package-info.java` for `structures/graphs/` (the author writes it).
- Resolve `catalogs/LongIntCatalog`, an unreferenced near-duplicate of `IntegerValueCatalog`.
- DAG tests in `src/test/` (the author writes them). Coverage: encoding transitions, root list after
  every phase, cycle refusal, slot reuse, `isTight` both ways, loose-then-`tighten(ROOT)` equivalence,
  reduction preserving reachability.

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
