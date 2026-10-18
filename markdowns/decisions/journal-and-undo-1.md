# Journal and undo (1 of 2) — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

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
- "addParent" under isTight cannot be inverted by undoing all in reversed order; it can be reverted
  by undoing all in the same order; also, it can be undone by closing the isTight temporarily.
- [The old plan's write barrier is declined; it] needs more details, explained here: for journal
  actions with checks and multi steps, a record at the beginning and end must be done, [and it]
  must contain a name for logging and alike; for steps, each must just contain parameters, itself
  and [its] inverse. More details can be added.
- A structure registers its op codes and a codec for its journal records. *(Old plan's wording,
  confirmed by the author.)*
- An operation declares its own logging mode; the client does not. *(Old plan's wording, confirmed
  by the author.)*
- [Recording positions, so undo restores the order of parents:] no need, we are recording only
  relations.
- I plan on making the journal contract alongside the core contract.
- The comment will be given by the client, which is "acses" [the requester], which can only request;
  steps are the application's responsibility. *(The author wrote "asrc"; renamed to "acses" at the
  author's request.)*
- While your [Claude's] decision of throwing [when undo is called] during a request is sound, we also
  need to add an abort option.
- [Adding identities:] Yes, journal adding identities; I forgot to tell because it is assumable.
- [`storeAll`:] I have a small feeling it is appropriate here, but databases often use a "journal
  before" [write-ahead log] type of action; we can do it later.
- "storeAll" can be a JSON or something better, you can choose; it [is] meant to store every
  transaction as a log. Just one rule: each transaction is stored [in] its own section, not the list
  each.
- For the boolean data of the journal we need to use bitset or roaring bitset, since [an] actual boolean
  array costs about 1 byte instead of 1 bit. *(2026-10-12)*
- [Catalog setters:] it is meant to be logged, and instead of storing [the] former change with the new
  one we can simply store the difference. If the client wants to store former and new versions for
  higher clarity, they can implement the Journal themselves, or we can make a separate Journal
  implementation that stores that way. *(2026-10-13)*
- Decided to store `new ^ old` for catalog rows that do not change in the number of bits.
  *(2026-10-15)*
- [The setter shape, and the logging mode for bulk writes:] not open, simply log the change using the
  method you gave [`old ^ new`].
