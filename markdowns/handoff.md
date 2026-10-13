# Handoff

Work decided to be done and not done yet. Facts about what exists are in `current-state.md`;
undecided things are in `questions.md`. The messy version was pushed on 2026-10-13 (`current-state.md`
→ Repository).

## Decided, not done

- Now that the messy version is pushed, every rename: the `OE_` prefix, `OntologyEngineCore` and the package
  `org.halim.ontology` become `acse`; `naming-fix.md` names the project, which keeps the name ACS,
  then that file is deleted; and the parent project ACS takes the modules `acse` and `acses`, with
  `src/` and `pom.xml` moving into the library's module (`decisions.md` → Layers and names).
- The commercial licence's text, drafted by a lawyer before it is sold, and a contributor agreement
  before the first outside contribution.
- `README.md` → The application and The goal were written before the goal was stated (`decisions.md`
  → Goal, 2026-10-08): they present OntoDirectory's filing-based RAG, not the database service with
  requests, a GUI and the agent pipeline. Rewriting them is the author's (the README is handwritten);
  where they differ, `decisions.md` wins. The Claude chat responses pasted while stating the goal are
  kept verbatim, gitignored, in `reference/chats/claude-chat-acs-agent-goal--2026-10-07_1007.md`.
- `acses`, later: `boot` launches a separate process, any other command bridges to it, and
  loading-folder and loading-class methods must exist (`decisions.md` → Layers and names). Creating a
  database copies `acse-persistence.jar` and the base classes from the device config, builds an empty
  core and stores it; loading opens the folder's manager and records the core under
  `--calledBy <name>`, else the folder's name (`decisions.md` → Persistence). The library side exists.
- Later: the urgent code work in `inferred-decisions.md` → Urgent work (unreviewed, so not started),
  the procedure contract (`decisions.md` → Architecture, Vision), and more structure components, the
  cartesian (ternary) and the transitions arity among them (`decisions.md` → Architecture).
- The folder on disk need not be renamed for pushing. If it is renamed, between sessions, Claude
  updates everything that points at its path (listed in Claude's memory).
- The novelty study is running in another session (2026-10-13); its launcher is in Claude's folder
  (see Claude's memory).
- Delete `reference/msgfilter.py` once the author says so; it is gitignored, so it cannot be published
  by accident. The pre-purge bundle was deleted on 2026-10-13.
