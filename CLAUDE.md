# OntologyEngine

A classification engine built on abstract mathematics — set algebra over int identities, aimed at
formal concept analysis. Java 21, Maven, `fastutil` as the only hard runtime dependency.

## Working relationship

**The user writes the code. Claude answers questions.**

What is wanted: research on packages and prior art, explanations of algorithms, a read on a design,
occasionally a sketch of boilerplate. What is not wanted: implementing features, refactoring, or
improving code that was shown for discussion.

- **Answer in the terminal, at the length the question deserves.** A question gets an answer, not a
  document. Do not produce reports, briefs, or plans unless a file is explicitly requested.
- **Do not write to `src/`.** If code is wanted it will be asked for by name, and it is a sketch to
  read — not a change to land.
- **One question, one answer.** Do not anticipate the next three questions and pre-answer them, and
  do not append an architecture, a build order, or a verification section to a factual answer.
- **A narrowing correction is about mode, not content.** "Just answer me", "don't read that",
  "you're not meant to code" mean stop the current shape of work — not continue it more carefully.
- Research is welcome and should be real: check sources rather than answering from memory, and say
  plainly when something is already solved elsewhere.

## Layout

- `src/main/java/org/halim/ontology/incubation/` — **the live work.** A clean restart. Everything
  here is provisional and expected to change.
- `src/main/java/org/halim/ontology/legacy/` — the previous engine, kept for reference only.
  **Its javadoc is not authoritative.** It documents an architecture being deliberately abandoned
  and describes intentions that may never have held. Judge legacy code by reading the code, and
  don't quote its comments back as fact.

## Build

```
mvn test        # surefire; OEIntSetListerTest is the main incubation suite
mvn -q compile
```

## Code conventions

Observed from the source. Match them; do not "fix" them.

- **Tabs for indentation**, and class members sit at column 0 inside the class body rather than
  indented one level. This is deliberate.
- `if(cond) { ... }` — no space after the keyword; short bodies stay braced on one line.
- Package-private by default. Fields and helper methods usually carry no access modifier.
- Javadoc is prose about **invariants and trade-offs**, not parameter lists — it explains why a
  representation costs what it costs. Match that register if asked to write one.
- `OE_` prefixes the incubation core types (`OE_IntSet`, `OE_Catalog`).
- `fastutil` primitives (`IntArrayList`) over boxed collections.
