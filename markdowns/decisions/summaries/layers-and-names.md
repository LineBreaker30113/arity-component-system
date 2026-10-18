# Layers and names — summary

Claude's summary of `decisions.md` → Layers and names and `layers-and-names.md`; written 2026-10-18.
A reading aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

Current names: ACS is the system, `acse` the library, `acses` the service; the quotes keep the old
names (`asca`, `ASCA`, `ascar`, and `ACS`/`ASC` for the library).

## General

- Four layers: library, database application, requester, GUI or agent.
  > “There are four layers”
- The service is a database service; the requester is an adaptor the GUI and agents will use.
  > “is a database service” · “the GUI and agents will use the adaptor”
- The requester name and requesting through a file are killed (2026-10-14).
  > “We killed the ascr [the requester] name”
- The author guesses the new plan: a parent project that compiles all other projects.
  > “a parent project that will compile all other projects”
- Six submodules: ACS (the contracts), ACSE, ACSES, OntoDirectory, ACS-alHikmet, and ACSAH viewer, a
  new GUI like OntoDirectory for ACS-alHikmet's results, the one focusing on the planned RAG structure
  (2026-10-18).
  > “six submodules” · “viewing the results of ACS-alHikmet” · “focuses on the RAG structure I planned”
- The GUI is OntoDirectory, which needs improvements.
  > “Yes, but needs improvements”
- Right now no access control or cyber security; not in MVP scope (2026-10-14).
  > “out of the scope for MVP”
- The library will not be usable on its own.
  > “it will not be usable on its own”
- acse replaces acs, acses replaces acse; it now sounds like "access".
  > “acses replaces the acse”
- ACS is the system, ACSE its implementation, ACSES the database service using it (2026-10-14).
  > “ACSE is its implementation”
- Not ACC: ASP (Arity-Structure-Procedure) is likely trademarked, ASPCC would be the full name; go
  with acs, and OE_ becomes ACS_ (2026-10-14).
  > “which is likely trademarked” · “We better go with acs” · “"OE_ -> ACS_"”

## Details

- The service must have a way to be managed; requests can go through the requester.
  > “must have a way to manage it”
- Each change loads a lot into memory, so the service will likely not run per command; it is likely
  to stay open.
  > “load a lot of things to memory” · “will be open for a time”
- The service must accept requests many ways and must load .class files easily, possible since the
  core is thin.
  > “many ways of accepting requests” · “load .class files with ease”
- Loading is separate from using; load-folder and load-class methods must exist.
  > “loading folder and class methods must exist”
- Management and loading requests may come through acses, as right now there is not a privilege
  system (2026-10-14).
  > “there is not a privilege system on the access”
- Boot must launch a separate process, since that command line cannot be used for anything else.
  > “it must launch a separate process”
- acses launches the engine if and only if boot is the command and not anyone is running; otherwise
  it bridges client and engine.
  > “if and only if (boot is the command and there is not anyone running)” ·
  > “a terminal application for bridging client and database engine”
- Not booted and boot not requested: the engine fails loud and fast (2026-10-14).
  > “fails loud and fast”
- Package acse; interfaces start with ACS except the core, journal and persistence manager; ACS is
  the idea, the engine the database side (2026-10-14).
  > “the interfaces start with ACS, except the core, journal” · “engine is the database side”
