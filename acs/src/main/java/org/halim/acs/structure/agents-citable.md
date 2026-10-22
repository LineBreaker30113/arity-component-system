# acs/structure: citable

## Architecture

- **1.** We need to define many Structure Components. A ternary example would be assuming an arity is [the]
  cartesian [product] of two other arities; another would be assuming an arity is some transitions
  from the one arity to another.

### Details

- **2.** The `Structure` contract holds which arities it spans; removal notification and alike; per-element
  read and write of its own state. *(Old plan's wording, confirmed by the author.)*
- **3.** There [are] many ways for two arity structures to exist, where a single arity structure is always a
  type of graph.

## Journal and undo

### Details

- **4.** "addParent" under isTight cannot be inverted by undoing all in reversed order; it can be reverted
  by undoing all in the same order; also, it can be undone by closing the isTight temporarily.
