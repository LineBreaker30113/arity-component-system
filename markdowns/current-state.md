# Current state

What exists now, checked against the repo on the date given, and the handoff. Rules: `CLAUDE.md` →
Markdown files.

## Layers — checked 2026-10-13

The live work is `incubation/acse_beta/`. `incubation/structurer/` is the previous layout, kept until
the author says otherwise; nothing in `acse_beta` imports it.

| Layer | Package | State |
|---|---|---|
| Identity sets | `acse_beta/sets/` | Negative at byte/short/int/long, positive at byte/short, Roaring. `lowestFree()`: fast on `NegativeInt`, a counting default elsewhere. |
| Catalog rows | `acse_beta/catalogs/` | Row contract `ACSE_ArityCatalogRowBase`; stored integer and floating rows, chunked; calculated rows `FilePathCatalogRow` and `HashedFilePathCatalogRow`. Stored rows take `setLongFor` and `setDoubleFor`, exact or refused, never growing; calculated rows refuse. |
| Arity | `ACSE_ArityComponentBase`, `ACSE_ComposableArityCatalog` | Occupied set plus rows; adding and removing rows and identities is journaled, and so is a write (`setLongFor`, `setDoubleFor`), as `old ^ new` over the stored bits, within the arity's reach. `removeIf` and `registerIf` are for the core only. |
| Structures | `acse_beta/structures/unary/` | `TightSetHierarchyDirectedAcyclicIntGraphComponent` (always reduced) and `FolderHierarchyDirectedAcyclicIntGraphComponent` (single parent). Both journal their steps and save the relation as (element, parent) pairs. |
| Core | `ACSE_Core`, `ACSE_CoreBasic` | Components by slot, a removed slot left empty; `delegateRemoval` with the forget recursion; `captureIdentity`; `store` and `load` of a folder, between requests. Every request takes a comment. `ArityComponentSystem_Core_Deprecated` is an older interface nothing uses. |
| Journal | `ACSE_Journal`, `ACSE_JournalBasic` | Transactions of nested requests; undo, redo, waypoints, `checkout`, abort; `storeAll` writes JSON Lines. The core builds its journal from a factory, the basic one by default. An aborted client request still loses the undone transactions its start dropped. |
| Persistence | `ACSE_PersistenceManager`, `ACSE_PersistenceManagerBasic` | A store writes a file per arity and per structure, the descriptions, the journal's log, `indexer.bin` naming the core's class, the journal's, and every class used, and `classes.jar` holding them; a load builds that core and journal, then each part through its class's static `loadFrom`, taking the classes from the folder's jar. Format 2. The basic manager stores into numbered generations and points `acse-persistence.data` at the current one. |
| Descriptions | `ACSE_DescriptionManager`, on `ACSE_CoreBasic` | A string for the core, each arity, catalog row and structure, and each class by name; outside the journal; stored in `core.bin`. |
| Procedures, filtering, `acses` | — | Do not exist. |

## Legacy features not yet in incubation — checked 2026-10-11

Locking, filtering, analytics, in-place walking, and the service layer. Undo/redo and logging now
exist as the journal; storing and restoring, as folder persistence (2026-10-13).

## Tests — measured 2026-10-13

`mvn test`: 182 tests, 0 failures. Nothing under `src/test/` exercises `acse_beta`; Claude's
throwaway checks of it live outside the repo (see Claude's memory).

## Repository — checked 2026-10-13

On `incubation/int-set-algebra`, with this session's work committed, and `main` fast-forwarded to
it. The remote `origin` is https://github.com/LineBreaker30113/arity-component-system.git, which the
author created; nothing is pushed. The pre-purge bundle was deleted on 2026-10-13, so no copy of the
history before the rewrite is kept.

Scan of the tracked tree and of the whole history: no email, home path or secret in any line ever
added, and every commit carries the GitHub noreply address. The only home-style paths are `~/main` and
`~/db3`, examples inside the author's commands under Goal. Tracked but not project work: `.mcp.json`,
Claude Code's local link to IntelliJ, and `src/main/java/Main.java`, IntelliJ's template.

## Former handoff — 2026-09-18

Kept as the record of where that session left things. The live one is `handoff.md`.

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
