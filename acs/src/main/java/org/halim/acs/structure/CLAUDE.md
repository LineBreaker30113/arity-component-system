# acs/structure

- Many structure components are needed; ternary examples would be an arity as the cartesian of two
  others, or as transitions between arities (1).
- The contract holds its arities, removal notification and per-element read and write of its state (2).
- Two-arity structures vary; a one-arity structure is always a graph (3).
- Under isTight, `addParent` cannot be inverted by undoing all in reverse, but can by undoing all in
  order or by closing isTight for a while (4).
- `binary/CLAUDE.md`: the synapse structure.
