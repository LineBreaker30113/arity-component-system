# Target use case

Claude's wording, from `GOALS.md`; same source as Words.

- **Identities are dense and local to an arity**, minted from 0, so their values stay small. Ends if
  identities become global across arities or carry generation counters.
- **Edits may be slow; reads may not.** Write time is spent to buy read speed or exactness.
- **Modularity the fullest** The engine must be able to express anything.
- [The DAG is] optimized for the case where the leaves are more than branch nodes, and parent*child
  count mostly does not exceed a constant. *(Javadoc of `SetHierarchyDirectedAcyclicIntGraphModule`.)*
