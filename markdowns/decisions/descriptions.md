# Descriptions

- [The description:] it must describe each instance of [a] component of the core using a string value;
  it must be able to be read and set. It is there so the agent can state the purpose of each
  component. *(2026-10-11)*
- It [the description] is somewhat of a journal itself, so no need of journalling, except while
  creating and adding we can require the requests take another string parameter for the description.
- Descriptions are few strings, and do not take much space; we can represent them in the easiest way.
- Also classes can have a description. For an example, there can be many different implementations
  of mathematically [the] same indexing logic (say two arities, one element from arity one is
  connected to one element from another arity or not) but different ways of representing them,
  performant for different cases. The agent [is] meant to choose between them; also the agent can
  decompile the class files to make its own descriptions. *(2026-10-14)*
