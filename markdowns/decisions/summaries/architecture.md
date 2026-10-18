# Architecture — summary

Claude's summary of `decisions.md` → Architecture and `architecture.md`; written 2026-10-18. A
reading aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

## General

- Arities are the first element type.
  > “Arities are the first type of element”
- No more Module: Component replaces it, Composable replaces Modular; be clear what composable means
  (2026-10-13).
  > “we already settled to "Component"”
- Composable is the vocabulary; plugin based was left, the core being too thin for plugins
  (2026-10-15).
  > “plugin based is an architecture we left”
- Structure is the second element type: relations around arities.
  > “the second type of elements”
- The core handles the database side; components can delegate themselves but must pass it through
  the core, for future plans (2026-10-15).
  > “they must pass it through the core”
- Right now the core is almost redundant, as arities and structures can work alone; it stays for
  future purposes and organization; ACS doesn't have a core, ACSE does (2026-10-15).
  > “the core is almost redundant” · “The ACS doesn't have a core”
- Composability and logging/undo-redo are needed together; missing them was critical in the former
  version.
  > “I need the modularity and "Logging, Undo-Redo, ..." at the same time”
- The core creates and adds components, delegates, journals, persists, filters and analyses, and
  reports system analytics like RAM use.
  > “creation-addition of components” · “system analytics”
- Legacy is read only: renaming should not reach it, except package headers, no problem; for now
  renaming continues in incubation.
  > “"legacy" is read only” · “the `package <path>` header can change”
- Structures refer to arities by core-resolved ints: arities and structures cannot be inserted, they
  become somewhat core-independent (another core can accept them), and object references would make
  them less dependent; for now ints are enough.
  > “We cannot "insert" arities or structures” · “for now ints are enough”
- Components can use object pointers instead of ints, but not taken: the journal must be handled
  with the core, and future features may reason about several arity, structure and procedure
  instances (2026-10-15).
  > “We do not go with that idea” ·
  > “reason about the multiple of arity, structure and procedure instances”
- Growth is not propagated from an arity to everything attached: a different arity is a different
  thing.
  > “"different arity" == "different thing"”
- Nothing narrows silently: exact or throw (UnsupportedOperationException for a wrong type,
  ArithmeticException when the value does not fit); floating narrowing rounds.
  > “Nothing narrows silently”
- Many structure components are needed; ternary examples would be an arity as the cartesian product
  of two others, or as transitions between arities.
  > “We need to define many Structure Components”
- Structures ensure their own rules (2026-10-10).
  > “The structures ensure their rules themselves”
- Procedures are the third component type: functions about ACS that benefit from being close to the
  core.
  > “a third type of components, the "procedures"”
- Procedures may and will change catalog data; storing those changes is not hard: extend the arity
  operations list.
  > “Procedures may and will change catalog data” · “extending the arity operations list”
- Procedures, only for changes or also for reads: they must allow reasoning, or at least state that
  they do not.
  > “it must allow a way of reasoning”
- Procedures must be clear enough that ACS can reason about them automatically (2026-10-15).
  > “automatically reasoned with our ACS system”

## Details

- The core must also have delegation contracts; an interface can be declared right now.
  > “contracts about delegation”
- A file for every top-tier interface, ArityComponent and StructureComponent variant, and
  IdentitiesSet; implementations in packages "identifiers", "arity", "structures.<x>ary".
  > “a separate file for every top tier interface” · “"structures.<x>ary"”
- Catalog components are accepted, but the client must be able to define each arity's table itself
  or use a composable one; names like ArityCatalogTable, ArityModularCatalogTable,
  ArityCatalogRowComponent.
  > “define catalogs without modules” · “"ArityCatalogRowComponent"”
- The Structure contract: spanned arities, removal notification, per-element read and write of its
  own state.
  > “removal notification and alike”
- The Structure contract must allow storing and loading via data streams; the core can manage files.
  > “via data streams”
- Scattered catalogs are unlikely, and a name-to-slot method is not necessary right now.
  > “"scattered" is unlikely for the assumption”
- Not all catalogs are pure array lists; they can be calculations using the identity.
  > “they can be calculations”
- The synapse structure maps two elements of one arity to one element of a second; all neurons on
  arity 1, every weight on arity 2; it resembles a category.
  > “it is just a type of 2 arity structure” · “resembles a category”
- Two-arity structures come in many forms; a one-arity structure is always a graph.
  > “always a type of graph”
