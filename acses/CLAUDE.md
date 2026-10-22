# acses

- The service must have a way to be managed, while requests can go through the requester (1); it loads a
  lot into memory, so it is likely open for a time, not run per command (2).
- It must accept requests in many ways and be modular, loading .class files with ease; loading and
  using a class are separate, so folder- and class-loading methods must exist (3, 4).
- Right now there is not a privilege system, so management and loading requests come through acses (5).
- `boot` must launch a separate process, as its command line cannot be used for anything else (6); the
  engine launches if and only if boot is the command and none runs, otherwise acses only bridges client
  and engine (7); not booted and no boot asked, it fails loud and fast (8).
- The expected use won't have several programs: one service, the rest requests (9).
- A user can just pick a different class instead of changing base classes (10); acses provides the
  base class files only once, at a database's creation, from the initial classes stored for it; a
  "config" folder is not obligatory (11, 12).
