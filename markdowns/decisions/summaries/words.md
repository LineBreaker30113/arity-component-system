# Words — summary

Claude's summary of `decisions.md` → Words; written 2026-10-18. A reading aid, not a source: the
decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

The first five are Claude's definitions, accepted by the author; the last two are the author's.

- Ontology: the philosophical sense; not OWL, RDF or a reasoner.
  > “Not OWL, RDF, or a reasoner”
- Arity: one identity space, the occupied set plus its columns; as a number, how many spaces a
  structure spans.
  > “the occupied set plus its columns”
- Catalog: one value per identity; an unwritten slot reads as zero, not absent.
  > “reads as zero, not absent”
- Structure: data about relations over one arity or several, under any rule.
  > “under any rule”
- Component: an arity, structure or procedure the core holds.
  > “an arity, structure or procedure the core holds”
- An element's identity is an int value; its index counts identities with smaller values. "Identity"
  and "index" are no longer used, not distinctive enough: iui (uniqueness index) and imv (memory
  value), with iui <= imv. Structures use the stable imv; sets map iui to imv, iui being the actual
  identity. Vacuum, now compacten, makes all iui equal their imv (2026-10-15).
  > “We no longer use the words "identity" or "index"” · “the notation "iui <= imv"” ·
  > “sets map the iui to imv”
- Compacten removes unnecessary space without changing the structure of data (2026-10-15).
  > “without changing the structure of data”
