# Journal and undo — summary

Claude's summary of `decisions.md` → Journal and undo and `journal-and-undo-1.md`, `-2.md`; written
2026-10-18. A reading aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

## General

- Check-and-call-checkless pattern: functions resolve a client request into private unchecked unit
  operations.
  > “"check and call checkless functions" pattern”
- Cross-cutting concerns come from the journal: undo/redo replay it, logging reads it; one lock at
  the transaction boundary; a single writer with snapshot readers.
  > “Cross-cutting concerns come from a journal”
- Every request about ACS must be able to store a comment, like a commit message for every action,
  since actions come from resources and later may be audited.
  > “like the git commit messages, but for every action”

## Details

- Actions whose inverse is another action are "invertible".
  > “Call it "invertible"”
- Actions whose recorded change is enough to compute the undo are "inverse calculatable".
  > “Call it "inverse calculatable"”
- Some actions are not undoable and snapshots are expensive, so we need to allow "break undo
  history".
  > “allow "break undo history"”
- Persistence is just writing data: no need of journal overhead.
  > “no need of journal overhead”
- Undo may need a different approach when a client request is a cluster of operations (undoing
  addParent's remove-parent calls first is auto rejected); we can have request resolvers record the
  start in the journal, then call atomic operations, likely better than operations telling their
  inverse, so client jobs are separated from inner operations.
  > “auto rejected” · “[The] first option is likely better”
- addParent under isTight cannot be undone in reversed order, but can be by undoing all in the same
  order, or by closing isTight temporarily.
  > “cannot be inverted by undoing all in reversed order”
- No write barrier: checked multi-step actions must record a start and an end, with a name for
  logging; each step must just contain its parameters, itself and its inverse; more details can be
  added.
  > “a record at the beginning and end must be done”
- A structure registers its op codes and a codec for its journal records.
  > “registers its op codes and a codec”
- An operation declares its own logging mode; the client does not.
  > “An operation declares its own logging mode”
- No need to record positions to restore parent order: only relations are recorded.
  > “we are recording only relations”
- The journal contract is planned alongside the core contract.
  > “alongside the core contract”
- The client (acses as requester) gives the comment and can only request; steps are the
  application's responsibility.
  > “which can only request”
- Throwing on undo during a request is sound, but an abort option is also needed.
  > “we also need to add an abort option”
- Adding identities is journalled.
  > “Yes, journal adding identities”
- storeAll seems appropriate, but a write-ahead "journal before" approach can come later.
  > “"journal before" [write-ahead log]”
- storeAll can be JSON or better (Claude's choice); every transaction is logged in its own section,
  not one list.
  > “each transaction is stored [in] its own section”
- The journal's boolean data needs a bitset or roaring bitset: a boolean array costs a byte per
  value, not a bit (2026-10-12).
  > “use bitset or roaring bitset”
- Catalog setters are logged as the difference; a client wanting old and new values can implement
  its own Journal, or we can make a separate implementation (2026-10-13).
  > “we can simply store the difference”
- Store `new ^ old` for catalog rows whose bit count does not change (2026-10-15).
  > “Decided to store `new ^ old`”
- Setter shape and bulk-write logging are not open: log the change with `old ^ new`.
  > “simply log the change using the method you gave”
- The journal's creation can be independent of the core: indexer.bin names its class, a load builds
  it from the folder's jar, and the core takes it instead of building ACSE_JournalBasic.
  > “the journal's creation independent of the core”
