# Current state

What exists now, checked against the repo on the date given, and the handoff. Rules: `CLAUDE.md` →
Markdown files.

## Layers — checked 2026-09-17

| Layer | Package | State |
|---|---|---|
| Identity representation | `incubation/structurer/sets/` | Negative at byte/short/int/long, positive at byte/short, Roaring. |
| Data per identity | `incubation/structurer/catalogs/` | Column contract, integral and floating families, lister and chunked storage, six widths. |
| Arity | `incubation/structurer/ArityModule`, `ModularArityCatalog` | Occupied set plus columns, a cell addressed by `(catalog, identity)`. |
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

## Handoff — 2026-09-17

Work decided to be done and urgent.

- The urgent code work: `inferred-decisions.md` → Urgent work, not yet reviewed.
- Delete `ARCHITECTURE.md` and `GOALS.md`; their content is migrated. The author: "prepare to delete".
- Nothing is committed. The staged `ontructure/package-info.java` is the author's.
