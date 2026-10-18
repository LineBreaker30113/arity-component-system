# Descriptions — summary

Claude's summary of `decisions.md` → Descriptions and `descriptions.md`; written 2026-10-18. A
reading aid, not a source: the decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

## General

- Each component instance of the core must have a string description that must be readable and
  settable, so the agent can state its purpose (2026-10-11).
  > “describe each instance of [a] component of the core using a string value” ·
  > “so the agent can state the purpose”
- Classes can have descriptions too: implementations of the same indexing logic (elements of two
  arities connected or not) can differ in representation and performance; the agent is meant to
  choose, and can decompile classes to describe them (2026-10-14).
  > “Also classes can have a description” · “decompile the class files”

## Details

- Descriptions need no journalling, except that creating and adding can require a description
  parameter.
  > “no need of journalling”
- Descriptions are few strings, do not take much space, and can be represented the easiest way.
  > “represent them in the easiest way”
