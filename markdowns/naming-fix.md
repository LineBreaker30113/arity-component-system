# Naming fix — Ontology Engine → Arity Component System

Staging file, written 2026-09-25. Everything below was applied, then reverted out of `README.md`,
`CLAUDE.md` and `pom.xml` so the author can edit the wording here first. In `README.md`, the title is
in effect since 2026-10-14, and of the subsection only its "says what it is" paragraph, in "Where
this came from" (2026-10-15); the `pom.xml` and `CLAUDE.md` parts are not.

## pom.xml

- `<artifactId>ontology-engine</artifactId>` → `<artifactId>arity-component-system</artifactId>`
- `<name>Ontology Engine</name>` → `<name>Arity Component System</name>`
- The `<description>` was left alone: it never carried the old name.

## CLAUDE.md

- Line 1: `# OntologyEngine` → `# ArityComponentSystem`

## README.md — the title

- `# Ontology Engine` → `# Arity Component System`
- A line under the title:

> *Formerly Ontology Engine. Why the name changed is at the end of "Where this came from".*

## README.md — a new subsection, at the end of "Where this came from"

> ### The name, and the one it had first
>
> The project was called **Ontology Engine** because the problem above was an ontology problem: a
> person filing an archive in OntoDirectory. The first use named it.
>
> Nothing ontological ever entered the engine. An identity is a bare int, a catalog is a column of
> values against those ints, and a structure is whatever relation a client declares. The engine
> supplies no meaning, so "ontology" described what the first client did with it, not what it is.
>
> What generalised it: one sort of entity became N, a structure came to span several of them at once,
> and the core grew thin enough that everything around it is a plugin. At that point the old name
> described a single application of a much wider thing.
>
> **Arity Component System** says what it is. An *arity* is one identity space — the occupied set plus
> its columns. A *component* is a plugin the core holds and drives. The *system* is the core and the
> components together. It is also the answer to the line above about ECS: same archetype layout,
> without the arity-2 ceiling.
>
> The old name is left visible on purpose. It is in the git history, in the `legacy/` package, in
> `OntologyEngineCore`, and in OntoDirectory, which is still the sibling project — and it records
> where the generalisation came from, which is the part worth keeping.

## Left unchanged on purpose

- `.idea/compiler.xml` still names the module `ontology-engine`; IntelliJ rewrites that file when the
  module is renamed there.
- Every `OntologyEngineCore` mention in `markdowns/` is the class, not the project.
- `legacy/`: renaming inside it only makes the old commits harder to read.
- The folder on disk, `.../OntologyEngine`, is the author's to rename after a session ends — renaming
  it under a running session breaks every path in use.

## To apply

Say so, and this goes back into the three files as edited here. This file stays afterwards.
