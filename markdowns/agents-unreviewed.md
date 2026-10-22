# root: entries to review

# Unreviewed

- The interfaces in `acs` start with `ACS_`, the core, journal, persistence manager and description
  manager included. *(Author, 2026-10-18: "Hereby interfaces in the acs start by ACS_ instead.")*
  Conflicts: root 32 [The prefixes:] (2026-10-14) would lose "except the core,
  journal and most importantly the persistence manager". Commons: "do it as "OE_ -> ACS_""
  (root 29). Complements: "ACS: The contract interfaces" (root 22). acse 10's
  "make an "ACSE_PersistenceManager" class" keeps the name it had.
  Section: Layers and names / Details
- During an inspection (the rollback `inspectAt`) the journal refuses a write loudly; a command
  waiting for the return answers with the current activity index and the count of activities after
  it. *(Author, 2026-10-19: "Refuse write loudly, and add a command to "wait" for returning to the
  former version, the command returns the current activity index and how many activities are
  pending."; pending picked among Claude's options: "Activities after the current point".)*
  Complements: "break undo history" (acs 41), which an inspection cannot move past.
  Section: Journal and undo
- ACSES passes requests to the core; those commands are planned in another session. *(Author,
  2026-10-19: "as the goal it gives commands to the Core with the request comment and returns what
  Core returns."; "I am planning them in another session.")* Commons: "Later, in `acses`: commands
  that interact with the arities" (`acses`, unreviewed).
  Section: Layers and names / Details
- A request is described by a line in a small string grammar, written by reflection from the methods the
  implementation marks, so no name is a literal; acsed gathers a database's lines into `acse-language.txt`
  at its root, which acsess reads. *(Author, 2026-10-20: "Go with (b), the string grammar"; "Only
  problem with b is, we need to automatically read the method names from the code instead of string
  literals."; "The implementation meant to define the requestables. I suggest a
  "getRequestables(DataOutputStream dos)" that describes the available request options."; "The file
  that describes commands can be in the database root named as "acse-language.*" or alike.")*
  Section: Layers and names / Details
- acsed's own commands are not journaled and do not go through `ACS_Actor`. *(Author, 2026-10-20: "ACSED
  will not be journalled."; "ACSED commands do not need to go through the ACS_Actor, right?")*
  Section: Journal and undo
- ACS-view is the acsess GUI, in a separate Maven module, `acs-view`: a database tool like SQL viewing
  tools that also edits, OntoDirectory's GUI cloned into it for the structure, not the views, and
  running acsess; its GUI design decisions are in `acs-view/agents-citable.md` and root 30, root 31. *(Author, 2026-10-21:
  "For context, we later will also make a similar application called ACS-view, that is simply the
  acsess GUI."; 2026-10-22: "I want you to clone the onto directory GUI to a new maven module of named
  acs-view, and make it prompt the acsess to work and visualize stuff."; [what the clone keeps:] "Not
  the views, but the structure of the GUI."; "I need you to build a separate maven module for the goal
  I specified. Do not make in place edits on other projects. The ACS viewer is a data base tool just
  like SQL viewing tools that also allow editing."; "These two is about the project, keep it at the
  root"; "Also move the SQL-tool sentence.")* Commons: "Then we will develop a GUI for that application
  that uses the terminal input" (root 2) and "the GUI and agents will use the adaptor"
  (root 18), acsess being both. Conflicts: "[The project ACS has] six
  submodules" (root 21) would lose its count, ACS-view being a seventh; asked.
  Section: Layers and names
- Verbatim: [ACS-view and OntoDirectory:] The ACS-view organizes ACS based databases; OntoDirectory is a
  special version that organizes files with ACS. [In replacing entry 23 "[Is the GUI OntoDirectory?]
  Yes, but needs improvements.":] they both are GUI applications. *(2026-10-22)*
  Section: Layers and names
  Replaces: root 23
- Verbatim: ACS-alHikmet: The agents that use the terminal to build the RAG from many resources; not my
  job, I gave it to a few brilliant newbies. *(2026-10-22)*
  Section: Layers and names
- Verbatim: ACSAH-viewer: The GUI for viewing a RAG; the last project, and it waits for the
  ACS-alHikmet. *(2026-10-22)*
  Section: Layers and names
- Verbatim: ACS-view: A simple GUI for ACSES; it allows editing and keeps the queried data in memory.
  *(2026-10-22)*
  Section: Layers and names
- Verbatim: OntoDirectory: Right now only a Set Hierarchy (instead of folders) method for sorting files;
  later we can integrate more of the ACS. *(2026-10-22)*
  Section: Layers and names
