# Identities and removal — summary

Claude's summary of `decisions.md` → Identities and removal and `identities-and-removal-1.md`,
`-2.md`; written 2026-10-18. A reading aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

"Vacuum" is now "compacten" (the author's rename, 2026-10-15). The older entries still say
"identity" and "index"; which of iui or imv each means is not settled yet.

## General

- The core is the only caller of removal requests from outside; the arity removes and delegates the
  message through the core (2026-10-15).
  > “Core is the only caller of removal requests (from outside)”
- Components will depend less on the core; arities remove their own elements (2026-10-15).
  > “arities remove their elements on their own”
- Structures are responsible for representing data optimally for memory and time; imv values can be
  removed or structures around them change (2026-10-15).
  > “optimally for memory and time”
- Journalling removed the need to separate forgetting from removal: data is forgotten, but
  journalled, the moment an element is removed (2026-10-15).
  > “forgotten (but journalled) the moment an element is removed”

## Details

- The compacten idea: an identity's index is converted to the identity at ArityModule's
  identitiesSet.
  > “the index of an identity is converted to identity”
- Given the identity number rather than the index, the identities set auto-skips removed ones; the
  arity component is meant to be used that way.
  > “it auto skips the removed ones”
- Execution reaching identities (through multi-arity structures, maybe a few other cases not
  remembered right now) checks they exist, when necessary.
  > “they check if they exist, when necessary”
- Structures are better off not shifting data, so they point at the index rather than the identity;
  we need to handle that.
  > “they point at the index rather than the identity”
- The index is the record's memory address, which does not shift on removal; that would not cause
  problems: functions transform the identity number first.
  > “transform the identity number before using it”
- The author believes structures do not need to check if an identity exists.
  > “I have a belief the structures do not need to check”
- To remove an identity on another arity, a structure calls ArityComponent's remove: the recursive
  step from the original arity through related structures.
  > “that is the recursive step”
- The arity removes the identity and calls the core forget to delegate it (2026-10-15).
  > “calls the core forget for delegating it”
- Forget needs to be undoable.
  > “Forget needs to be undoable”
- "swap" or "captureNotForget" will not be needed, given what an arity represents; more later.
  > “"swap" or "captureNotForget"”
- Storing a structure: recording the relation is enough; the core can store temporarily, then
  compacten, then finally, but that is not the only sequence: allow direct storing.
  > “recording the relation is enough” · “allow [the] option of direct storing”
- Calculated arities must depend on a structure; depending directly on another arity can raise
  errors.
  > “must be dependent on a structure”
- Removal and binary search over the hole list will likely never get that slow.
  > “likely will never get that slow”
- Structures are not responsible for the element count; it is the structure's add-on.
  > “not responsible for the element count”
- The author thinks compacten will be a rare call, on storing to disk and reloading.
  > “the compacten will be a rare call”
- A forget call must remove the associated data (like an indexer's connections) without shifting all
  the identities; the core handles delegations over it.
  > “doesn't shift all the identities” · “Delegations over that call will be handled by the core.”
- No structure compactens a dead identity it reaches on its own.
  > “No structure compactens a dead identity”
- Bulk catalog reads intersect the range with the occupied set once, instead of checking each
  element.
  > “intersect the range with the occupied set once”
- The client can skip identity validation when it likely knows them.
  > “skip the validation of identities”
- The automatic existence check validates a reference exactly when it leaves its structure
  (returned, or used to address another arity), if the client requests checking.
  > “exactly when it leaves the structure holding it”
- The author thinks pending removals are not needed; identity sets handle it.
  > “we do not need to make pending removals”
- Capturing back: the identity set must allow capturing the lowest positive excluded value, the
  excluded list's first element if it is not empty, else extending the borders; the identities set
  contract is responsible.
  > “capturing the lowest positive excluded value” ·
  > “So the identities set contract is responsible.”
- No "truncate" is needed right now; later it can be made. Compacten (may be renamed "compress") can
  swap the last entries with the dead ones and truncate them.
  > “We do not need to make a "truncate" right now” · “swap the last few entries with the dead ones”
- Undoing a remove after a compacten is out of scope right now; compacten is the planned undo
  horizon.
  > “planned to be the horizon of undo”
