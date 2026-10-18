# Identities and removal (1 of 2) — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

*("vacuum" in this section is renamed to "compacten" at the author's request, 2026-10-15.)*

- I plan on using the compacten idea where the index of an identity is converted to identity at the
  ArityModule's identitiesSet.
- If the identity number rather than the index is given (which is handled by the identities set of
  the ArityComponent), it auto skips the removed ones; we just need to specify that the intended
  behavior is using the arity component like that.
- When execution (through the multi-arity structures, and maybe few other cases I do not remember
  right now) reaches identities, they check if they exist, when necessary.
- [On the above:] the important detail is that the structures are also better off not shifting data,
  so they point at the index rather than the identity; by "they check when necessary" I mean we need
  to handle this phenomenon.
- [Index there is the memory address where the record's data sits, which does not shift on removal:]
  Yes, it is [that], but [it] would not cause any problem; the functions just need to transform the
  identity number before using it.
- I have a belief the structures do not need to check if an Identity exists.
- Structures, if they need to remove an identity on another arity, need to call ArityComponent's
  remove on that identity; that is the recursive step: 0 original arity -> 1 related structures -> 2
  start from (0) on each identity needed to [be] removed.
- Arity removes the identity and calls the core forget for delegating it. *(2026-10-15)*
- Forget needs to be undoable.
- We will not need a function like "swap" or "captureNotForget" because of what an arity represents;
  more on that later.
- [Storing a structure:] recording the relation is enough. Also at the core level storing can be
  done by temporarily storing, then compacten, then final storage. [That] is not bound to be the only
  [sequence]; allow [the] option of direct storing.
- [Calculated arities:] the calculated arities must be dependent on a structure; this is better
  since being directly dependent [on] another Arity can actually raise errors.
- The removal and binary search [over the hole list] likely will never get that slow.
- Structures are not responsible for the element count; it is just the structure add-on.
- I think the compacten will be a rare call, on storing to disk and reloading from it.
- We must make a "forget" call that removes the associated data (like the connections on the indexer)
  but doesn't shift all the identities. Delegations over that call will be handled by the core.
- No structure compactens a dead identity it reaches on its own.
- Bulk catalog reads intersect the range with the occupied set once, then read, instead of checking
  each element.
- We need to allow client to skip the validation of identities, when they likely know it.
- The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. If checking is requested by client.
- I think we do not need to make pending removals; the identity sets handle it.
- [Capturing back:] identity set must allow a way of capturing the lowest positive
  excluded value. I mean there is an IntArrayList of excluded [identities]: [its] first element if
  the list is not empty, or just increasing the borders. So the identities set contract is
  responsible.
