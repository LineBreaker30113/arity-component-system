# Journal and undo (2 of 2) — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

- We can make the journal's creation independent of the core: `indexer.bin` names the journal's class
  beside the core's, a load builds the journal from that class through the folder's jar as it builds
  the core, and the core takes that journal instead of building `ACSE_JournalBasic` itself. *(Claude's
  wording, confirmed by the author.)*
