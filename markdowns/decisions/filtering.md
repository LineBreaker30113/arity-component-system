# Filtering — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

- Right now the filtering service can only provide the logical operators, and filters on data depend
  on the client. This is somewhat intended, since we allowed the data to vary, but some
  filtering modules to simplify the client's experience must be done.
- Cool names for "OR" variants like "IMPLIES", all are redundant. Keep logic minimal right now.
- They [the OR variants] are boilerplate, meant to be handled last.
