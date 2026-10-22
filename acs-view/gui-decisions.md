# ACS-view: GUI decisions

The author's decisions on ACS-view's GUI, in the author's words, all confirmed. Words in [brackets] say
what the author's words refer to. What ACS-view is, what kind of tool it is, and its module, are
project decisions, in the Citable (`markdowns/decisions/citable.md`).

- [How ACS-view sends its requests to acsed, picked among Claude's options:] Run acsess: runs the
  acsess program, like a terminal, using beginSession so one refresh is one program run and one
  connection. *(2026-10-22)*
- [Whether the first version changes a database or only shows it, picked among Claude's options:]
  Show and edit. *(2026-10-22)*
- [The Java package, picked among Claude's options:] org.halim.acsview. *(2026-10-22)*
- I believe the GUI must have views like "core", "journal", "classes", "arities", "structures",
  "procedures", "content" (content pages may be many and can focus on different things).
  *(2026-10-22)*
- [Where these decisions live:] I want you to record the GUI design related decisions of acs-viewer in
  its own maven module folder as "gui-decisions.md" and all confirmed. *(2026-10-22)*
