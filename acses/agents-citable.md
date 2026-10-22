# acses: citable

## Layers and names

*(Names as spoken at the time. The entry "For the naming, acse replaces the acs" sets the current
ones: the library is `acse`, the service program is `acses`; since 2026-10-14, ACS names the system,
`acse` implements it and `acses` uses it for the database service. Before that entry, `asca`, `ASCA` and the service `acse` mean `acses`; `ACS` and `ASC`
mean `acse`; `acsr`, `asrc` and `ascar` are the requester.)*

### Details

*(Old names: see the note under Layers and names.)*

- **1.** Since asca is a service, it must have a way to manage it, while the requests can go through the
  ascar.
- **2.** Asca needs to load a lot of things to memory in order to make any change, so it will likely not be
  used as `asca components ~/db3`; in other words, it is likely the asca will be open for a time.
- **3.** My suggestion for the ASCA, it must allow many ways of accepting requests, also it must be modular,
  being able to load .class files with ease, which is possible since the core is too thin.
- **4.** Loading is simply loading classes as accessible components. Loading a class and using [it] are
  separate things, so loading folder and class methods must exist.
- **5.** [Which ways of accepting requests may carry the management and loading requests:] right now there
  is not a privilege system on the access, so the answer is the acses. *(2026-10-14)*
- **6.** Also if the command `asca boot` becomes the working instance, that command line cannot be used for
  anything else, so it must launch a separate process of asca.
- **7.** The expected use case is the "acse" [the service] calls will launch [the database engine] if and
  only if (boot is the command and there is not anyone running), otherwise the acse is a terminal
  application for bridging client and database engine for requests and responses.
- **8.** If not booted and not boot requested, the engine fails loud and fast. *(2026-10-14)*

## Persistence

- **9.** The expected use case won't have multiple programs. One program that will be open as a service,
  [the] rest are requests.

### Details

- **10.** I gave up the idea for [the] ability to change base classes; if [a] user wants to use a different
  class, it can just decide to.
- **11.** When acses is going to create a new database, the acses will provide the base class files only
  once, during [the] creation of the database.
- **12.** [The device config:] I mean the initial implementation classes we are creating here will be stored
  for the ACSES application; the user will run the ACSES application to create a database anywhere it
  wants, then the ACSES application will copy class files on its side; using the "config" folder is
  not obligatory. *(2026-10-14)*
