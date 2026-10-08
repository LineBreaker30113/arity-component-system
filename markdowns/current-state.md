# Current state

What exists now, checked against the repo on the date given, and the handoff. Rules: `CLAUDE.md` →
Markdown files.

## Layers — checked 2026-09-17

| Layer | Package | State |
|---|---|---|
| Identity representation | `incubation/structurer/sets/` | Negative at byte/short/int/long, positive at byte/short, Roaring. |
| Data per identity | `incubation/structurer/catalogs/` | Column contract, integral and floating families, lister and chunked storage, six widths. |
| Arity | `incubation/structurer/ArityModule`, `ACSE_ModularArityCatalog` | Occupied set plus columns, a cell addressed by `(catalog, identity)`. |
| Structures | `incubation/structurer/structures/` | One: the DAG. `indexers/` is an empty directory. |
| Engine | — | Does not exist. `incubation/OntologyEngineCore` was an earlier attempt; its structure list is typed to one class, `BidirectionalIndexingService`. |
| Cross-cutting concerns | — | Do not exist. |
| New home | `incubation/ontructure/` | Only the author's staged `package-info.java`. |

## The DAG — checked 2026-09-17

`structures/graphs/SetHierarchyDirectedAcyclicIntGraphModule`: a set hierarchy over int identities.
One int per element holds a single parent inline; a second parent or any child gives the element a
`multiData` entry. The root is element 0, and its children live in slot 0. Every `addParent` walks the
element's descendants to refuse a cycle; under `isTight` it also keeps the parent set reduced and
tightens the element's descendants.

Not checked: whether loading with `isTight` off, then calling `tighten(ROOT)` once, gives the same
parent sets as loading with it on. It is on the DAG test list.

## Legacy features not yet in incubation — checked 2026-09-17

Locking, undo/redo, logging, persistence sessions, filtering, analytics, in-place walking, and the
service layer over them.

## Tests

`mvn test`: 182 tests, 0 failures (legacy suite plus `OEIntSetListerTest`), measured 2026-09-09.
Nothing under `src/test/` exercises `structurer/structures/` (checked 2026-09-17).

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
