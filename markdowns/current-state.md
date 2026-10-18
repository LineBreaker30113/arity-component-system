# Current state

What exists now, checked against the repo on the date given, and the former handoff. Rules: `CLAUDE.md` →
Markdown files.

## Layers — checked 2026-10-13; paths, format and acses 2026-10-18

The live work is the `acs` module (`org.halim.acs`, the contracts) and the `acse` module
(`org.halim.acse`, the classes); `deprecatedCode/` holds `org.halim.ontology`, outside the build.

| Layer | Package | State |
|---|---|---|
| Identity sets | `arity/sets/`, in `acs` and `acse` | Negative at byte/short/int/long, positive at byte/short, Roaring. `lowestFree()`: fast on `NegativeInt`, a counting default elsewhere. |
| Catalog rows | `arity/catalog/` in `acs`; `memory/`, `file/`, `calculated/` under it in `acse` | Row contract `ACS_ArityCatalogRowBase`; stored integer and floating rows, chunked; calculated rows `FilePathCatalogRow` and `HashedFilePathCatalogRow`. Stored rows take `setLongFor` and `setDoubleFor`, exact or refused, never growing; calculated rows refuse. |
| Arity | `ACS_ArityComponentBase`, `ACSE_ComposableArityCatalog` | Occupied set plus rows; adding and removing rows and identities is journaled, and so is a write (`setLongFor`, `setDoubleFor`), as `old ^ new` over the stored bits, within the arity's reach. `removeIf` and `registerIf` are for the core only. |
| Structures | `structure/unary/`, in `acse` | `TightSetHierarchyDirectedAcyclicIntGraphComponent` (always reduced) and `FolderHierarchyDirectedAcyclicIntGraphComponent` (single parent). Both journal their steps and save the relation as (element, parent) pairs. |
| Core | `ACSE_Core`, `ACSE_CoreBasic` | Components by slot, a removed slot left empty; `delegateRemoval` with the forget recursion; `captureIdentity`; `store` and `load` of a folder, between requests. Every request takes a comment. `ArityComponentSystem_Core_Deprecated`, an older interface nothing uses, is now in `deprecatedCode/`. |
| Journal | `ACSE_Journal`, `ACSE_JournalBasic` | Transactions of nested requests; undo, redo, waypoints, `checkout`, abort; `storeAll` writes JSON Lines. The core builds its journal from a factory, the basic one by default. An aborted client request still loses the undone transactions its start dropped. |
| Persistence | `ACSE_PersistenceManager`, `ACSE_PersistenceManagerBasic` | A store writes a file per arity and per structure, the descriptions, the journal's log, `indexer.bin` naming the core's class, the journal's, and every class used, and `classes.jar` holding them; a load builds that core and journal, then each part through its class's static `loadFrom`, taking the classes from the folder's jar. Format 3. The basic manager stores into numbered generations and points `acse-persistence.data` at the current one. |
| Descriptions | `ACSE_DescriptionManager`, on `ACSE_CoreBasic` | A string for the core, each arity, catalog row and structure, and each class by name; outside the journal; stored in `core.bin`. |
| Service | `acses`: `org.halim.acses.acsess` (the access side, JDK only) and `org.halim.acses.daemon` | `acses boot` starts the daemon as a separate process, under `setsid` or else `nohup`, on a Unix socket, `$XDG_RUNTIME_DIR/acses/acses.sock` unless `ACSES_SOCKET` names another, with the lock and the log beside it; it serves one request at a time. `createDB` writes `acse-persistence.jar`, builds the manager from it, and stores a new `ACSE_CoreBasic` through it; the other commands are `loadDB`, `components`, `storeDB`, `unloadDB`, `databases` and `shutdown`. Paths must be absolute. `mvn package` lays the program out in `acses/target/acses/`. |
| Procedures, filtering | — | Do not exist. |

## Legacy features not yet in incubation — checked 2026-10-11

Locking, filtering, analytics, in-place walking, and the service layer. Undo/redo and logging now
exist as the journal; storing and restoring, as folder persistence (2026-10-13).

## Tests — measured 2026-10-18

`mvn -o test` runs no tests: the 19 test files (182 tests, passing before the move) are in
`deprecatedCode/`, outside the build. Claude's throwaway checks live outside the repo (see Claude's
memory).

## Repository — checked 2026-10-14

On `incubation/int-set-algebra`, with `main` at the same commit, `a856b45`. `main` is pushed to
`origin`, https://github.com/LineBreaker30113/arity-component-system.git: public, `main` its default
branch, and GitHub detects the licence as AGPL-3.0 from `LICENSE`. The other local branches are not
pushed. The pre-purge bundle was deleted on 2026-10-13, so no copy of the history before the rewrite
is kept.

`markdowns/novelty-study.md` is published since `a856b45` (2026-10-14) and linked from the README.
Its working files are in `reference/novelty/`, gitignored: `novelty-study-2.md` (the later passes),
`checkpoints.md`, `search-log.md` and `novelty-handoff.md`.

Scan of the tracked tree and of all 44 commits, every one on `main`: no email, home path or secret in
any line ever added, and every commit carries the GitHub noreply address. The only home-style paths
are `~/main` and `~/db3`, examples inside the author's commands in `decisions.md` (Goal; Layers and
names). Tracked but not project work: `.mcp.json`, Claude Code's local link to IntelliJ, and
`src/main/java/Main.java`, IntelliJ's template.

## Former handoff — 2026-09-18

Kept as the record of where that session left things. The live one is in `inferred-decisions.md` →
Unreviewed → Handoff.

- The urgent code work: `inferred-decisions.md` → Urgent work, not yet reviewed.
- The author reviews the Unreviewed entries, the 19 term definitions among them, and decides the old
  plan's positions at the end of `questions.md`.
- `reference/` holds `pre-purge-backup.bundle` and `msgfilter.py`, both gitignored and both deletable
  once the history rewrite is trusted.

Repository state, 2026-09-18: committed through `10aa948`, working tree clean, no remote, nothing
pushed. History was rewritten this day — the study inventory and a third-party paper were purged from
every commit, and three commit messages that named the studied projects were reworded. The only copy
of the pre-purge history is that bundle.

Repository state, 2026-10-11: committed through `3df5498` on `incubation/int-set-algebra`, no remote,
nothing pushed; `.idea/inspectionProfiles/` and `.idea/prettier.xml` untracked.
