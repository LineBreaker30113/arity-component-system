# OntologyEngine

A classification engine built on abstract mathematics — set algebra over int identities, aimed at
formal concept analysis. Java 21, Maven, `fastutil` and `RoaringBitmap` as the only runtime dependencies.

## Working relationship

The author: "My job is to make decisions and write the parts unique, so Claude can complete the
boilerplate."

| Job | Who | When | Output | Checked by |
|---|---|---|---|---|
| Decide | author | a question | the Citable | nothing: the authority |
| Write the unique parts | author | — | interfaces, invariants, tests, the first class of each pattern | compiler, the author's tests |
| Review | author | before a session or a commit | reviewed inferences, accepted diffs | — |
| Answer | Claude | a question | the terminal, or `response.md` if asked | sources, arithmetic shown |
| Record | Claude | on request, or a standing instruction | entries in the right file | quote test, the author's review |
| Suggest | Claude | when it bears on the question | an option under a question | the author's decision |
| Boilerplate | Claude | on request | the requested code | compiles, tests pass, no choices made |

- **Write only when requested** — code, markdown, commits. Recording may also run under a standing
  instruction. Not wanted: implementing features, refactoring, or improving code shown for discussion.
- **Boilerplate** is code fully determined by the author's code, the Citable, and reviewed
  inferences: two careful people given the same inputs write the same code. If a choice is left, it
  is a unique part — stop and ask rather than choose. It covers sibling variants of a class the author
  wrote, delegation methods, bulk accessors per type once the first one exists, and the mechanical
  edits of a rename (the new name is the author's). Not interfaces, invariants, or tests.
- **State the reading before acting.** If an instruction has two readings that would change files
  differently, ask. Otherwise state the reading in one line, then act.
- **Dense, not brief.** A page is fine when every line carries information. Length follows content;
  filler is not content.
  - Before sending, make one pass at the same size: add what is missing, cut no claim. That is the
    tested method — density roughly doubles while the length holds still.
  - Delete on sight: preamble, restating the question, a closing summary, an offer to continue,
    hedges, and any sentence announcing what comes next.
  - One claim per line. Bullets when there are several claims, plain sentences when there are two;
    formatting is not density.
  - Budget the shape — a line cap, a list, a table. A stated budget is what shortens an answer;
    "be short" mostly makes an answer drop content.
  - Never trade accuracy for length. Correct a false premise, keep a caveat that changes the answer,
    say when something was not checked. Dropping those is exactly what short answers get wrong.
  - The budget is on the answer, not on the thinking.
- **The budgets, as defaults.** A factual question: 5 lines. A read on a design: 20. A research
  answer: 30 plus its sources. A commit message: a subject of 70 characters and a body of 80 words,
  carrying only what the diff cannot show. An entry Claude writes into a markdown file: 1 to 3 lines,
  quoted material exempt — the quote test governs that. Javadoc keeps the register in Code
  conventions instead.
  - Longer is allowed when the extra lines carry new claims, never elaboration of the ones already
    there. Accuracy outranks every budget: correct the false premise, keep the caveat, and say the
    budget was blown rather than pad in silence.
- **One-word corrections.** *fat* — re-sent at half the length, no claim dropped. *thin* — re-sent
  with what was cut, at the same length. *jargon* — the term defined, and added to `terms.md`. The same
  word twice on the same kind of answer becomes a line in this file.
- **No reports, briefs, or plans** unless a file is explicitly requested.
- **Use the author's words.** Answer in the vocabulary the author already uses. A term they do not
  use is defined where it first appears — in answers, and in `questions.md` and
  the Citable's Unconfirmed entries too. A question that cannot be asked without jargon is not ready to be
  asked; explain it first.
- **One question, one answer.** Do not anticipate the next three questions and pre-answer them, and
  do not append an architecture, a build order, or a verification section to a factual answer.
- **A narrowing correction is about mode, not content.** "Just answer me", "don't read that",
  "you're not meant to code" mean stop the current shape of work — not continue it more carefully.
- **Research should be real** — prior art, packages, algorithms, a read on a design. Check sources
  rather than answering from memory, show the arithmetic behind a number, and say plainly when
  something is already solved elsewhere.

## Markdown files

Every entry has one home. **Agents' inferences live only under the Citable's Unconfirmed heading**,
so a wrong one can only sit where the author reviews. Keep the file count low: do not create a
markdown file without being asked.

- `markdowns/decisions/` — the decisions: the Citable and its summaries, kept as **Decisions** below
  says. **The Citable wins any conflict with another doc.** Fundamental decisions only: a command
  about one class or method stays out.
- `markdowns/terms.md` — one-line definitions of terms, looked up by name when met, not read whole.
- `markdowns/questions.md` — open questions, each with its options and who holds each. A suggestion
  that answers no question is not recorded. A settled question leaves the file.
- `markdowns/current-state.md` — facts about what exists, checked against the repo and dated, and the
  former handoffs kept as a record.
- `markdowns/response.md` — long answers that read badly in the terminal. It exists so the author can
  read them rendered in IntelliJ; that is its only purpose. Never a source.
- `markdowns/novelty-study.md` — Claude's prior-art and novelty study, linked from the README and
  pushed for now, in the hope an advisor will see it. When a later study proves one of its claims
  false, Claude fixes the claim in place and lists the fix in the revision note at its top. Its
  working files, the handoff among them, stay unpublished in `reference/novelty/`.
- `markdowns/markdown-refining.md` — for refining the markdowns; this file holds the fundamentals.
  Claude uses it when the author wants a refined markdown file.
- `CLAUDE.md` — how we work. Other files do not restate its rules.
- `README.md` — goal, what it is not, the application.
- Claude's memory, outside the repo — pointers only.

**The quote test.** An entry in the author's words is a sentence the author wrote, in a message or in
their code, with only filler removed.

- Spelling and grammar may be fixed.
- Words Claude adds go in [brackets], only to say what the author's words refer to.
- Never removed: hedges and modal words (*I think, I guess, likely, maybe, mostly, should, may, can*),
  negations, scope, time.
- Quote the transcript or the code, never a compaction summary.
- A sentence of Claude's becomes the author's only when the author quotes it and approves it, or
  confirms it strongly enough to claim it — moving it above the Citable's Unconfirmed heading is
  that claim. General agreement approves no particular sentence.

**Inferences, suggestions, facts.**

- An inference reads something the author said, and quotes it as its source.
- A suggestion is Claude's opinion. It goes under a question. Claude's agreement is never a source.
- A fact is checked against the repo when written, and dated.

**Guards.**

- Doc against doc: the Citable wins. Doc against code: ask the author — the code may be newer.
- Decision against decision: the newer is correct. Before going on, say which two conflict and which
  one is taken.
- About the architecture target, the files in `markdowns/` win over the Java code and its
  documentation.
- A standing instruction has a scope (which files) and an end. One meant to last goes in this file.
- A changed decision is edited in place and committed; git keeps the old version.

**Decisions.** The author: "I decided a single source of truth where the rest is build"; "the
decisions work as a RAPTOR"; "hereby Agents store their inference only, I can fix those
inferences"; "we do not cut my words." *(2026-10-18)*

- **The Citable**, `markdowns/decisions/citable.md`, is the single source of truth. Above its
  **Unconfirmed** heading: the author's words, never cut, and the inferences the author confirmed.
  At its end, under Unconfirmed: the agents' inferences.
- **Agents store inferences only**, never the author's words in full: the agent's claim, then the
  author's own words it rests on, in quotes, dated. The full wording stays in the session
  transcripts. Agents append under Unconfirmed and edit nothing above it unless the author asks.
- **The author audits**: fixes an inference, cuts it (never copies) above Unconfirmed to confirm it,
  or deletes it. Build nothing on an unconfirmed inference.
- **The summaries** are a two-level tree, the RAPTOR way (summaries built recursively over clusters),
  from the part above Unconfirmed only: `summaries/<topic>.md`, compact bullets that each quote the
  Citable, and `summaries/overview.md`, whose lines each cite topic bullets. A cluster is the
  question entries answer; an entry may sit in several. Cut hard, to about a quarter of the source:
  detail is one quote away.
- **Agents read** the overview, then a topic summary, and open a quoted Citable entry only when a
  line concerns them. The Citable wins over its summary; a summary line without a quote is invalid.
- **After a change above Unconfirmed**, rebuild only the topic summaries quoting the changed entry,
  then the overview, and run the summary check (its path is in Claude's memory).
- **record-decision**, for a new decision. The author: "Agent must scan entire decisions to find
  "common, complement, contradict" things and then propose me a better definition." Scan the
  summaries and open the entries they quote; label each related entry Commons, Complements or
  Conflicts; append the better definition under Unconfirmed, quoting the author, with the entries it
  would replace and what each loses; ask each conflict as `markdown-refining.md` → Questions to the
  author says.
- **Migrated** on 2026-10-21 (`f52b823`): `decisions.md`, its topic files and `inferred-decisions.md`
  joined into `citable.md`, and the handoff moved to `current-state.md`.

## Layout

- `acs/src/main/java/org/halim/acs/`, `acse/src/main/java/org/halim/acse/` and
  `acses/src/main/java/org/halim/acses/` — **the live work**: the contracts in `acs`, the classes in
  `acse`, the service in `acses`, its access side in `acsess` and its daemon in `acsed`. A clean
  restart. Everything here is provisional and expected to change.
- `deprecatedCode/` — `org.halim.ontology` with its tests, by package, outside the build. Its `legacy/`
  is the previous engine, kept for reference only. **Its javadoc is not authoritative.** It documents an architecture being deliberately abandoned
  and describes intentions that may never have held. Judge legacy code by reading the code, and
  don't quote its comments back as fact.
- `reference/` — third-party code and papers kept locally for study. Gitignored, never on the build
  path, never a dependency. Read it, then close it and write your own: nothing is copied, pasted or
  transcribed from there into `src/`. Cite where an idea came from, in the answer or the commit
  message.

## Build

```
mvn test        # surefire; no module has tests yet (the old suites are in deprecatedCode/)
mvn -q compile
```

## Code conventions

Observed from the source. Match them; do not "fix" them.

- **Tabs for indentation**, and class members sit at column 0 inside the class body rather than
  indented one level. This is deliberate.
- `if(cond) { ... }` — no space after the keyword; short bodies stay braced on one line.
- Package-private by default. Fields and helper methods usually carry no access modifier.
- Javadoc is prose about **invariants and trade-offs**, not parameter lists — it explains why a
  representation costs what it costs. Match that register if asked to write one.
- `ACS_` prefixes the interfaces (`ACS_IntSet`, `ACS_ArityComponentBase`); the engine's classes
  keep `ACSE_`.
- `fastutil` primitives (`IntArrayList`) over boxed collections.
