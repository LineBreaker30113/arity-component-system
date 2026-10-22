# Markdowns' new architecture: the author's words on the three-file method, until they find a better place

## From `markdowns/plan.md` → The author's words, copied as it is

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


## The migration session, 2026-10-22, with the grammar fixed

- [Where the Project instructions setting goes, picked among Claude's options:] "3, add it to the
  project's settings.local.json [`.claude/settings.local.json`]."
- [How the entries are organized:] "Entries should be organized like:
  Root: general vision, goal of each module.
  ACS: The meaning of the interfaces ("What arity represents?", "What are the responsibilities of the
  core?" and alike).
  ACSE: The implementations (Things like "In which cases this implementation performs better?" and
  "For which cases we can implement a performant class?").
  ACSES: The terminal application and alike.
  ACS-alHikmet: The agents that use the terminal to build the RAG from many resources; not my job, I
  gave it to a few brilliant newbies.
  ACSAH-viewer: The GUI for viewing a RAG; the last project, and it waits for the ACS-alHikmet.
  ACS-view: A simple GUI for ACSES; it allows editing and keeps the queried data in memory.
  OntoDirectory: Right now only a Set Hierarchy (instead of folders) method for sorting files; later we
  can integrate more of the ACS."
- [On Claude's readings of it:]
  - [1, relations between modules:] "Relations, as I said about the comparisons (many things follow the
    same rule), stay on the same level as the related stuff."
  - [2, project-wide entries that are neither the vision nor a module's goal go to the root:] "True."
  - [3, the package folders inside a module:] "No, they do not need to move up."
  - "For clarity, the goal is: an explanation inside of a folder only explains things inside of that
    folder and nothing else."
  - [4, the legacy code:] "Please find the places where an entry must go yourself; the rules are clear."
  - [5, the GUI entries:] "As the rules follow, if ACS-view explained itself in general (this is
    comparing it to the outside, so) it moves to the root. If it describes the elements inside the
    ACS-view, they must stay."
  - [6, the working method:] "Record it somewhere, like markdowns new architecture. We later can find it
    a better place." [Picked among Claude's options:] "2. A new file, markdowns/new-architecture.md".
- [On the summary file's name, Q2's "(Q2: a, except CLAUDE.md)", which the plan read as `AGENTS.md`:]
  "What [is] the "AGENTS.md"? I decided "CLAUDE.md"". [Then:] "yes, go ahead" [renaming the summaries
  to `CLAUDE.md` and dropping the Project instructions setting].
- [On an entry that replaces entry 23, "[Is the GUI OntoDirectory?] Yes, but needs improvements.":] "If
  we will replace, then explicitly state they both are GUI applications."
