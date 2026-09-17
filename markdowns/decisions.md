# Decisions

The author's words. Rules: `CLAUDE.md` → Markdown files.

## Process

- This is the plan: we code the classes to design; later we will add the boilerplate, like bulk
  access to columns in many types (int, long, double).

## Architecture

- I guess we better rename the architecture to "plugin" based, since the core is almost redundant.
- I needed to make a "Structure" interface, meant to be the second type of elements (the relations
  between sorts of elements) in the "OntologyEngineCore".
- We need to clarify each component is a component, so rename things like "ArityComponent" and
  "StructureComponent".
- The delegation-like actions should be done there [in the core].
- The ontructure package can be renamed but must be used, because incubation contains many unwanted
  elements that are not worth removing.
- The former version's missing features were critical for the application. I need the modularity and
  "Logging, Undo-Redo, ..." at the same time.

## Identities and removal

- I plan on using [the vacuum idea] where the index of an identity is converted to identity at the
  ArityModule's identitiesSet.
- If the identity number rather than the index is given (which is handled by the identities set of
  the ArityComponent), it auto skips the removed ones; we just need to specify using it like that.
- When execution (through the multi-arity structures, and maybe few other cases I do not remember
  right now) reaches identities, they check if they exist, when necessary.
- The removal and binary search [over the hole list] likely will never get that slow.
- We should allow a judgment call method alongside auto method (does what you say) for the client.
- Structure is not responsible for the element count anyway; it is just the structure add-on.
- I am not sure I got it: consider there is an indexer (2-arity structure) that reached a dead
  identity. Should it vacuum it alone? That would be harmful.
- I think the vacuum will be a rare call, on storing to disk and reloading from it.
- We can also make a "forget" call that removes the associated data (like the connections on the
  indexer) but doesn't shift all the identities.
- Most importantly, if the removed identity gets captured back, firstly a forget call must run on it.
- The forget is necessary at the capture back because the data of the former identity is still there,
  so forget does remove the data but does not shift.
- "Make vacuum the undo horizon." This is also good; make this the current solution. We might not need
  undo after we vacuum.
- The vacuum destroys all, so it [breaking references on undo after a vacuum] is unavoidable, but
  certainly suppressible: since the vacuum step is meant to shift the necessary pointers, it will just
  do the same in the undo trace. Since the new identity can only be one number that can be calculated,
  it can do it.
- The vacuum step can edit the values inside of the journal or can give a mapper; we better make a
  mapper interface.

## Journal and undo

- Sometimes it [a change] should not even be snapshotted, just noted.

## Identity sets

- The usage of "short" over "int" in the identities set saves up to only 8KB, redundant for a Java
  application, so we can delete both short and byte variants.
- Add PositiveLong and [Positive]Int, also deleting the narrow variants, to the plan.
- Right now the identitiesSet doesn't need to hold a "firstIndex"; doesn't really matter though.

## Filtering

- The filtering service is meant to have logical combinations of filters (AND, OR, SUBTRACT, NOT,
  NAND, NOR, XOR, IFOF, IMPLIES, NIMPLIES, CONTRADICTS, NCONTRADICTS), but also things that depend on
  catalog or structure data, like namedLike or childOf.
- Right now the filtering service can only provide the logical operators, and filters on data depend
  on the client. This is somewhat intended, since we allowed the data to vary, but some modules to
  simplify the client's experience must be done.
