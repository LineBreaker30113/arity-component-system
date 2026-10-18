# Architecture — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

- The core must also have contracts about delegation; we can (right now) declare an interface [for
  them].
- [Under `acs_beta`,] I plan on making a separate file for every top tier interface, ArityComponent
  variants and StructureComponent variants, and identitySet [`IdentitiesSet`]. Then packages for
  implementations. I prefer packages of "identifiers", "arity", "structures.<x>ary".
- [Catalogs as components, `CatalogComponent`:] I accept it, but we must allow the client to decide
  to define catalogs without modules. I mean for each Arity we will have a table, and the client
  must be able to define [the] table itself or use a modular table. Also, for clarity, we will need
  to name them like "ArityCatalogTable" or "ArityModularCatalogTable" or "ArityCatalogRowComponent".
  [Since then: "Modular" becomes "Composable".]
- The `Structure` contract holds which arities it spans; removal notification and alike; per-element
  read and write of its own state. *(Old plan's wording, confirmed by the author.)*
- [The] Structure contract must allow a way of storing and loading the data via data streams; the
  core can manage files and alike.
- [Gather, and resolving a catalog's name to its slot:] "scattered" is unlikely for the assumption,
  and [the] naming method is not necessary right now.
- Not all the catalogs are meant to be pure array lists; they can be calculations using the identity
  in the arity.
- [The synapse structure:] it is just a type of 2 arity structure where two elements from a single
  arity [are] matched to a single element on the second arity. All of the neurons on all the layers
  are placed on arity 1, and every weight is placed on arity 2; [the] structure resembles a category
  (in [the] mathematical sense), since there [are] two sets and [the] second set binds to [the]
  cartesian of the first set with [the] first set.
- There [are] many ways for two arity structures to exist, where a single arity structure is always a
  type of graph.
