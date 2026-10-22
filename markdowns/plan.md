# Plan — the three-file method: moving the decisions into the folders

Written 2026-10-22 by Claude, for one session of the understanding phase, whose only goal is creating the
folders' markdowns and moving the entries into them; it stops for the author's review and commit. **The
author's words** below are the source; lines marked *reading* are Claude's readings of them. Until the
method is formalized after this session ("After the migration is done by the new session, we will
formalize the method."), this plan wins over `CLAUDE.md` → Decisions about the method ("this plan is
newer, but we need to make a migration first"). Round 4's plan is in git at `141008e`.

## The author's words

The method, 2026-10-22, with the grammar fixed:

> The RAPTOR RAG is good for reasoning and I want to use it as your context remembrance method for
> developing this project. In order to do so, the plan is to make the root folders' markdowns keep the
> summaries and general goals, and comparisons about sub folders to explain them. The leaf folders'
> markdown files give the finest details as decisions. However, this will likely cause many markdown
> files; for simplicity we will use a three file method: agents.md, agents-citable.md,
> agents-unreviewed.md. Where: Agents hold the summary of the decisions made, citable is the agreed on
> facts, and the agent-unreviewed [agents-unreviewed.md] is the things I need to check.
>
> Rules are:
> - The agents will read the agent.md [agents.md] only, and if confused will check the citable first.
> - If they conflict about the details, (relatively) leaf wins; about the goal and summary, the
>   (relatively) root wins.
> - If I make a decision, it will be in the unreviewed section; I will review and fix them.
> - If I make a decision, it [the agent] will store it in two parts: Verbatim, meaning (a better
>   grammar and simpler explanation done by removing filler (and alike) words).
> - If I state I reviewed decisions, the agent will look at unreviewed files and will move them to
>   citable.
> - Newer entries are lower, and if they conflict they will win against former entries.
>
> So in development I will make decisions which are moved to the markdowns; I will review them, and the
> agent will move them and build the agents.md using the former agents.md and citables. Remember to
> clear the agents.unreviewed.md [agents-unreviewed.md] file.

The answers to Claude's eight questions, 2026-10-22; the quoted option texts are Claude's, picked by the
author:

- [Which folders get the three files:] "(Q1: a)" — "The repo root, each module (`acs/`, `acse/`,
  `acses/`, `acs-view/`, `ontodirectory/`), and a package folder once a decision about it is made, such
  as `acse/…/arity/catalog/file/`. Folders that only hold one folder get none. A decision about several
  folders goes to the deepest folder holding all of them, and the other folders' agents.md can point at
  it."
- [The names, and how an agent comes to read agents.md:] "(Q2: a, except CLAUDE.md)" — "`AGENTS.md`,
  `agents-citable.md`, `agents-unreviewed.md`, plus that one setting [`claude-md-and-agents-md`]."
- [Today's files:] "(Q3: primarily (a), your ideas are cool, but since [the] root holds way too many
  markdowns, specifically the root lists those on the markdowns)" — (a): "`citable.md` becomes the root
  agents-citable.md as it is, beside CLAUDE.md. A later decision about a detail goes to its folder and
  wins there (rule 2). The 35 Unconfirmed entries go to the agents-unreviewed.md of the folder each one
  concerns. `gui-decisions.md` becomes `acs-view/agents-citable.md`, and the `questions.md` line goes to
  the root's unreviewed file. `summaries/` and `markdowns/decisions/` are replaced by the agents.md
  files."
- [Verbatim and Meaning:] "(Q4: Verbatim has the grammar fixes, and the meaning is your inference; it
  can be shorter and that's the only goal; if [it] cannot get shorter, no need for inferences. (a))" —
  (a) [today's entries, which have one part]: "Keep them as they are, counting as both parts."
- [Which wins across levels, and the overridden entry:] "(Q5: (a). The agent must move the details to
  the level they are in. The level of a detail is defined by what it describes. If it describes [a] few
  things' relation, it belongs to the parent of the relations. If it describes a thing, it [is] meant to
  be on the same level; example: a markdown file [that] describes the acs-viewer is on the parent of the
  acs-viewer folder.)" — (a): "Level across files, date within a file. A new decision is written in the
  deepest folder it concerns, so a newer detail lands in the leaf anyway. The Meaning names any entry the
  new one overrides, so your review sees it." *Reading*: the (a) also picks "Kept and marked "[replaced
  by: …]", since "we do not cut my words"."
- [What agents.md contains:] "(Q6: (a))" — "Sentences built from the reviewed Meanings. Each line cites
  its citable entries by number; numbers are never reused. Each file is about a quarter of its citable,
  and the root's also compares the subfolders. They are rebuilt leaves first and root last, each from
  its former agents.md, its citable, and its subfolders' agents.md."
