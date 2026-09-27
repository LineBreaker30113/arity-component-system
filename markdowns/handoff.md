# Handoff

Work decided to be done and not done yet, and what has to happen before the first publish. Facts
about what exists are in `current-state.md`; undecided things are in `questions.md`.

## Before publishing

- Decide the licence. Without a `LICENSE` file the default is all rights reserved, which contradicts a
  README inviting reimplementation, and Codeberg will not host without one. The question is in
  `questions.md`.
- Do every rename first. Renaming packages and classes after the first push churns a public history;
  before it, it costs nothing.
- Choose the host and the repository name. GitHub, GitLab, Codeberg, or self-hosted Forgejo.
- Re-run the scan that was clean on 2026-09-18: `git ls-files` carries only this project's own work,
  and no tracked file holds a credential, a home path or an email.

## Decided, not done

- Apply `naming-fix.md` once its wording is edited — the two `pom.xml` lines, the two titles, and the
  README subsection on the name — then delete that file.
- Rename the folder on disk from `OntologyEngine`. The author's to do between sessions: renaming it
  under a running session breaks every path in use.
- The urgent code work listed in `inferred-decisions.md` → Urgent work. Still unreviewed, so still
  not started.
- The author writes a pseudocode-like journal class, and we improve it from there.
- Delete `reference/pre-purge-backup.bundle` and `reference/msgfilter.py` once the history rewrite is
  trusted. Both are gitignored, so neither can be published by accident.
