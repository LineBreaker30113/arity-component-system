# Identities and removal (1 of 2)

- I plan on using [the vacuum idea] where the index of an identity is converted to identity at the
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
- [Claude's wording, approved:] Make the core the only caller of removals.
- Structures, if they need to remove an identity on another arity, need to call ArityComponent's
  remove and [the] forget of [the] core on that identity; that is the recursive step: 0 original
  arity -> 1 related structures -> 2 start from (0) on each identity needed to [be] removed.
- Forget needs to be undoable.
- We will not need a function like "swap" or "captureNotForget" because of what an arity represents;
  more on that later.
- [Storing a structure:] recording the relation is enough. Also at the core level storing can be done
  by temporarily storing, then vacuum, then final storage. [That] is not bound to be the only
  [sequence]; allow [the] option of direct storing. Also each time [storing] needs to create a new
  file, so there needs [to be] a pointer.
- [Calculated arities:] the calculated arities must be dependent on a structure; this is better
  since being directly dependent [on] another Arity can actually raise errors.
- [Claude's `long captureIdentity(int arity, String comment)` on the core:] go with it.
- The removal and binary search [over the hole list] likely will never get that slow.
- We should allow a judgment call method alongside auto method (does what you say) for the client.
- Structures are not responsible for the element count; it is just the structure add-on.
- Consider there is an indexer (2-arity structure) that reached a dead
  identity, it should not vacuum it alone. That would be harmful.
- I think the vacuum will be a rare call, on storing to disk and reloading from it.
- We must make a "forget" call that removes the associated data (like the connections on the indexer)
  but doesn't shift all the identities. Delegations over that call will be handled by the core.
- Importantly, if a removed identity gets captured back, a forget call must run on it prior to capture.
- The forget is necessary at the capture back because the data of the former identity is still there.
- Make "Vacuum is the undo horizon." is the currently used solution.
- No structure vacuums a dead identity it reaches on its own.
- Bulk catalog reads intersect the range with the occupied set once, then read, instead of checking
  each element.
- We need to allow client to skip the validation of identities, when they likely know it.
- The automatic existence check validates a reference exactly when it leaves the structure holding
  it: returned to a caller, or used to address another arity. If checking is requested by client.
- I think we do not need to make pending removals; the identity sets handle it.