- [Several sessions at once:] "(Q7: There will be only one session on a folder.)"
- [Where the method is written:] "(Q8: this plan is newer, but we need to make a migration first.)"
- "Lastly there are two phases: one is development (ordinary), and the other is understanding (moving
  the data across markdowns). After you make the plan, we will start a new session with the only goal
  of creating markdowns and moving those related entries."

The answers to Claude's two further questions, and two remarks, 2026-10-22:

- [On Claude's reading that today's entries are placed by level now:] "Level is the folder."
- [The move on "reviewed":] "Since the "unreviewed" markdowns will have a certain structure, I feel like
  we can use a python script to move them automatically."
- [Q-A, where the root's three files go and which is its summary:] "Q-A: (a)" — "In `markdowns/`:
  `AGENTS.md`, `agents-citable.md` and `agents-unreviewed.md`. `CLAUDE.md` gets one line,
  `@markdowns/AGENTS.md`, so the root summary loads at launch; otherwise it is unchanged until the method
  is formalized. The repo root keeps only `CLAUDE.md` and `README.md`."
- [Q-B, where a module's files sit:] "Q-B: (a)" — "In the module folder, which stands for the folders
  down to its package root: `acse/AGENTS.md`."
- [Which entries the review move takes:] "The script must move things above the `# Unreviewed` line."
- [Before this session:] "Commit all, then I will run the script [the launcher]."

## The design

### The files

- A folder that holds entries has `AGENTS.md`, the summary; `agents-citable.md`, the entries the author
  agreed on; and `agents-unreviewed.md`, the entries the author has not checked. A folder, a module
  included, gets them when its first entry arrives (*reading* of Q1: a, so no file stands empty).
- The root's three files are in `markdowns/` (Q-A: (a)). `CLAUDE.md` loads the root summary with the
  line `@markdowns/AGENTS.md`, written there without backticks, since the docs say an import inside a
  code span is not read.
- A module folder stands for the folders between it and its package root (Q-B: (a)): `acse/`'s files
  describe what is in `org.halim.acse`; `ontodirectory/`'s, what is in `org/halim/`, which holds three
  package roots (`legacy`, `nStructure`, `setSea`).
- Loading, checked against the docs on 2026-10-22 (https://code.claude.com/docs/en/memory; installed
  Claude Code 2.1.308, AGENTS.md support since 2.1.277):
  - By default this repo's `CLAUDE.md` stops every `AGENTS.md` from loading: "By default, Claude reads
    `AGENTS.md` only when you have no `CLAUDE.md` in your working directory or above it."
  - With Project instructions set to `claude-md-and-agents-md`, Claude reads "Your `CLAUDE.md` and
    `AGENTS.md` files together, each directory's `CLAUDE.md` files first and its `AGENTS.md` after them".
  - A subfolder's `AGENTS.md` loads "when Claude opens a file there with the Read tool". The docs say
    this for the default mode; step 8 checks it with the setting.
  - An `AGENTS.md` that `CLAUDE.md` imports "isn't read twice".
  - `agents-citable.md` and `agents-unreviewed.md` load nowhere by themselves, as rule 1 wants.

### Where an entry goes

- The author's rule (Q5), with "Level is the folder.": an entry goes to the folder its level names. It
  applies now to today's entries too (*reading*): Q5's "The agent must move the details to the level
  they are in" overrides Q3 (a)'s "as it is", and the rest of Q3 (a) holds.
- A folder's files describe the things directly in it, folders and files, and the relations among them
  (*reading*). A relation goes to the deepest folder holding all its things (Q1: a). An entry is never
  split: one about two things goes whole to the folder holding both. "The root" is `markdowns/`'s files.
- Examples, as Claude would place them; step 4's table decides:
  - "The ACS is a system, as it names; ACSE is its implementation, and ACSES uses it for [the] database
    service." — the relation of `acs`, `acse` and `acses` → the root.
  - "ACS-view is the acsess GUI, in a separate Maven module, `acs-view`: …" (unconfirmed) — `acs-view`
    as a thing → the root, as the author's example says.
  - "ACSES splits into two packages: `acsed`, the daemon, … and `acsess`, the command line tool, …"
    (unconfirmed) — the relation of `acsed` and `acsess`, both in `org.halim.acses` → `acses/`.
  - "We can make the journal's creation independent of the core: `indexer.bin` names the journal's class
    beside the core's, …" — the journal, the core and the persistence manager, all in `org.halim.acse`
    → `acse/`.
  - Goal, Vision, Licence, Process, Words and Target use case describe the project → the root.

### Entries

- In a citable, an entry is numbered when it arrives, `- **N.** `, from 1, never reused or renumbered.
  Entries keep the section headings they had (`## Goal`, `## Persistence`, …). A section's notes are
  copied into every file that gets its entries. A moved entry keeps its words, [brackets] and italic
  notes. A new entry goes at the end of its section, so within a section the lower is the newer
  (rule 6).
- In an unreviewed file, a new entry has two parts: **Verbatim**, the author's words with the grammar
  fixed (`CLAUDE.md` → The quote test), dated; **Meaning**, Claude's inference, only when it is shorter.
  A `Section:` line names its citable section, and a `Replaces:` line names any entry it overrides: Q5
  (a) has the Meaning name it, and a line of its own lets the script mark it. Unreviewed entries are not
  numbered.
- A folder is named by its path from the repo root, with a module's chain down to its package root left
  out: `acse/arity/catalog/file` names `acse/src/main/java/org/halim/acse/arity/catalog/file/`, and
  `root` names `markdowns/`.
- Today's entries keep their one part (Q4: (a)). Today's unconfirmed entries move as they are
  (*reading*): their claim is already Claude's inference and their quotes the Verbatim. Their pointers
  to `citable.md` → a section are rewritten to the cited entry's new place, which changes Claude's words
  only, and each rewrite is listed.
- When an entry reaches a citable and overrides an older one, the older is kept and marked
  `[replaced by: <folder> N]` (Q5: (a)).
- Conflicts: within a file, the lower wins; across files, the leaf wins on details and the root on
  goals and the summary (rule 2).

### The review move

- The structure the script relies on (the author: "Since the "unreviewed" markdowns will have a certain
  structure, I feel like we can use a python script to move them automatically."). An
  `agents-unreviewed.md` holds, in order:
  - one header line;
  - the entries the author has reviewed, which the author moves above the `# Unreviewed` line;
  - the `# Unreviewed` line;
  - the entries not yet reviewed, newest lowest, where agents append.
- Every entry is one top-level bullet that carries its section on its own `Section: <name>` line, so it
  can move without a heading. A new entry is `- Verbatim: … *(<date>)*`, then an optional `Meaning: …`
  line, a `Section: …` line and an optional `Replaces: <folder> N` line. A legacy entry is its bullet as
  it is, plus its `Section:` line.
- `~/mein/claude/scripts/agents_review_move.py REPO [FOLDER …] [--apply]`, written in step 6:
  - Without `--apply`, it does a dry run: per folder, each entry with its section and the number it would
    get, saved dated in `~/mein/claude/results/`.
  - It takes only the entries above the `# Unreviewed` line ("The script must move things above the
    `# Unreviewed` line."); the line and everything below it stay.
  - With `--apply`, it does six things:
    - appends each entry to the end of its section in the folder's `agents-citable.md`, making the
      section at the end of the file if missing;
    - numbers it `- **N.** …` with the next unused number;
    - marks each `Replaces:` target `[replaced by: <folder> N]`;
    - removes the moved entries from the unreviewed file, which is how "clear the agents-unreviewed.md
      file" is read (*reading*);
    - saves a dated record to `~/mein/claude/results/`;
    - prints the folders whose `AGENTS.md` needs a rebuild, leaves first, root last.
  - It refuses to run when any line falls outside the structure, so nothing is moved or cleared
    silently.
- Rebuilding `AGENTS.md` means writing a summary, so it stays the agent's work, done on the folders the
  script prints.

### AGENTS.md

- Sentences built from the entries: from their Meanings where they have one, from their words
  otherwise. Each line cites the entries it rests on: `(12, 15)` in its folder's citable, `(acse 4)` or
  `(root 7)` in another folder's.
- Every entry of the folder's citable is cited. A line keeps the hedges, negations, scope and time words
  of the entries it cites, or leaves that claim out. The 2026-10-21 checking run found five quotes that
  each dropped one of these.
- About a quarter of its citable's words. The root's also compares its subfolders and lists the files in
  `markdowns/` (Q3).
- Built from citables only, never from unreviewed entries: leaves first, root last, each from its
  citable and its subfolders' `AGENTS.md`. The root's former summary is `markdowns/decisions/summaries/`
  (quotes only, at 39% of its source).

### What stays

- `current-state.md`, `questions.md`, `terms.md`, `markdown-refining.md`, `novelty-study.md`,
  `naming-fix.md` and `README.md` keep their content; only their lines naming moved files change
  (step 10). `questions.md`'s "Author-decision" line moves, since it is a decision.
- `CLAUDE.md` changes only by the import line (Q-A: (a)) and step 10's pointers. The method itself is
  formalized after this session.

## Facts, 2026-10-22

- `markdowns/decisions/citable.md`: 746 lines, 167 entries above Unconfirmed and 35 under.
- `acs-view/gui-decisions.md`: 5 entries, "all confirmed".
- The "Author-decision" line in `questions.md`.
- Every source is committed with this plan. `javac.20261021_081457.args`, a compiler argument file at
  the repo root holding home paths, stays untracked.
- `markdowns/decisions/summaries/`: 14 topic files and `overview.md`.
- The five modules hold 104 folders, 61 of them holding Java files.
- Two entries the method touches:
  - `gui-decisions.md`'s "[Where these decisions live:] I want you to record the GUI design related
    decisions of acs-viewer in its own maven module folder as "gui-decisions.md" and all confirmed." is
    replaced by the three-file method: it is kept and marked.
  - Process's "I want the inferred decisions and [the] rest of the decisions to stay under the same path
    [`markdowns/decisions/`]." still holds per folder, since each folder's unreviewed and citable files
    sit together; only its bracket is outdated.

## Steps

1. **Checkpoint.** Run `git status` and say what changed in the sources since this plan's commit. Ask
   anything still unclear as `markdown-refining.md` → Questions to the author says. This session
   touches every folder's markdowns, so no other session records decisions while it runs (Q7). Wait.
2. **The setting.** Ask the author either to set Project instructions to `claude-md-and-agents-md` in
   `/config`, or to approve adding `"pluginConfigs": {"cc-plugin-agents-md@builtin": {"options":
   {"instructionFiles": "claude-md-and-agents-md"}}}` to `~/.claude/settings.json`. It applies from the
   next message.
3. **Inventory**, by script: every entry with its source, section and first words; counts per source.
   The result goes, dated, to `~/mein/claude/results/`.
4. **Placement table**: for each entry, the thing or relation it describes and its folder, grouped by
   folder. Each whole section that stays at the root is one line, so the review reads only what moves.
   Entries whose folder is unclear are listed separately, with numbered options. Save it dated, stop
   for the author's review, and take the fixed table.
5. **Move**, by script:
   - Create the files in each folder of the table, each with one header line.
   - Each entry from `citable.md` above Unconfirmed or from `gui-decisions.md` goes, numbered, to its
     folder's `agents-citable.md`, under its section. The unconfirmed entries and `questions.md`'s line
     go below the `# Unreviewed` line of its `agents-unreviewed.md`, each with its `Section:` line.
   - A check by script: every source entry is found word for word exactly once, in the folder the table
     says. The only added text is the numbers, headings, header lines, copied section notes and the
     listed pointer rewrites.
6. **The review-move script**: write `agents_review_move.py` and test it on a copy in the scratchpad.
   The test has made-up entries above and below the `# Unreviewed` line in two folders, one entry with
   a `Replaces:` line, and one malformed file the script must refuse. Run it, dry, on the migrated
   unreviewed files. The results go, dated, to `~/mein/claude/results/`.
7. **AGENTS.md**, leaves first, root last; then add the import line to `CLAUDE.md`.
   - A check by script, adapted from `summary_check.py`: citations resolve, every entry is cited, guard
     words are kept, and each file's words are counted against its citable.
   - Then one checking run on a different model, as on 2026-10-21: per entry ok, lost or changed. Fix
     every changed entry and report the lost share.
8. **Loading**: open a file with the Read tool in a folder that now has an `AGENTS.md`, and say whether
   the `AGENTS.md` arrived in context. If not, stop and report. The root summary's import is checked at
   the start of the next session.
9. **Remove what moved**, once steps 5 to 7 pass: `markdowns/decisions/`, `acs-view/gui-decisions.md`,
   and `questions.md`'s moved line.
10. **Pointers**: list the lines that name the moved files, propose replacements, and edit on approval.
    They are in `CLAUDE.md`, `current-state.md`, `questions.md`, `README.md`, `markdown-refining.md`,
    `novelty-study.md`, Claude's memory, and the launchers in `~/mein/claude/scripts/`.
11. **Stop**: report `git status`; entries per source and per folder, before and after; lines and words
    per file; and the checks' results. No commit until the author says.

- If the author moves entries above a `# Unreviewed` line during the session and says they are
  reviewed: run `agents_review_move.py --apply`, then rebuild the `AGENTS.md` files it prints, leaves
  first.
- Before each step that edits files, say in one line what it will change.
- Never cut or change the author's words. A placement or move that would need it is asked, with
  numbered options.
- Scripts go to `~/mein/claude/scripts/`, results dated to `~/mein/claude/results/`. Report at the end in
  a few lines.
