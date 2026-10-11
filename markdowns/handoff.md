# Handoff

Work decided to be done and not done yet, and what has to happen before the first publish. Facts
about what exists are in `current-state.md`; undecided things are in `questions.md`.

## Before publishing

- Apply the licence (`decisions.md` → Licence): a `LICENSE` file with the AGPL-3.0 text and the
  Universal FOSS Exception, a `<licenses>` block in `pom.xml`, and a README line saying a commercial
  licence is available. Before the first outside contribution, a contributor agreement.
- Oracle's page says nothing about reusing the exception's text: ask Oracle, or have the lawyer who
  drafts the commercial licence write an equivalent.
- Do every rename first. Renaming packages and classes after the first push churns a public history;
  before it, it costs nothing. Now also: ACS → `acse`, the two modules `acse` and `acses` under one
  parent project, and the move of `src/` and `pom.xml` into the library's module folder.
- Choose the host and the repository name. GitHub, GitLab, Codeberg, or self-hosted Forgejo.
- Re-run the scan that was clean on 2026-09-18: `git ls-files` carries only this project's own work,
  and no tracked file holds a credential, a home path or an email.

## Decided, not done

- The goal is recorded in `decisions.md` → Goal (2026-10-08). `README.md` → The application and The
  goal predate it and are the author's to rewrite (the README is handwritten); `decisions.md` wins
  until then. The Claude chat responses pasted while stating the goal are kept verbatim, gitignored,
  in `reference/chats/claude-chat-acs-agent-goal--2026-10-07_1007.md`.
- Apply `naming-fix.md` once its wording is edited — now for the new names (`acse`, `acses`, "Arity
  Component System Engine"): the two `pom.xml` lines, the two titles, and the README subsection on
  the name — then delete that file.
- `acses` is decided and not started: `boot` launches a separate process, any other command bridges
  to it, and loading-folder and loading-class methods must exist (`decisions.md` → Layers and names).
- Rename the folder on disk from `OntologyEngine`. The author's to do between sessions: renaming it
  under a running session breaks every path in use.
- The urgent code work listed in `inferred-decisions.md` → Urgent work. Still unreviewed, so still
  not started.
- Catalog writes as journaled arity operations, so procedures can change data (`decisions.md` →
  Architecture). Open: setter shape, and the logging mode for bulk writes.
- The procedure contract (`decisions.md` → Architecture, Vision).
- Define more structure components, the cartesian (ternary) and the transitions arity among them
  (`decisions.md` → Architecture).
- The novelty study runs in a separate session the author starts; its launcher is in Claude's folder
  (see Claude's memory).
- Delete the old `structurer/structures/graphs/SetHierarchyDirectedAcyclicIntGraphModule` once the
  author says so; the two `acse_beta` hierarchies replace it.
- Delete `reference/pre-purge-backup.bundle` and `reference/msgfilter.py` once the history rewrite is
  trusted. Both are gitignored, so neither can be published by accident.
