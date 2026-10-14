# Journal and undo (1 of 2)

- Some actions have an inverse that is among the other actions. Call it "invertible".
- Some actions recording the change is enough for calculating the undoing.
  Call it "inverse calculatable".
- Some actions are not undoable, snapshot taking is expensive se we need to allow "break undo history".
- "Persistence", saving to files, is just writing some data; no need of journal overhead.
- Sometimes a cluster of operations is [the] steps of a client request, and undoing may take a
  different approach. The example was: adding a parent may cause many remove parent calls, but
  requesting undo on those prior to undoing the add parent is auto rejected. We have two options:
  we can have request resolver functions that call the atomic operations after recording the
  beginning of the operations to the journal, or making them tell the inverse. [The] first option is
  likely better, so we are separating the client side of jobs from inside operations.
- I recently decided the "check and call checkless functions" pattern. Simply, there will be
  functions that resolve [a] client request into private reckless functions of unit operations.
- "addParent" under isTight cannot be inverted by undoing all in reversed order; it can be reverted
  by undoing all in the same order; also, it can be undone by closing the isTight temporarily.
- [Redo:] simple, re-execute all.
- [The DAG's free list, `freeSlots` and `multiData` length:] the slot allocation will likely be
  deterministic, and it feels like it won't matter.
- They [journal primitives] are meant to be the action and its parameters with an inverse.
- [The old plan's write barrier is declined; it] needs more details, explained here: for journal
  actions with checks and multi steps, a record at the beginning and end must be done, [and it]
  must contain a name for logging and alike; for steps, each must just contain parameters, itself
  and [its] inverse. More details can be added.
- A structure registers its op codes and a codec for its journal records. *(Old plan's wording,
  confirmed by the author.)*
- An operation declares its own logging mode; the client does not. *(Old plan's wording, confirmed
  by the author.)*
- Cross-cutting concerns come from a journal: undo/redo replay it, logging reads it; one lock at the
  transaction boundary; multithreading as a single writer with readers on a snapshot. *(Old plan's
  wording, confirmed by the author.)*
- [Legacy's `runAtomically`, one lock and one undo entry per client request:] now we will handle
  this with [the] journal; this is important.
- [Recording positions, so undo restores the order of parents:] no need, we are recording only
  relations.
- [Over proxies:] yes, journal wins.
- I plan on making the journal contract alongside the core contract.
- For every request about ACS, it must be able to store a comment, like the git commit messages, but
  for every action, because every action is made from a resource and later may be audited, which
  will require context.
- The comment will be given by the client, which is "asrc" [the requester], which can only request;
  steps are the application's responsibility.
- While your [Claude's] decision of throwing [when undo is called] during a request is sound, we also
  need to add an abort option.
- [Adding identities:] Yes, journal adding identities; I forgot to tell because it is assumable.
- [`storeAll`:] I have a small feeling it is appropriate here, but databases often use a "journal
  before" [write-ahead log] type of action; we can do it later.
