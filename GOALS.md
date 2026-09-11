# Goal and plan

The one file an agent should read to learn *what this project is for*. `ARCHITECTURE.md` says how the
engine is built; `CLAUDE.md` says how to work here; `reference-rules.md` says what is in `reference/`
and what may be done with it. Everything about intent lives here, and nowhere else.

---

## The goal

**OntologyEngine is a library for building arbitrary mathematical structures over N sorts of
integer-identified entities, supplying no interpretation of its own.**

The engine mints identities, stores columns of data against them, and maintains whatever relations a
client declares between them. It never decides what an identity means. Meaning is the client's, and
the engine's job is to make the client's meaning cheap to read.

## The words, defined

The sentence above uses several words that carry other meanings elsewhere. In this repository:

- **Ontology** — the *philosophical* sense: any classification or representation of elements as
  beings. Not OWL, not RDF, not a description-logic reasoner.
- **Entity** / **identity** — a bare non-negative `int` or `long`, minted densely from 0 and unique
  within its arity. It carries uniqueness and nothing else: no meaning, no ordering beyond ascending,
  no structure.
- **Arity**, as a noun here — *one identity space*: the occupied set of identities plus the columns
  of data hanging off them. (Distinct from arity as a number, which is how many identity spaces a
  structure spans.)
- **Catalog** — one value per identity; a column. Total: it answers for every identity, and an
  unwritten slot reads as zero rather than as absent.
- **Structure** — data about relations, over one arity or between several, under any rule. A class
  hierarchy over one arity is a DAG; an indexer between two arities is many-to-many. Arities above
  two are the point of the project.
- **Module** — currently ambiguous in the source, and a known open naming problem. See
  `ARCHITECTURE.md`.

## What the definition assumes

These are load-bearing. If one stops holding, the design under it stops being justified.

- Identities are **dense and local to an arity**, so their values stay small. That is what lets
  byte- and short-wide set representations carry real arities rather than toys. The premise ends if
  identities ever become global across arities, or carry generation counters.
- **Leaves outnumber branch nodes** in hierarchies, and parent×child count stays under a constant.
- **Edits may be slow; reads may not.** Every trade in this codebase spends write time to buy read
  speed or exactness.
- One hard runtime dependency (`fastutil`). Java 21, Maven.

## What it is not

- Not OWL, RDF, or a reasoner. No semantics are extracted, assumed, or supplied.
- Not an ontology *learner*. It does not mine structure out of data.
- Not an ECS, though it converges on the archetype / structure-of-arrays layout. The explicit aim is
  to pass the **arity-2 ceiling** ECS engines stop at — flecs packs exactly two participants into one
  64-bit pair id.
- Not a general graph library. Structures are over identity sets, not over objects.
- Not a schema language.

---

## The application

The engine is a substrate. The application it is being built toward:

**OntoDirectory** — the sibling project, an ontology-powered file manager — is where a human curates
the ontology by organizing and tagging their own archive. That curated structure, produced as a
by-product of filing, becomes the retrieval backbone for a RAG over paragraphs of facts. Two agents
sit on top:

1. an **ingest agent** that digests PDFs into the store, and
2. an **answer agent** that responds from it.

Target use is a personal learning assistant; the same store repurposes as durable agent memory.

### Why this is the interesting part

Every published graph-RAG system pays an LLM to *invent* its graph, and that extraction is the
field's central cost problem. OntoDirectory means the ontology is **curated, personal and free** —
authored by the user as a side effect of work they were doing anyway. The ontology-grounded RAG
literature reports large gains from schema-first retrieval, but it uniformly assumes a pre-existing
*institutional* domain ontology. Nobody has "the user's own hand-curated hierarchy" as the schema,
because nobody else has the file manager that produces one.

That is the project's defensible position. It is a product position, not a research one.

---

## Prior art

Not restated here. `reference-rules.md` records what is already solved elsewhere and by whom, beside
the code and papers that establish it. Read it before deciding something is new.

## The question that decides it

Before the engine has to carry anything: take a few hundred pages actually being studied, and test
whether retrieval grounded in the **hand-curated** hierarchy beats plain hybrid vector search on
questions actually asked.

If it wins, there is a result nobody else can easily reproduce, and a reason to scale into a
purpose-built substrate. If it loses, that is known early and cheaply.

Either way the engine stands on its own terms. It is wanted for itself, and it is not to be
justified by the application — the corpus a personal assistant sees is orders of magnitude smaller
than the scale these representations are designed for, and pretending otherwise would be building on
a premise that does not hold.
