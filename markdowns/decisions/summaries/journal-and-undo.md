# Journal and undo — what is recorded, and how is it undone?

Quotes of `citable.md` → Journal and undo, written 2026-10-21. Rules: `CLAUDE.md` → Decisions.

1. “the "check and call checkless functions" pattern” · “private reckless functions of unit
   operations” · “Cross-cutting concerns come from a journal” · “a single writer with readers on a
   snapshot”
2. “For every request about ACS, it must be able to store a comment” · “later may be audited” ·
   “which can only request; steps are the application's responsibility”
3. “an inverse that is among the other actions. Call it "invertible"” · “recording the change is
   enough for calculating the undoing” · “"inverse calculatable"” · “Some actions are not undoable”
   · “allow "break undo history"”
4. “no need of journal overhead” · “Yes, journal adding identities”
5. “undoing may take a different approach” · “we can have request resolver functions” · “[The] first
   option is likely better” · “cannot be inverted by undoing all in reversed order” · “can be undone
   by closing the isTight temporarily”
6. “a record at the beginning and end must be done” · “parameters, itself and [its] inverse” · “More
   details can be added” · “we also need to add an abort option”
7. “registers its op codes and a codec” · “An operation declares its own logging mode; the client
   does not” · “no need, we are recording only relations”
8. “the journal contract alongside the core contract” · “We can make the journal's creation
   independent of the core” · “`indexer.bin` names the journal's class”
9. “I have a small feeling it is appropriate here” · “we can do it later” · “can be a JSON or
   something better” · “every transaction” · “its own section, not the list each”
10. “use bitset or roaring bitset”
11. “we can simply store the difference” · “they can implement the Journal themselves” · “Decided to
    store `new ^ old`” · “do not change in the number of bits” · “not open, simply log the change”
