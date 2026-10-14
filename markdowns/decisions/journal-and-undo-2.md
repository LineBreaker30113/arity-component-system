# Journal and undo (2 of 2)

- "storeAll" can be a JSON or something better, you can choose; it [is] meant to store every
  transaction as a log. Just one rule: each transaction is stored [in] its own section, not the list
  each.
- [For the "until" methods refusing the wrong direction:] we can add a checkout method for ease of use.
- [`abortRecordingActivity()`:] approved.
- For the boolean data of the journal we need to use bitset or roaring bitset, since [an] actual boolean
  array costs about 1 byte instead of 1 bit. *(2026-10-12)*
- [Catalog setters:] it is meant to be logged, and instead of storing [the] former change with the new
  one we can simply store the difference. If the client wants to store former and new versions for
  higher clarity, they can implement the Journal themselves, or we can make a separate Journal
  implementation that stores that way. *(2026-10-13)*
- [Since `new - old` does not restore a double exactly:] your [Claude's] "old ^ new" method [over the
  raw bits] solves the current problem with the journal.
- [The setter shape, and the logging mode for bulk writes:] not open, simply log the change using the
  method you gave [`old ^ new`].
- We can make the journal's creation independent of the core: `indexer.bin` names the journal's class
  beside the core's, a load builds the journal from that class through the folder's jar as it builds
  the core, and the core takes that journal instead of building `ACSE_JournalBasic` itself. *(Claude's
  wording, confirmed by the author.)*
