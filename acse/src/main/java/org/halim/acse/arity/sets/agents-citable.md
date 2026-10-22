# acse/arity/sets: citable

## Identities and removal

### Details

*("vacuum" in this section is renamed to "compacten" at the author's request, 2026-10-15.)*

- **1.** The removal and binary search [over the hole list] likely will never get that slow.

## Identity sets

### Details

- **2.** Add [Positive]Int, also deleting the narrow variants, to the plan.
- **3.** Some arities may have elements up to a certain number easily computed, like say we have a single
  layer fully [connected] perceptron: its nodes are one arity, its weights are a different arity,
  and its count and range can be easily computed, so the client can implement a specific identity
  set if it wants.
- **4.** Right now the engine accepts such a thing with ease, so we just need to document it. Later we may
  need to make a few additions for it to work.
