# Layers and names — details

The author's niche decisions; the general ones are in `decisions.md`. Rules: `CLAUDE.md` → Markdown
files.

*(Old names: see the note under Layers and names in `decisions.md`.)*

- Since asca is a service, it must have a way to manage it, while the requests can go through the
  ascar.
- Asca needs to load a lot of things to memory in order to make any change, so it will likely not be
  used as `asca components ~/db3`; in other words, it is likely the asca will be open for a time.
- My suggestion for the ASCA, it must allow many ways of accepting requests, also it must be modular,
  being able to load .class files with ease, which is possible since the core is too thin.
- Loading is simply loading classes as accessible components. Loading a class and using [it] are
  separate things, so loading folder and class methods must exist.
- [Which ways of accepting requests may carry the management and loading requests:] right now there
  is not a privilege system on the access, so the answer is the acses. *(2026-10-14)*
- Also if the command `asca boot` becomes the working instance, that command line cannot be used for
  anything else, so it must launch a separate process of asca.
- The expected use case is the "acse" [the service] calls will launch [the database engine] if and
  only if (boot is the command and there is not anyone running), otherwise the acse is a terminal
  application for bridging client and database engine for requests and responses.
- If not booted and not boot requested, the engine fails loud and fast. *(2026-10-14)*
- [The prefixes:] [The] package becomes acse but the interfaces start with ACS, except the core,
  journal and most importantly the persistence manager. ACS is the idea, engine is the database
  side. *(2026-10-14)*
