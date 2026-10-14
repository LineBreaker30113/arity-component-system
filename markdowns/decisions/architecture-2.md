# Architecture (2 of 2)

- Nothing narrows silently: conversion is exact or throws. `UnsupportedOperationException` = wrong
  type asked; `ArithmeticException` = value does not fit. Floating narrowing rounds instead. *(Old
  plan's wording, confirmed by the author.)*
- [Gather, and resolving a catalog's name to its slot:] "scattered" is unlikely for the assumption,
  and [the] naming method is not necessary right now.
- We need to define many Structure Components. A ternary example would be assuming an arity is [the]
  cartesian [product] of two other arities; another would be assuming an arity is some transitions
  from the one arity to another.
- Not all the catalogs are meant to be pure array lists; they can be calculations using the identity
  in the arity.
- The structures ensure their rules themselves. *(2026-10-10)*
- Most importantly, I plan on adding a third type of components, the "procedures", which are
  functions about ACS that benefit being close to the system core.
- Procedures may and will change catalog data; it is like their goal, because they are meant to be
  close to the ACS. We need a way to store changes on catalog data, which is not hard, just extending
  the arity operations list.
- [Procedures, only for changes or also for reads:] it must allow a way of reasoning, or at least
  state they do not otherwise.
- [The synapse structure:] it is just a type of 2 arity structure where two elements from a single
  arity [are] matched to a single element on the second arity. All of the neurons on all the layers
  are placed on arity 1, and every weight is placed on arity 2; [the] structure resembles a category
  (in [the] mathematical sense), since there [are] two sets and [the] second set binds to [the]
  cartesian of the first set with [the] first set.
- There [are] many ways for two arity structures to exist, where a single arity structure is always a
  type of graph.
