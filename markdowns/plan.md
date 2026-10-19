# Plan — actors, ACSED and ACSESS, and the core commands

Written 2026-10-19 by Claude, a draft for one execution session, which stops for the author's review
and commit. Every line rests on the author's words quoted here, a fact checked on 2026-10-19, or is
marked as Claude's suggestion. **Open** must be answered here before the session passes step 1. The
Citable wins any conflict: stop and ask. Round 3's plan is in git at 31e0194.

## Decided

The author, 2026-10-19:

- Picked among Claude's options: "Commands now, filtering next" — "This round: commands for arities,
  identities and values, plus plain reads (one identity's values, the occupied identities). The next
  round builds filtering".
- How `addCatalogRow` picks its row class, picked: "Full class name" — "Any row class on the daemon's
  classpath, given by its full name."
- Reads written to the journal as queries (`recordQueryRequest`/`recordQueryResponse`): "Yes, record
  them".
- "as the goal it gives commands to the Core with the request comment and returns what Core returns."
- "the interface contracts for components must allow a way to sign their allowed methods with
  bindings, and a call method. Because if you think about it, the ACS contracts are for the management
  of ACS not its data. So if a component allows things outside the responsibilities of ACS it must
  allow it through a "call" like method. This likely fixes all."
- "The "doTraceableStep" is similar to what we want. The difference is, instead of steps the requests
  are registered. [...] the classes that implement it are actors that expose some methods to outside
  and some other methods to Journal. This is elegant, we need to make the `TraceableForUndoingRedoing`
  a separate interface with its own name like "ACS_Actor", and even the journal can implement it, since
  it exposes some methods to the user."
- "The `doTraceableStep` will be part of ACS_Actor, and the Journal will simply make it return an
  illegal action error stating (This feature is out of MVP)."; "The refuse because of "inspection" will
  likely also be handled inside of the journal's call method"; the multi-branch journal: "completely
  out of scope".
- "Every action returns a result, so we do not need CQRS right now."
- "About the call function, make it take a "DataInputStream"." — "Objects introduce overhead, for a
  2^25+ record data base this is unacceptable. The method require any number of inputs of any types,
  formalizing this would be hard."
- "the daemon will be called ACSED and the command line tool will be called ACSESS. So the ACSES project
  is split in two packages, acsed that gets raw data, and its inputs are specified during the booting
  session, and the acsess that converts the CLI to raw Data."; "by inputs I mean things like sockets."
- "You thought `acsess writes by the same layout the daemon reads by.`, which is correct, the
  communication becomes the responsibility of acsess. The ACSED will take in parameters when it is
  booted up to open input channels (or maybe only one channel, whichever preferred). The ACSESS will
  bootup the ACSED for usage, so it can specify the channels itself, and can use them itself."
- "ACS_Actor is simple, the session can write it. ACS_Actor will have three more methods than the
  "Traceable..." class which is "request(DataInputStream parameters, DataOutputStream returns)"."
- "Every requestable method meant to be binded, and must have error checks, more importantly it will be
  handled on the component implementation, in order to preserve AOP (the aspect of behavior of
  component is defined in that component). The others are about describing itself, and has can be
  defined later."
- "We still can and should write interfaces (in ACS) for structure types"; "We need to make sub
  structure contracts having their methods." Interfaces other than `ACS_Actor` are the author's
  (`CLAUDE.md`).

So `call` above is `request`, and this session writes it alone of the three; the two describing
methods wait.

The author's answers to Open, 2026-10-19:

- O4: "Call only, (at least for now)" — the core and the arities through `request` only.
- O5: "Use a path relative to database root, created automatically, calculated by arity and row index
  like data. Since there will be an actor that decide stuff from the DataInputStream inputs, we can do
  whatever we want."
- O6: "undo, redo, inspection related, undo/redo until. You can guess those." Claude's guess: the
  journal's `request` takes `undoLastActivity`, `redoLastActivity`, `undoUntilActivityIndex`,
  `redoUntilActivityIndex`, `inspectAt`, `endInspection`, `isInspecting`, `getCurrentActivityIndex`,
  `getPendingActivityCount`. `waitInspectionReturn` stays acsed's: a held wait keeps a connection open,
  which a method running on the daemon's one thread cannot.
- O8: "One, since it is likely for all the functionality." Claude's reading: acsess derives the socket
  path as today and gives it to acsed at boot; a later acsess derives the same path; a second boot keeps
  today's rule.
- O9: "I said Data Streams."
- O10: "Yes, that requires its own talk." No structure contracts this round.
- O7: "ACSES become ACSED, simple." Claude's reading: `ACSES_Commands`, `ACSES_Daemon`,
  `ACSES_Database`, `ACSES_PersistenceJar` and `ACSES_Request` take `ACSED_`; `acsed.sock`, `.lock`,
  `.log`, `$ACSED_SOCKET`; the tool's main class `ACSES` becomes `ACSESS`, as the author named the tool.
- O1: "Method name is a huge no, that kind of stuff will be handled on the ACSESS, we want the ACSED to
  be fast and low memory. It can throw IO exception, but it fails outside io it only needs to print out
  the error."
- O2: "By layout, if you mean the interfaces, yes. If you also mean a static definition of layout or
  copy of implementations, NO, Because we are nowhere near to implement all the classes, formalizing
  the method of automatically binding the implemented Actor's actions visible strings and integer
  values for the Data Streams is the only option."
- O3: "ensureCapacity should, it has no harm of being open. One important detail, there must be
  checkless methods and requests must do the checks and call those methods."
- O5: "Good option. Having a separate file for every type of record is better in general, but we still
  allowed a single file for multi row option, we can append its name "plus" as in
  `arity-<k>-row-<j>plus.bin`." With O5's suggestion: `FileContentCatalogRow` gets its path row's
  index from `parameters`.
- O6: "yes" — `UnsupportedOperationException`.
- O9: "Wasn't it through the journal and plus minus stuff? We need to change it anyway, a byte
  specifying which group and then three bytes that become a four byte int specify the index." Claude's
  reading: the journal's actor ids take the same int. `storeAll` writes them only into `journal.jsonl`,
  which nothing reads back, so `FORMAT` stays 0.

## Facts, checked 2026-10-19

- `TraceableForUndoingRedoing` is nested in `ACS_Journal` (line 52); `ACS_Core:12`,
  `ACS_ArityComponentBase:47` and `ACS_StructureComponentBase:10` extend it; `beginRecordingActivity`
  takes it (`ACS_Journal:9`). `doTraceableStep` is written in `ACSE_CoreBasic`,
  `ACSE_ComposableArityCatalog` and seven structure classes; the journal replays by `actorOf`, the core
  0, arity k as -(k+1), structure k as k+1 (`ACSE_JournalBasic:365`).
- `ACS_Core`: `addArity` (two), `addCatalogRow(int, ACS_ArityCatalogRowBase, String, String)`,
  `addStructure`, `removeArity`, `removeStructure`, `delegateRemoval`, `captureIdentity` (two),
  `getJournal`. `ACS_ArityComponentBase`: `occupied`, `registerIf`, `removeIf`, `catalogCount`, reads for
  int, long, double, boolean, Object and String, writes for long, double, boolean and String,
  `copy…Into(…, dest, offset)`; `ensureCapacity` and `truncate` take no comment.
- The memory rows have a no-argument constructor; the file rows take a `Path`
  (`IntegerValueCatalogFileInt(Path)`); `FileContentCatalogRow` takes another row.
- `acses` holds `acsess` (`ACSES`, `ACSES_Request`) and `daemon` (`ACSES_Commands`, `ACSES_Daemon`,
  `ACSES_Database`, `ACSES_PersistenceJar`). The jar is `acses`, its main `org.halim.acses.acsess.ACSES`,
  which names the daemon by a string (`ACSES:29`).
- A connection carries one request: a count, then each argument as UTF-8 length and bytes; the answer is
  a status and one text (`ACSES_Request:21`). The daemon opens one Unix socket and blocks on `accept()`
  (`ACSES_Daemon:58`); its path is `$ACSES_SOCKET`, else under `$XDG_RUNTIME_DIR`, else the temporary
  folder, with the log and the lock beside it (`ACSES_Request:28`).
- No command writes to a core yet, so every journal through the daemon is empty (53d82b3's message).
  Claude's check outside the repo: `~/mein/claude/scripts/acses-smoke/run.sh`, 137 checks at 53d82b3.

## Open — the author answers under each before step 1 passes

Claude's suggestions, not approved.

- **O1. The method's code and the error.** Suggestion: the code a `short` (65,536 methods per actor);
  `boolean request(…) throws IOException`, false when a check or the method failed, the error's text
  then written to `returns`, so the requester sees it.
- **O2. The automatic binding**, which ties a visible string and an integer to each action. Suggestion:
  an annotation on each requestable method carries its code; acsed reflects an actor's class once, at
  `loadDB` or `addArity`, into a table (code, name, parameter types, result type) that acsess asks for
  by a describing request and converts by; `request` stays a switch on the code. Against: codes taken
  from the sorted signatures renumber when a method is added.
- **O3a. "requests must do the checks and call those methods"**, two readings: (A) the request case
  calls the public method, which already checks and then calls the checkless one, as `setLongFor`
  does; (B) the checks move into `request`, and the public methods become checkless. Suggestion: A.
- **O3b. `ensureCapacity`'s check:** today it sets `lastIndex = identity` even below the reach
  (`ACSE_ComposableArityCatalog:307`), so an open `ensureCapacity(0)` would lower the arity's reach,
  outside the journal. Suggestion: the request refuses a negative identity and keeps the larger reach.
- **O7. Two names the reading leaves:** the module `acses` and the package root `org.halim.acses`,
  since "the ACSES project is split in two packages"; and the jar, `acses` today, which holds both
  programs. Suggestion: the module and the root keep `acses`; the jar becomes `acsess`, the tool typed.
- **O9. The groups and the end:** which byte is which group, and whether bytes left in `parameters`
  refuse the request. Suggestion: 0 core, 1 journal, 2 arity, 3 structure; yes, since reading one more
  byte costs nothing.

## The session

1. Checkpoint, and wait for the author: every Open item answered. If one is not, stop and list it.
   Never discard uncommitted work.
2. The split, by O7: `org.halim.acses.daemon` becomes `org.halim.acses.acsed`; the renames, the daemon's
   name string, the jar's name and main class. Mechanical.
3. Write `ACS_Actor` in `org.halim.acs`: `doTraceableStep`, moved with its javadoc, and `request` by
   O1; not the two describing methods. It replaces `TraceableForUndoingRedoing` in every reference in
   `acs` and `acse`. `ACSE_JournalBasic` implements it by O6: its step throws "This feature is out of
   MVP"; its `request` takes the methods O6 lists and refuses a write during an inspection; its actor
   ids become O9's group byte and three-byte index.
4. `request` in `ACSE_CoreBasic` and `ACSE_ComposableArityCatalog`, by O1–O5: one switch per class on
   the method, each case reading its inputs, running the checked method and writing its result, so the
   component handles it; a read is recorded by `recordQueryRequest`/`recordQueryResponse`;
   `addCatalogRow` loads its class by full name, a file row given its path by O5. No structures
   (O10).
5. acsed opens the one socket acsess gives it at boot (O8) and reads requests by O9; acsess boots it,
   converts the CLI to raw data by the layouts (O2), and prints the results.
6. Verify: `mvn -o -q compile`. Extend Claude's `~/mein/claude/scripts/acses-smoke/run.sh`: boot with a
   channel and a later acsess run finding it; `addArity`, `addCatalogRow` by class name, `captureIdentity`,
   a write and a read of each type, the occupied identities; a read recorded as a query; a write during
   `inspectAt` refused; leftover bytes refused; the journal's step error. Tests in `src/test/` are the
   author's.
7. List the doc lines the work makes false (`CLAUDE.md` → Layout, `current-state.md`'s ACSES row, the
   README's commands, the javadocs naming `acses`), propose replacements, edit on approval.
8. Propose entries under Unreviewed in `inferred-decisions.md` for the answers given at step 1, quoting
   the author; add them on approval.
9. Stop: `git status`, the diff's summary, the checks' results. No commit until the author says.
