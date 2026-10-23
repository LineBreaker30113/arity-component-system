# Plan — acsess-gui: the rename, the views and the content panels

Written 2026-10-23 by Claude, for one execution session, which stops for the author's review and commit.
**The design** is the author's decisions, Claude's readings of them (marked *reading*), and Claude's
options the author picked. **The author's words** are the source. A decision in a folder's files wins any
conflict with this plan: stop and ask. Round 4's plan is in git at `141008e`, the migration's at `553ecc3`.

## The design

### The rename, first

- The module `acs-view` becomes `acsess-gui`: its folder (by `git mv`), its `artifactId`, its `<name>`
  `ACSESS GUI`, the package `org.halim.acsview` → `org.halim.acsessgui`, `ACSViewMain` →
  `ACSESS_GUI_Main`, `ACSViewService` → `ACSESS_GUI_Service`, and the comment every change carries,
  `acs-view` → `acsess-gui`. The other class names stay (*reading*: Q1 (a) names only these two).
- The root pom lists `acsess-gui` among its modules, so `mvn compile` builds it with the rest; it still
  depends on nothing in `acs`, `acse` or `acses`.
- `.idea/compiler.xml` and `.idea/encodings.xml` follow the folder's name.
- The folder's three files move with it. Claude's words follow the name (the headers, `CLAUDE.md`'s
  lines, `markdowns/CLAUDE.md` lines 36 and 83); the author's words stay as written, "ACS-view" and
  "acs-viewer" included, under the quote test. `markdowns/new-architecture.md` is not touched.

### The views

- The views, from `acs-view` 2: core, journal, classes, arities, structures, procedures, content.
  Q3 (a): each view acsess can serve today is built; the structures and procedures views exist and say
  they wait for their requests, since acsess has none (a structure's relations, a procedure's anything).
- Today's views: the navigator (the components as a tree), the data grid (an arity as a table, edited in
  place), the journal (standing, undo, redo, store) and the console (acsess lines run as one session).
  What each of the seven shows, and which of today's views it becomes or keeps, is the checkpoint's
  table: the author decides it.

### The content panels

- A content panel has a selection at its top: `arity`, `structure`, `procedure`, `arity-row`,
  `arity-identity`. What each shows:
  - `arity`: its row types and its identity count;
  - `arity-row`, `arity-identity`, `structure`, `procedure`: the checkpoint asks, with Claude's
    suggestions (a row's values over the identities, a page at a time; one identity's value in every row
    of its arity; a structure's class, description and spanned arities; a procedure's "waits for its
    requests").
- A panel takes the focused instance of its type. *Reading* of "clicking after focus will trigger it for
  alignment with the drag-and-drop ability": choosing an element (in the navigator, or another panel)
  focuses it; a click on a panel then gives it the focused element, if of its type; the panel does not
  follow the focus by itself. The checkpoint confirms this.
- Or an element, focused or not, is dragged and dropped onto a panel.
- Any number of content panels: opened and closed like the other views, each holding its own type and
  element.

### What is read is kept

- "keeps the queried data in memory" (the root's unreviewed entry, which the author is about to
  confirm): what a view read is kept, so another view or panel showing the same thing reads it without a
  program run; after a change, the kept data is dropped and read again, as every view reads again today.
  Built only once the entry is in the root's citable.

## The author's words

The author, 2026-10-23:

- "I want you to plan a session that will complete the acs-view, but prior to that rename it to
  acsess-gui, since it is what it is."
- On Claude's options: "(Q1: a)" — package `org.halim.acsessgui`, `ACSESS_GUI_Main` and
  `ACSESS_GUI_Service`, matching `ACSED_`/`ACSESS`; "(Q2: a)" — it joins the root pom's modules;
  "(Q3: a)" — every view acsess can serve today: core, journal, classes, arities, content; the structures
  and procedures views exist but say they wait for requests; "(Q6: a)" — this file, and a launcher.
- "(Q4: The content page will have a selection at the top for "arity" [lists the row types and identity
  count], "structure", "procedure", "arity-row", "arity-identity". The content will take the focused [but
  clicking after focus will trigger it for alignment with the drag-and-drop ability] instance of it's
  type. Or an unfocused element can be dragged and dropped into it. There may be any number of content
  panels. The session can ask me questions.)"
- On the unreviewed "keeps the queried data in memory": "(Q5: Do not overthink on it, I am about to
  confirm.)"

## Facts, checked 2026-10-23

- `acs-view`: 25 classes, 1,976 lines, committed at `f979710`; built on its own, not in the root pom's
  `<modules>`. Views: `WelcomePage`, `NavigatorView`, `DataGridView`, `JournalView`, `ConsoleView`, each a
  `WorkSpaceViewPanel` placed by `WorkspacePanel` and toggled by `WorkspaceSection`.
- The shown database and its chosen arity are `WorkspaceController`'s, fanned out by
  `triggerDatabaseRefresh` and `broadcastAritySelection(int)`; no other element can be chosen yet.
- Every request runs on `ApplicationController`'s one request thread, as an acsess program run.
- acsess's requests: 7 of the core, 12 of an arity, 9 of the journal, and acsed's own (7 at subject -1, 9
  under a database); none of a structure or a procedure. `components` lists a structure with the arities
  it spans.
- The name `acs-view` appears in 26 Java files (139 lines), the pom, the two `.idea` files, the folder's
  `CLAUDE.md` and `agents-citable.md`, and `markdowns/`: `CLAUDE.md`, `agents-citable.md` (entries 30 and
  31's brackets), `agents-unreviewed.md` and `new-architecture.md`.

## The session

1. **Checkpoint.** `git status`; say whether the root's "keeps the queried data in memory" entry is now
   in `markdowns/agents-citable.md`. Present the design in ten lines, then two things to decide, and wait:
   - the views table: per view of the seven, what it shows, the requests it uses, and which of today's
     views it becomes or keeps (the navigator, the grid and the console included);
   - the content panels: what `arity-row`, `arity-identity`, `structure` and `procedure` show, and the
     reading of the click after focus.
   Ask anything else the design leaves open, with numbered options; the author: "The session can ask me
   questions."
2. **The rename**, mechanical; then `mvn -o -q compile` from the root.
3. **The focus**: an element of each type can be focused, from the navigator and from a panel, and
   dragged.
4. **The content panels**: the selection, the five types as the checkpoint settled, the click and the
   drop, any number open.
5. **The views** as the table says.
6. **What is read is kept**, if the entry is confirmed; otherwise skip and say so.
7. **Verify**: `mvn -o -q compile`. A check outside the repo, in `~/mein/claude/scripts/`, driving
   `ACSESS_GUI_Service` against an acsed booted on a scratch socket (as `acses-smoke/run.sh` does): a
   database made, an arity with two rows, identities captured, a value set; then each content type's
   reads, and kept data dropped after a change. Start the GUI once and say whether it opens; with no
   display, say so. Results dated in `~/mein/claude/results/`.
8. **Records**: propose entries for this round's decisions, in the format `CLAUDE.md` → Decisions gives:
   the rename to the root's `agents-unreviewed.md`; the class names, the views and the content panels to
   `acsess-gui/agents-unreviewed.md`, one `Replaces: acsess-gui 1` for the package. Add them on approval.
9. **Docs**: list the lines the work makes false (the folder's `CLAUDE.md`, `markdowns/CLAUDE.md`, the
   pom's description), propose replacements, edit on approval.
10. **Stop**: `git status`, the diff's summary, the checks' results. No commit until the author says.
