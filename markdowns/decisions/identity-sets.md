# Identity sets — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

- Add [Positive]Int, also deleting the narrow variants, to the plan.
- Some arities may have elements up to a certain number easily computed, like say we have a single
  layer fully [connected] perceptron: its nodes are one arity, its weights are a different arity,
  and its count and range can be easily computed, so the client can implement a specific identity
  set if it wants.
- Right now the engine accepts such a thing with ease, so we just need to document it. Later we may
  need to make a few additions for it to work.
- I think we need to find ways to define arity sizes; for example, [in] the "cartesian" instance I
  told, the arity that represents the cartesians can have its size calculated.
- [May identities be long?] Long imv is out of scope, but yes they can. *(2026-10-15)*
