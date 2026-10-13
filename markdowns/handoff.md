# Handoff

Work decided to be done and not done yet, and what has to happen before the first publish. Facts
about what exists are in `current-state.md`; undecided things are in `questions.md`.

## Before publishing

The first push is the messy version as it is, to keep its identity (`decisions.md` → Layers and
names). It waits for the author's README edits; Claude then commits them, re-runs the scan, and
pushes `main`.

- The licence is set: `LICENSE` holds the AGPL-3.0 text alone, so hosts label it; `NOTICE` states
  version 3 only, the Universal FOSS Exception by Oracle's link, and the commercial licence; `pom.xml`
  has a `<licenses>` block. Left: a README line saying a commercial licence is available (the
  author's), and a contributor agreement before the first outside contribution.
- The scan was clean on 2026-10-13 (`current-state.md` → Repository): `git ls-files` carries only this
  project's own work, and no tracked file holds a credential, a home path or an email. Claude re-runs
  it before the push if anything is committed after it.
- The author decides whether `.mcp.json` and `src/main/java/Main.java` stay tracked for the push.

## Decided, not done

- After the first push, every rename: the `OE_` prefix, `OntologyEngineCore` and the package
  `org.halim.ontology` become `acse`; `naming-fix.md` names the project, which keeps the name ACS,
  then that file is deleted; and the parent project ACS takes the modules `acse` and `acses`, with
  `src/` and `pom.xml` moving into the library's module (`decisions.md` → Layers and names).
- The commercial licence's text, drafted by a lawyer before it is sold.
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
