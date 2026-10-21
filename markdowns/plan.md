# Plan — actors, ACSED and ACSESS, and the core commands

Written 2026-10-19 and 2026-10-20 by Claude, for one execution session, which stops for the author's
review and commit. **The design** below is what the session builds; each line is the author's decision,
Claude's reading of one (marked *reading*), or Claude's suggestion the author took ("Take your
suggestions", 2026-10-19 and 2026-10-20). **The author's words** are the source, in the order given; where
a later answer replaced an earlier one, the design follows the later. The Citable wins any conflict: stop
and ask. Round 3's plan is in git at 31e0194.

## The design

### Scope

- This round: requests for the core, the arities and the journal: arities, catalog rows, identities,
  values, and plain reads (one identity's values, the occupied identities). No structures, no filtering.
- Interfaces stay the author's, except `ACS_Actor` and the annotation `ACS_Requestable`, which this
  session writes.

### `ACS_Actor`, in `org.halim.acs`

Replaces `ACS_Journal.TraceableForUndoingRedoing` everywhere. Its methods:

- `void doTraceableStep(byte stepPair, int[] params, int index)`, moved with its javadoc: the journal's
  side, unchecked steps.
- `boolean request(DataInputStream parameters, DataOutputStream returns) throws IOException`: the
  outside's side. `parameters` starts with the request's code, an `int`, then the inputs; the result
  goes to `returns`. False when a check or the method failed, the error's text then written to `returns`;
  a failure outside I/O only reports the error.
- `void getRequestables(DataOutputStream dos) throws IOException`: writes one line per request, in code
  order, in the grammar below, UTF-8, each ending in `\n` (*reading*: text, so `acse-language.txt`
  reads as text).
- A static helper that writes the line of every method of a class marked `ACS_Requestable`, in code
  order, by reflection, so no name is a literal; an implementation's `getRequestables` calls it. It
  refuses loudly when codes repeat or skip (they run 0 to count−1) or a marked method has a type the
  grammar lacks.

`ACS_Requestable` marks a requestable method and carries its code. Each class's `request` is a switch on
the code, written in the same order: each case reads its inputs, calls the public method, which keeps its
checks and calls the checkless one (`setLongFor` as today), and writes the result.

### The grammar

- A line: `name(type name, …) result`, e.g. `setLongFor(int catalog, long identity, long value, String
  comment) void`; names from reflection, so the parent pom compiles with `-parameters`.
- Types: `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `String`, any of them with `[]`;
  a result is one of them or `void`.
- On the wire each as `DataOutputStream` writes it; a `String` as its UTF-8 length, an int, then the
  bytes, as `ACSES_Request` writes today; an array as its length, an int, then its elements, which a case
  may write without building the array.
- Flags fill parameters by name: `name=value` the parameter of that name (`comment=` the `String
  comment`), `+Name` a `boolean` one, matched ignoring the first letter's case (`+Duplicate` fills
  `duplicate`); a flag not given leaves `""` or `false`; the plain arguments fill the rest in order.
- Two requests may share a name when their parameter counts differ (`addArity`, `captureIdentity`);
  acsess picks the one whose remaining parameters match the plain arguments typed.

### What each actor takes

- **Core** (type 0): `addArity(String description, String comment)`; `addCatalogRow(int arity, String
  className, String description, String comment)`, an overload in `ACSE_CoreBasic` that builds the row
  by its full class name; `removeArity`, `removeStructure`, `delegateRemoval`, both `captureIdentity`.
  The object-taking `addArity` and `addStructure` are not requestable.
- **A row's file**: a memory row is built with no argument; a file row gets `arity-<k>-row-<j>.bin` at
  the database's root, `arity-<k>-row-<j>plus.bin` for a single file holding several rows;
  `FileContentCatalogRow` gets its path row's index from `parameters`.
- **Arity** (type 1): `registerIf`, `removeIf`, `catalogCount`, the reads and writes of each type, the
  occupied identities as an `int[]` (a requestable method of its own: `occupied()` returns an object),
  and `ensureCapacity`, which refuses a negative identity and no longer lowers the reach (today it sets
  `lastIndex = identity`, `ACSE_ComposableArityCatalog:307`). `truncate` is not requestable.
- **Journal** (type -128, index 0): `undoLastActivity`, `redoLastActivity`, `undoUntilActivityIndex`,
  `redoUntilActivityIndex`, `inspectAt`, `endInspection`, `isInspecting`, `getCurrentActivityIndex`,
  `getPendingActivityCount`. Its `request` refuses a write during an inspection; its `doTraceableStep`
  throws `UnsupportedOperationException` saying "This feature is out of MVP".
- A read is recorded as a query, by `recordQueryRequest` and `recordQueryResponse`.

### Addressing

- A request's subject is an int: 0 and up a database's index, 0 the first; -1 acsed; the other negative
  values kept for later server-level subjects.
- Within a database, a type, an int: 0 core, -128 journal, 1 arity, 2 structure, 4 procedure, 9 arity row,
  and -1 for acsed's handling of the database, not journaled as the journal is not; then the index, an
  int.
- The header's fields are plain ints, not packed: "commands can be long, we do not need to limit
  ourselves to bytes". Bytes count where data is held: the journal keeps type and index packed into the
  one int per request it holds today, a byte then three bytes, since one more int per request is 2^25 ×
  4 bytes = 128 MiB for 2^25 requests. It writes them only into `journal.jsonl`,
  which nothing reads back, so `FORMAT` stays 0.
- *Reading* for rows this round: a row has no requests of its own yet (`ACS_ArityCatalogRowBase` is no
  actor, and a row's write is journaled by its arity). So `db:arity-0.row-7` fills the arity request's
  first parameter, `int catalog`, with 7, the author's "a command that requires another index for the
  row". Type 9 is kept for when rows have requests.

### `acse-language.txt`, the probe and the wire

- At the database's root: per class, a line `class <full name> <count>`, then its `getRequestables`
  lines. acsed writes it at `createDB` and `loadDB`, and appends a class the file lacks; never after an
  actor changes. `migrate` and `storeDB toPath=` do not copy it, since a load rewrites it; it counts as
  ACS's own file, so `+WithOtherFiles` does not copy it as another.
- acsed is a pure server, subject -1. Its commands are its own switch on the code, not `ACS_Actor`:
  "ACSED will not be journalled", and its commands "do not need to go through the ACS_Actor". Its command
  methods are marked `ACS_Requestable`, and at boot the helper writes their lines to `acsed-language.txt`
  beside the lock (the name is Claude's), which acsess reads without a probe. `boot` stays acsess's,
  since it starts the process. The flag reader, the usage lines and `beginSession` move to acsess.
- A command naming a database takes it as its subject, under acsed's database type: `storeDB`,
  `migrate`, `unloadDB`, `acquire`, `release`, `probe`, `inspectionState`, `waitInspectionReturn`,
  `components`. Subject -1 keeps `createDB`, `loadDB`, `databases` and `shutdown`.
- `probe(int type, int index) int`: acsed reads that actor's class and answers its place in the file,
  appending its section first when it is new.
- Every request is a frame: the subject; for a database, the type and the index (0 for the core, the
  journal and acsed's database type) and, for an actor, the class's place from the probe; the
  acquisition key, a `String`, `""` without one; then `parameters`. A connection carries a byte, 1 for
  `+continueOnRefusal`, a count, then the frames; acsed answers each in order, a status then `returns`,
  and stops at the first refusal unless the byte is 1. One frame usually; `Commit`'s queue as many.
- acsed refuses a frame when that actor's class is no longer at that place, or when bytes are left in
  `parameters` after `request`.
- `inspectAt` and `endInspection` leave acsed's commands for the journal's requests; `inspectionState`
  and `waitInspectionReturn` stay, and after any journal request that ends an inspection acsed answers
  the waits held.
- A database is an index too. `acsed.databases`, beside the lock (the file's name is Claude's): a line
  per loaded database, its index, name and folder, tab-separated, the folder last; rewritten at `boot`,
  `loadDB`, `unloadDB` and `migrate`, as a temporary file renamed into place. acsess reads it for a
  name's index and its folder's `acse-language.txt`. An index is never reused while acsed runs, so a
  stale read reaches no other database; a name holding a tab or a newline is refused.

### The split

- `acses` stays the module and `org.halim.acses` the root; `daemon` becomes `acsed`. `ACSES_Commands`,
  `ACSES_Daemon`, `ACSES_Database`, `ACSES_PersistenceJar` and `ACSES_Request` become `ACSED_…`; the tool's
  `ACSES` becomes `ACSESS`; the jar `acsess`; `acsed.sock`, `.lock`, `.log`, `$ACSED_SOCKET`.
- acsess boots acsed with `socket=<path>`, today's path by default; a later acsess derives the same path;
  a second boot keeps today's rule.
- acsess, per command: a first word holding `:` is an address, any other an acsed command (`acsess
  loadDB /data/main calledBy=main`). It splits an address at its last `:`, the database before it and the
  actor after (`acsess main:arity-0.row-7 setLongFor 5 42 comment=fix`; `db.v2:arity-0` is the database
  `db.v2`, so a dot in a name needs no rule), turns the name into its index by `acsed.databases`, probes
  the actor, reads the class's lines from the file, translates the arguments by them, sends, and prints
  the result by its type. Two round trips a command; the author: "it can make do with the speed".

## The author's words

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
  structure contracts having their methods."
- On Claude's open items: "Call only, (at least for now)"; on a file row's path: "Use a path relative to
  database root, created automatically, calculated by arity and row index like data. Since there will be
  an actor that decide stuff from the DataInputStream inputs, we can do whatever we want."; on the
  journal's requests: "undo, redo, inspection related, undo/redo until. You can guess those."; on the
  channels: "One, since it is likely for all the functionality."; on the wire: "I said Data Streams.";
  on structures: "Yes, that requires its own talk."; on the names: "ACSES become ACSED, simple."
- "Method name is a huge no, that kind of stuff will be handled on the ACSESS, we want the ACSED to be
  fast and low memory. It can throw IO exception, but it fails outside io it only needs to print out
  the error."
- "By layout, if you mean the interfaces, yes. If you also mean a static definition of layout or copy of
  implementations, NO, Because we are nowhere near to implement all the classes, formalizing the method
  of automatically binding the implemented Actor's actions visible strings and integer values for the
  Data Streams is the only option."
- "ensureCapacity should, it has no harm of being open. One important detail, there must be checkless
  methods and requests must do the checks and call those methods."
- On the row file: "Good option. Having a separate file for every type of record is better in general,
  but we still allowed a single file for multi row option, we can append its name "plus" as in
  `arity-<k>-row-<j>plus.bin`."; on the step's error class: "yes".
- "Wasn't it through the journal and plus minus stuff? We need to change it anyway, a byte specifying
  which group and then three bytes that become a four byte int specify the index."

The author, 2026-10-20:

- On the code and the error: "Approved."; on the checks: "keep the check in the public methods for now,
  in development extra error check doesn't harm."
- "The "request" method handles it a certain way, describing it can be another responsibility, but can
  be given to "ACS_Actor" again. So two new methods in the ACS_Actor interface, "get allowed request
  count" and "getRequestMethodAt(int methodI)"." *(Replaced below by `getRequestables`.)*
- "0 means core, -128 means journal, 1 is for arity, 2 for structures, 4 for procedures, 9 for arity
  rows"; on rows: "after picking the arity we can simply make a command that requires another index for
  the row. But you can go with your idea for now, since an element is not likely to have more than 127
  different data."
- "the command should allow things like "db.arity-0.row-7", it is easier and usable." *(Its separator
  replaced below.)*
- On what describes a request, among a `Method`, a string in a small grammar and a description object:
  "Go with (b), the string grammar"; "Only problem with b is, we need to automatically read the method
  names from the code instead of string literals."
- "The implementation meant to define the requestables. I suggest a "getRequestables(DataOutputStream
  dos)" that describes the available request options. When the ACSED runs it calls all the classes of
  the data base for this purpose, and writes them all into one file describing the commands. Then the
  ACSESS reads that file each time sending a request."
- "The file that describes commands can be in the database root named as "acse-language.*" or alike."
- "The file method takes too much overhead for just translating delegation. I suggest a "probe" request
  which make the instance load its method data from the file. So file is rewritten only when a new class
  is imported."; on which instance: "A, the acsess process. The ACSESS will likely be used in terminal
  (as intended), it can make do with the speed slow for a data base."
- "Take your suggestions for the rest."
- "Let's make it `acsess main:arity-0.row-7 setLongFor 5 42 comment=fix` instead."
- "Data base must also be an index, so a file listing data base names with paths with their indexes are
  the right choice."
- "So the acsess reads the data base name and gives the index to the acsed. ACSED meant to be a pure
  server as I said before, and ACSESS is the command line tool to connect to it locally."
- On acsed's own commands, among Claude's options (binary this round, or later): "a, and use 127 for
  acsed".
- Asked whether acsed could be -1, or the negative groups acsed's: "ACSED will not be journalled."
- "The journal is inside of a data base. Wait, do we plan to make a "Subject Action Parameters" like
  structure for the commands? Then the negative values mean the database's journal, and 0 means ACSED,
  and the positives mean data base index. I think this is not optimal tho, since a journal is dependent
  on a data base, we can simply use positive values for database index and negative values for ACSED
  commands. After the data base index we can add another byte for the type which will also contain the
  Journal."
- "ACSED commands do not need to go through the ACS_Actor, right?" (Claude agreed.)
- On Claude's readings: "a, index 0 is first, and yes to database subjects. Also commands can be long,
  we do not need to limit ourselves to bytes. Why optimize the command length for bytes of data?"
- On acsed's database type (-1) and the journal's packed ids: "Take your suggestions".

## Facts, checked 2026-10-19 and 2026-10-20

- `TraceableForUndoingRedoing` is nested in `ACS_Journal` (line 52); `ACS_Core:12`,
  `ACS_ArityComponentBase:47` and `ACS_StructureComponentBase:10` extend it; `beginRecordingActivity`
  takes it (`ACS_Journal:9`). `doTraceableStep` is written in `ACSE_CoreBasic`,
  `ACSE_ComposableArityCatalog` and seven structure classes; the journal replays by `actorOf`, the core
  0, arity k as -(k+1), structure k as k+1 (`ACSE_JournalBasic:365`). Undoing `addArity` empties its
  slot (`ACSE_CoreBasic:431`).
- `ACS_Core`: `addArity` (two), `addCatalogRow(int, ACS_ArityCatalogRowBase, String, String)`,
  `addStructure`, `removeArity`, `removeStructure`, `delegateRemoval`, `captureIdentity` (two),
  `getJournal`. `ACS_ArityComponentBase`: `occupied`, `registerIf`, `removeIf`, `catalogCount`, reads for
  int, long, double, boolean, Object and String, writes for long, double, boolean and String,
  `copy…Into(…, dest, offset)`; `ensureCapacity` and `truncate` take no comment.
- The memory rows have a no-argument constructor; the file rows take a `Path`
  (`IntegerValueCatalogFileInt(Path)`); `FileContentCatalogRow` takes another row.
- Stored components are `arity-<slot>.bin` and `structure-<slot>.bin` (`ACSE_CoreBasic:402`).
- `acses` holds `acsess` (`ACSES`, `ACSES_Request`) and `daemon` (`ACSES_Commands`, `ACSES_Daemon`,
  `ACSES_Database`, `ACSES_PersistenceJar`). The jar is `acses`, its main `org.halim.acses.acsess.ACSES`,
  which names the daemon by a string (`ACSES:29`).
- A connection carries one request: a count, then each argument as UTF-8 length and bytes; the answer is
  a status and one text (`ACSES_Request:21`). The daemon opens one Unix socket and blocks on `accept()`
  (`ACSES_Daemon:58`); its path is `$ACSES_SOCKET`, else under `$XDG_RUNTIME_DIR`, else the temporary
  folder, with the log and the lock beside it (`ACSES_Request:28`).
- No annotation or reflection exists in `acs`, `acse` or `acses`; the parent pom sets no `-parameters`.
- No command writes to a core yet, so every journal through the daemon is empty (53d82b3's message).
  Claude's check outside the repo: `~/mein/claude/scripts/acses-smoke/run.sh`, 137 checks at 53d82b3.

## The session

1. Checkpoint: present the design in ten lines, and ask the one name it leaves: the arity's requestable
   method for the occupied identities (Claude's suggestion: `occupiedIdentities`). Wait for the answer.
   Never discard uncommitted work.
2. The split: packages, the renames, the daemon's name string, the jar, the socket, lock, log and
   variable. Mechanical.
3. `acs`: `ACS_Actor` with its helper, and `ACS_Requestable`; every reference to
   `TraceableForUndoingRedoing` moves to `ACS_Actor`. The parent pom compiles with `-parameters`.
4. The journal: its actor ids as type and index; its marked methods, `request` and `getRequestables`;
   its step's error.
5. The core and the arity: the marked methods with their codes, the `addCatalogRow` overload, the
   occupied identities, `ensureCapacity`'s check (first check that no caller relies on it lowering the
   reach), the row files, `request`, `getRequestables`, the reads recorded as queries.
6. acsed's commands, under subject -1 and acsed's database type. First present their lines, one per command in the grammar, each
   keeping its meaning and flags, and wait for the author's approval; then convert them, with `probe`
   and `acsed-language.txt`.
7. acsed: `socket=` at boot; frames and connections; `acse-language.txt`; actor requests and their
   refusals; `acsed.databases` and the database indexes; `inspectAt` and `endInspection` moved to the
   journal, with the held waits answered; `migrate` and `storeDB toPath=` leaving the file out.
8. acsess: commands and addresses, `acsed.databases`, the probe, the files, the translation, the flags,
   the overloads, `row-<j>`, the flag reader and usage lines moved in, `beginSession` and `Commit` as
   frames, the printed results.
9. Verify: `mvn -o -q compile`. Extend Claude's `~/mein/claude/scripts/acses-smoke/run.sh`, keeping what
   it checks today through the new wire: boot with `socket=` and a later acsess finding it;
   `acse-language.txt` after `createDB` and `loadDB`, and a class appended; a probe; `addArity`,
   `addCatalogRow` by class name (a memory row and a file row), `captureIdentity`, a write and a read of
   each type through `row-<j>`, the occupied identities; a read recorded as a query; `ensureCapacity`
   below the reach keeping it; a write during `inspectAt` refused, and `waitInspectionReturn` answered by
   the journal's `endInspection`; `Commit` stopping at a refusal and going on with `+continueOnRefusal`;
   a stale class place refused; leftover bytes refused; an unloaded database's index not reused; the
   journal's step error. Tests in `src/test/` are the author's.
10. List the doc lines the work makes false (`CLAUDE.md` → Layout, `current-state.md`'s ACSES row, the
    README's commands, the javadocs naming `acses`), propose replacements, edit on approval.
11. Propose entries under Unconfirmed in `citable.md` for this round's decisions, quoting the
    author; add them on approval.
12. Stop: `git status`, the diff's summary, the checks' results. No commit until the author says.
