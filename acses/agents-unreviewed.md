# acses: entries to review

# Unreviewed

- Later, in `acses`: commands that interact with the arities and that query data, and custom class
  files handled properly, with an option mechanism beside a list function. *(Author, 2026-10-18, in
  the 2026-10-18 `plan.md` (not in git) → P2: "Since the database is really modular, and we made the Composable Arity instance, we
  must implement a way of interacting with the arities. Also, there must be methods of querying
  data."; answering P2: "since we plan allowing custom class files, we need to handle it properly,
  but later. Certainly an option mechanism along side a list function necessary.")*
  Section: Layers and names / Details
- The return from an inspection takes no argument and is `endInspection`. *(Author, 2026-10-19:
  "Int in "uncheck" was a mistake."; `endInspection` picked among Claude's options.)*
  Section: Journal and undo
- `storeDB toPath=` stores a copy elsewhere and is never refused for an inspection; a plain `storeDB`
  is refused during one unless `+ignoreInspection`. *(Author, 2026-10-19: "the "storeDB" command must
  have an optional "--toPath" parameter for specifying the store path, so an agent can store a
  checkout version somewhere else if it wants."; "Yes, refuse the plain storeDB except if there is
  "--ignoreCheckOut"."; "The --toPath is a copy".)*
  Section: Persistence
- `migrate` moves a database to a new folder, with `newName=`, `+WithFormerVersions`,
  `+WithOtherFiles` (every other entry at the root) and `+deleteOld` (Claude's name); it is refused
  during an inspection unless `+ignoreInspection`, and the database then uses the core loaded back
  from the new folder. *(Author, 2026-10-19: "we can make a "migrate" command, which has an optional
  flag for "--AllFiles" (files on the folder root not recognized by ACS) and "--newName <newName>"
  for giving it a new name."; on deleting the old folder and copying all or the current state only:
  "Make it a command flag"; "I want a "--WithFormerVersions" and an "--WithOtherFiles" flag that does
  what they say."; "Every file at the root of the database."; the loaded-back core picked among
  Claude's options.)* Conflicts: `plan.md` at `31e0194` → P9's `--AllFiles` and `--currentOnly`, replaced; the
  newer is taken.
  Section: Persistence
- The rollback is `inspectAt`, and the wait splits in two: `inspectionState` probes at once,
  `waitInspectionReturn` holds until the return. *(Author, 2026-10-19: "The name "wait" is vauge and
  can do two different things, I want two commands with better names."; "The checkout command (used
  to) mean rolling back to a certain state of a database, and I found the name inspectAt better than
  it since it is a serious reroll."; "If the goal is to probe state of inspection, go with 2";
  `waitInspectionReturn` picked as the spelling.)*
  Section: Journal and undo
- `beginSession` sends nothing until `Commit`, which sends the queue together; the first refusal
  stops the rest unless `+continueOnRefusal`; values may be quoted with `"`, `` ` ``, ```` ``` ```` or
  `"""`, the last two across lines. *(Author, 2026-10-19: ""beginSession" (which means the terminal
  keep recieving until "Commit" is called, like a python terminal idle)"; "The session doesn't send
  anything until commit, then all request will be queued togather."; "Make the stop default and go
  on a command flag."; ````"The commands must allow value names typed in `"` or "`" and "```" (multi
  line) or `"""`."````; Commit ending the prompt only and the flag's name picked among Claude's
  options.)*
  Section: Layers and names / Details
- `acquire` gives a key that every request it covers must give as its first argument, for
  operational safety, held in the daemon's memory only; its scope and `+changesOnly` are flags, and
  it ends with `release`, or `release +force` without the key. *(Author, 2026-10-19: "a "lock"
  command wich returns a key and for the rest of the requests will require the first argument to be
  the key, not for security but operational safety."; "Make all of the options available as command
  flags."; "b: good, we assume the requester has good intentions, with only a change to
  malfunction"; "c: No, locks are not mandotary, if the data base fails mean time the lock better get
  reaccuired"; "d: Yes, other solutions will be for other cases"; "I got the "lock" from mutex
  instances."; acquire and release picked among Claude's options.)*
  Section: Layers and names / Details
- `loadDB` of a folder another name holds needs `+Duplicate`; given where nothing else holds the
  folder, it only warns. *(Author, 2026-10-19: "a: make it mandotary to have the flag called
  "duplicate", if there is duplicate but not another instance pointing on it, only warn.")*
  Section: Layers and names / Details
- Switches take the shape `+Name` and valued flags `name=value`, so no name is mistaken for a flag.
  *(Author, 2026-10-19: "we need to ensure no name starts with "--" or find a better flag prefix.";
  "flags cannot become parameters but they can take the shape +Duplicate, and parameter flags can
  become toPath=, so cool.")* Conflicts: `plan.md` at `31e0194` → P5's `--` flags, replaced; the newer is taken.
  Section: Layers and names / Details
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
  (root 17, root 18), acsess being the requester; "it must launch a separate process"
  and "will launch [the database engine] if and only if (boot is the command and there is not anyone
  running)", now acsess launching acsed. Complements: "ACSES uses it for [the] database service"
  (root 28) keeps ACSES as the project
  holding both; "many ways of accepting requests" gains that boot chooses them. No conflict found.
  Section: Layers and names
- acsess translates names into codes; a probe answers where an actor's class sits in the file, which is
  rewritten only when a new class comes; acsed reads no name to pick a method. *(Author, 2026-10-20: "I
  suggest a "probe" request which make the instance load its method data from the file. So file is
  rewritten only when a new class is imported."; "I decided the daemon is not responsible of translating
  the commands, they come as raw data, and the ACSESS translates commands to data."; ["The method name is
  a huge No." means] "the fact ACSED is not responsible with reading names to check which method.")*
  Commons: "can only request" (root 47), acsess being the client.
  Section: Layers and names / Details
- A request's address is plain ints: the subject, a database's index or acsed; within a database the
  type, 0 the core, -128 the journal, 1 an arity, 2 a structure, 4 a procedure, 9 an arity row.
  *(Author, 2026-10-20: "0 means core, -128 means journal, 1 is for arity, 2 for structures, 4 for
  procedures, 9 for arity rows"; "we can simply use positive values for database index and negative
  values for ACSED commands."; "Also commands can be long, we do not need to limit ourselves to bytes.
  Why optimize the command length for bytes of data?")*
  Section: Layers and names / Details
- A database is an index, listed with its name and folder in a file acsed writes; acsess turns a name
  into its index; a database's name holds no colon. *(Author, 2026-10-20: "Data base must also be an
  index, so a file listing data base names with paths with their indexes are the right choice."; "So the
  acsess reads the data base name and gives the index to the acsed. ACSED meant to be a pure server as I
  said before, and ACSESS is the command line tool to connect to it locally."; "Instead we forbid the
  users to use `:` on the database name")*
  Section: Layers and names / Details
- The core and the journal are addressed by the database's name, `main:`, and also as `main:core` and
  `main:journal`. *(Author, 2026-10-20: "I thought both of them [the core and the journal] can be
  expressed as the name of the database"; "Also allow the `main:core` and `main:journal` actions for
  clarity.")*
  Section: Layers and names / Details
- Separate projects for ACSESS and ACSED are a goal. *(Author, 2026-10-20: "making a separate maven
  module for the daemon is one of the (future) goals."; "I meant separate projects for ACSESS and ACSED
  is a goal.")*
  Section: Layers and names
