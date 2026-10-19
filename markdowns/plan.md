# Plan — checkout and the ACSES commands

Written 2026-10-19 by Claude, for one execution session, which stops for the author's review and commit.
Every line rests on the
author's words quoted here, a fact checked on 2026-10-19, or is marked as Claude's proposal. The Citable
wins any conflict: stop and ask.

## Decided

The author, 2026-10-19, after adding the method to `ACS_Journal` "so the client can check other places":

- "Refuse write loudly, and add a command to "wait" for returning to the former version, the command
  returns the current activity index and how many activities are pending." Pending, picked among
  Claude's options: "Activities after the current point".
- "Int in "uncheck" was a mistake." The author has since named it `returnFromCheckout()`.
- "the "storeDB" command must have an optional "--toPath" parameter for specifying the store path, so an
  agent can store a checkout version somewhere else if it wants."
- "Yes, refuse the plain storeDB except if there is "--ignoreCheckOut"."
- "The --toPath is a copy, we can make a "migrate" command, which has an optional flag for "--AllFiles"
  (files on the folder root not recognized by ACS) and "--newName <newName>" for giving it a new name."
- Picked among Claude's options: during a checkout, `migrate` is refused unless `--ignoreCheckOut`, and
  `storeDB --toPath` is not. Whether `migrate` deletes the old folder, and whether it copies the whole
  folder or the current state only: "Make it a command flag".

Kept from round 1, in the code: paths must be absolute; `createDB` refuses a folder holding files; the
daemon answers one request at a time (`ACSES_Daemon:60`).

## Facts, checked 2026-10-19

- `ACSE_JournalBasic` keeps one line of transactions. `checkout` moves the applied count `done`
  (`moveTo`); the next `beginRecordingActivity` calls `dropRedo()`, which deletes every transaction
  after `done`. So a write after a checkout would delete the way back.
- `ACS_Journal` declares `returnFromCheckout()`, the author's uncommitted change; implementing it in
  `ACSE_JournalBasic` is this session's (step 2). Until then `mvn -o -q compile` fails.
- `ACS_Core` gives no way to its journal; `ACSE_CoreBasic` has a public field `journal`. A database's
  core may load from the folder's own jar, so ACSES cannot cast it to the program's `ACSE_CoreBasic`.
- ACSES's request reader knows two flags only, `--comment` and `--calledBy` (`ACSES_Commands:270`).
- The author's uncommitted work: `ACS_Journal`, `ACS_ArityComponentBase`, `ACS_ArityCatalogRowBase`,
  `ACSE_ComposableArityCatalog`, three `package-info.java`, and new files `BooleanValueCatalog`,
  `StringValueCatalog` and the `StringValueCatalog*` and `BooleanValueCatalogBits` classes.

## Proposals — approved

Claude's, approved by the author on 2026-10-19: "Your proposals are approved."

- **P1. Contract lines the author writes**, since interfaces are the author's: on `ACS_Core`,
  `ACS_Journal getJournal()`; on `ACS_Journal`, `boolean isCheckedOut()`, `int getCurrentActivityIndex()`
  and `int getPendingActivityCount()`. ACSES needs each: to refuse, and to answer `wait`.
- **P2. The current activity index** is the last request of the last applied transaction, -1 when none
  is applied; **pending** counts the requests in the transactions after it.
- **P3. The return point.** The first checkout saves `done`; a second checkout, and undo or redo during
  a checkout, move without changing it. `returnFromCheckout()` moves to it and forgets it; without a
  checkout it is refused. The return point is not stored, so `FORMAT` stays 0.
- **P4. The refusal.** During a checkout, `beginRecordingActivity` throws `IllegalStateException`
  naming the checked-out request and `returnFromCheckout`; nothing is recorded or dropped.
- **P5. Flags.** One reader for every command: with a value `--calledBy`, `--comment`, `--toPath`,
  `--newName`; without one `--ignoreCheckOut`, `--AllFiles`, `--deleteOld`, `--currentOnly`. A flag the
  command does not take is refused, and its usage line lists the ones it takes.
- **P6. Commands** `checkout <name> <activityIndex>` and `returnFromCheckout <name>`, each answering the
  current activity index and the pending count.
- **P7. `wait <name>`.** Without a checkout it answers at once. Otherwise the daemon keeps the
  connection open and answers it when `returnFromCheckout` runs; `unloadDB` and `shutdown` answer the
  waiting ones with a refusal. The single thread stays free, so other requests go on.
- **P8. `storeDB <name> [--toPath <path>] [--ignoreCheckOut]`.** Plain, during a checkout: refused
  unless `--ignoreCheckOut`. `--toPath`: an absolute folder, absent or empty, gets
  `acse-persistence.jar` as at `createDB`, then the store; the database stays on its own folder.
- **P9. `migrate <name> <path> [--AllFiles] [--newName <newName>] [--deleteOld] [--currentOnly]
  [--ignoreCheckOut]`.** The flag names `--deleteOld` and `--currentOnly` are Claude's.
  - The target is absolute, absent or empty. Without `--currentOnly`, the files ACS recognizes are
    copied (the pointer, the manager jar, the stored versions), then a store is made; with it, as
    `--toPath`. `--AllFiles` also copies the root files ACS does not recognize.
  - The loaded database then uses the new folder, under `--newName` if given; a name in use is refused.
  - `--deleteOld` deletes the old folder only after the new one loads back; without it, the old one
    stays.

## The session

1. Checkpoint, and wait for the author: whether the uncommitted work above is committed first (never
   discard it); whether P1's contract lines are written. They are the author's: do not write them.
2. The journal: `ACSE_JournalBasic` implements `returnFromCheckout()` and P1's journal lines by P2–P4;
   `ACSE_CoreBasic` implements `getJournal()`.
3. ACSES: the flag reader (P5); `checkout`, `returnFromCheckout`, `wait`, `storeDB`, `migrate` (P6–P9);
   the `COMMANDS` list and usage lines.
4. Verify: `mvn -o -q compile`. Extend Claude's `~/mein/claude/scripts/acses-smoke/run.sh`: checkout, a
   write refused, `storeDB` refused, with `--ignoreCheckOut`, with `--toPath`; `wait` answered by a
   return and at once without a checkout; `migrate` with each flag. Tests in `src/test/` are the author's.
5. List the doc lines the work makes false (`current-state.md`'s ACSES row, `ACSES_Commands`' javadoc,
   the README's commands if any), propose replacements, edit on approval.
6. Propose entries under Unreviewed in `inferred-decisions.md` for Decided above, quoting the author;
   add them on approval.
7. Stop: `git status`, the diff's summary, the checks' results. No commit until the author says.
