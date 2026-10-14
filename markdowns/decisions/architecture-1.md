# Architecture (1 of 2)

- Arities are the first type of element; structures are the second.
- "Component" replaces "Module" as the word for plugins, in every name.
- No more "Module", we already settled to "Component", so "Modular" becomes "Composable"; just be
  clear what composable means. *(2026-10-13)*
- I guess we better rename the architecture to "plugin" based, since the core can be excluded.
- The "Structure" interface meant to be the second type of elements (the relations
  around sorts of elements (arity)) in the "OntologyEngineCore".
- We need to clarify each component is a component, so rename things like "ArityComponent" and
  "StructureComponent".
- The delegation-like actions should be done there [in the core].
- The core must also have contracts about delegation; we can (right now) declare an interface [for
  them].
- The ontructure package can be renamed but must be used, because incubation contains many unwanted
  elements that are not worth removing.
- The former version's missing features were critical for the application. I need the modularity and
  "Logging, Undo-Redo, ..." at the same time.
- The core handles creation-addition of plugins, delegations, journal (logging + undo/redo),
  persistence, filtering + analysis, also system analytics like how much RAM is used by/where, etc.
- [For `ontructure/`:] the project name became Arity Component System, so we will use "acs_beta"
  right now. [Since then: "acse replaces the acs", see Layers and names.]
- [Under `acs_beta`,] I plan on making a separate file for every top tier interface, ArityComponent
  variants and StructureComponent variants, and identitySet [`IdentitiesSet`]. Then packages for
  implementations. I prefer packages of "identifiers", "arity", "structures.<x>ary".
- [The `OE_` prefix, `OntologyEngineCore` and the package `org.halim.ontology`:] now the name is
  "ACS" [now `acse`], replace them with "ACS" [`acse`].
- [The ACS rename, now to `acse`:] do the one pass before the first push, so every import moves
  once. *(Claude's wording, confirmed by the author.)* [Since then: the messy version is uploaded
  first, see Layers and names.]
- "legacy" is read only. Renaming should not reach /legacy, but the `package <path>` header can
  change, no problem; for now we can keep renaming for the `incubation.ontructure`.
- [The `Module` at the end of `SetHierarchyDirectedAcyclicIntGraphModule`:] replace with
  `Component`.
- [Catalogs as components, `CatalogComponent`:] I accept it, but we must allow the client to decide
  to define catalogs without modules. I mean for each Arity we will have a table, and the client
  must be able to define [the] table itself or use a modular table. Also, for clarity, we will need
  to name them like "ArityCatalogTable" or "ArityModularCatalogTable" or "ArityCatalogRowComponent".
  [Since then: "Modular" becomes "Composable".]
- The `Structure` contract holds which arities it spans; removal notification and alike;
  per-element read and write of its own state; its op codes and codec. *(Old plan's wording,
  confirmed by the author.)*
- [The] Structure contract must allow a way of storing and loading the data via data streams; the
  core can manage files and alike.
- [Structures referring to their arities by ints, which the core resolves:] this has its own
  results. We cannot "insert" arities or structures, they become somewhat independent of core (any
  other core with different structures can accept it), using object references makes them less
  dependent on the core. So I am open to suggestions, but for now ints are enough.
- [The old plan's "growth is propagated directly from the arity to everything attached" is
  declined:] "different arity" == "different thing".
