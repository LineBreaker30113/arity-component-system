# Current state

What exists now, checked against the repo on the date given, and the handoff. Rules: `CLAUDE.md` →
Markdown files.

## Layers — checked 2026-10-11

The live work is `incubation/acse_beta/`. `incubation/structurer/` is the previous layout, kept until
the author says otherwise; nothing in `acse_beta` imports it.

| Layer | Package | State |
|---|---|---|
| Identity sets | `acse_beta/sets/` | Negative at byte/short/int/long, positive at byte/short, Roaring. `lowestFree()`: fast on `NegativeInt`, a counting default elsewhere. |
| Catalog rows | `acse_beta/catalogs/` | Row contract `ACSE_ArityCatalogRowBase`; stored integer and floating rows, chunked; calculated rows `FilePathCatalogRow` and `HashedFilePathCatalogRow`. No setters yet. |
| Arity | `ACSE_ArityComponentBase`, `ACSE_ModularArityCatalog` | Occupied set plus rows; adding and removing rows and identities is journaled. `removeIf` and `registerIf` are for the core only. |
| Structures | `acse_beta/structures/unary/` | `TightSetHierarchyDirectedAcyclicIntGraphComponent` (always reduced) and `FolderHierarchyDirectedAcyclicIntGraphComponent` (single parent). Both journal their steps and save the relation as (element, parent) pairs. |
| Core | `ACSE_Core`, `ACSE_CoreBasic` | Components by slot, a removed slot left empty; `delegateRemoval` with the forget recursion; `captureIdentity`. Every request takes a comment. |
| Journal | `ACSE_Journal`, `ACSE_JournalBasic` | Transactions of nested requests; undo, redo, waypoints, `checkout`, abort; `storeAll` writes JSON Lines. |
| Procedures, persistence, filtering, `acses` | — | Do not exist. |

## Legacy features not yet in incubation — checked 2026-10-11

Locking, persistence sessions, filtering, analytics, in-place walking, and the service layer.
Undo/redo and logging now exist as the journal.

## Tests — measured 2026-10-11

`mvn test`: 202 tests, 0 failures. Nothing under `src/test/` exercises `acse_beta`; Claude's
throwaway checks of it live outside the repo (see Claude's memory).

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
