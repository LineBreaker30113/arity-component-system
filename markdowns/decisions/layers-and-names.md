# Layers and names

*(Names as spoken at the time. The entry "For the naming, acse replaces the acs" sets the current
ones: the library is `acse`, the service program is `acses`; since 2026-10-14, ACS names the system,
`acse` implements it and `acses` uses it for the database service. Before that entry, `asca`, `ASCA` and the service `acse` mean `acses`; `ACS` and `ASC`
mean `acse`; `acsr`, `asrc` and `ascar` are the requester.)*

- If we think about it, it [ACS] is a library. There are four layers: 1 library, 2 database
  application, 3 requester, 4 GUI or 4 Agent.
- The ACS is a library, the archiver (hereby "asca") is a database service, [the] requester (hereby
  "ascar") is an adaptor, [and] the GUI and agents will use the adaptor.
- Since asca is a service, it must have a way to manage it, while the requests can go through the
  ascar.
- The reason we need a separate application to request is because asca needs to load a lot of things
  to memory in order to make any change, so it will likely not be used as `asca components ~/db3`; in
  other words, it is likely the asca will be open for a time, so it cannot be used like
  `asca <command>` unless we make the main function check if the service is already open and move
  into the "command" mode, which is also a valid approach, but a separate application can serve as a
  protocol.
- I realised the idea of the main function of "asca" deciding to act as ascar if there is already an
  asca process running is actually nice, we can do it to reduce our work.
- I guess the new plan is to make a parent project that will compile all other projects in order to
  develop our applications.
- For now, let's make this project only two modules, ASC [the library] and ASCA [the service] (which
  can handle requests in many ways).
- We can also make modules for the OntoDirectory and alike.
- [Is the GUI OntoDirectory?] Yes, but needs improvements. Also we can make another GUI application
  that focuses on the RAG structure I planned.
- My suggestion for the ASCA, it must allow many ways of accepting requests, also it must be modular,
  being able to load .class files with ease, which is possible since the core is too thin.
- By "plugins" I meant components like arity, catalog or structure components, since they will have
  well defined contracts.
- Loading is simply loading classes as accessible components. Loading a class and using [it] are
  separate things, so loading folder and class methods must exist.
- [Which ways of accepting requests may carry the management and loading requests:] right now there
  is not a privilege system on the access, so the answer is the acses. *(2026-10-14)*
- Also if the command `asca boot` becomes the working instance, that command line cannot be used for
  anything else, so it must launch a separate process of asca.
- The expected use case is the "acse" [the service] calls will launch [the database engine] if and
  only if (boot is the command and there is not anyone running), otherwise the acse is a terminal
  application for bridging client and database engine for requests and responses.
- ACS is the library, it will not be usable on its own.
- For the naming, acse replaces the acs, and acses replaces the acse. Now it sounds like access, which
  is cool.
- [Every rename the handoff lists:] rename all. *(2026-10-13)*
- I will keep the name ACS for this project, the ACSES will be a (maven) module of this project, and
  ACSES will also be another sub module. Do not do it yet. I want to upload the messy version of this
  code base first for keeping its identity.
- The ACS is a system, as it names; ACSE is its implementation, and ACSES uses it for [the] database
  service. *(2026-10-14)*
