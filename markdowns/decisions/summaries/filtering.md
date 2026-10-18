# Filtering — summary

Claude's summary of `decisions.md` → Filtering and `filtering.md`; written 2026-10-18. A reading
aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

## General

- Filtering offers logical combinations (AND, OR, SUBTRACT, NOT, NAND, NOR, XOR, IFOF, IMPLIES,
  NIMPLIES, CONTRADICTS, NCONTRADICTS) and data-dependent filters like namedLike or childOf; it is
  essentially a query service.
  > “essentially a query service” · “"namedLike" or "childOf"”

## Details

- Right now the service can only offer the logical operators and data filters are the client's,
  somewhat intended; but some filtering modules must be done.
  > “filtering modules to simplify the client's experience must be done”
- Named OR variants like IMPLIES are all redundant; keep logic minimal right now.
  > “all are redundant”
- The OR variants are boilerplate, handled last.
  > “meant to be handled last”
