# Ontology Engine

To understand the evolution of the project, the steps are:
The "legacy" package, was the old package I used for a single version of that goal.
The "incubation" package is meant to be an empty improved architecture of it.
The "incubation.structurer" was the first serious try.
The "incubation.acse_beta" is the target of the project.

**A library for building arbitrary mathematical structures over N sorts of integer-identified
entities, supplying no interpretation of its own.** The engine mints identities, stores columns of
data against them, and maintains whatever relations a client declares between them. Meaning is the
client's.

A self-contained package of code for building any structure mathematically possible on N sorts of
entities. Do not confuse with the name "Ontology": unlike other tools named "Ontology", this one does
not extract ontology from the data and is not for building the ontology from the data. Not OWL, not
RDF, not a reasoner — no semantics are extracted, assumed or supplied. It is a performant way of
representing ontology on a computer: it stores entity sets and the relations between them in a
compressed form, and leaves every interpretation to the client.

This package is meant to allow any combination of mathematical structures on any arity.

All handwritten because the topic is abstract.

## The general shape

- **Sets.** Elements are represented as bare integer identities starting from 0.
- **Catalogs.** Data bound to a set, for each element or not, any way of getting it.
- **Structures.** Data about structure, can be for any number of sets, can have any rule.

| Example             | Form                                                              | Arity    |
|---------------------|-------------------------------------------------------------------|----------|
| the class hierarchy | a graph over that set, rules decide whether it's a tree or a DAG  | one set  |
| classes ↔ elements  | an indexer — many-to-many, with several possible implementations  | two sets |
| a binary relation   | a set of ordered pairs, one element from each set                 | two sets |

Relations are additive: a set can carry several, of different arities, at once.

That's "N arity structure builder", or "Structure of Arrays" builder.

## What it aims to be

**Optimized.** Fast enough that the only thing which would dramatically beat it on your workload is
something over-engineered for your one niche case.

**Extensible.** Features attach rather than being cut in. The architecture leaves plugin space by
design, so capabilities can ship as addons instead of as core.

**Universal.** Every use case these terms cover, not a chosen subset of them.

**Ontology.** In the full philosophical sense — any classification or representation of elements as
beings, not one schema language's idea of it.

**Management.** Persistence, reading, writing, analysing. The whole lifecycle, not just the query
side.

**Self-contained.** Carries its own needs. One runtime dependency, droppable into any project.

## What it is not

- Not OWL, RDF, or a reasoner. No semantics are extracted, assumed, or supplied.
- Not an ontology learner. It does not mine structure out of data.
- Not an ECS, though it converges on the archetype / structure-of-arrays layout. The aim is to pass
  the arity-2 ceiling ECS engines stop at — flecs packs exactly two participants into one 64-bit id.
- Not a general graph library. Structures are over identity sets, not over objects.
- Not a schema language.

## Where this came from

Built "bottom up", as the generalization of a niche problem: folders are not enough for organizing
huge quantities of files. The solution I decided on was letting folders have multiple parent folders.
To optimize it for the computer I decided to represent it by two arrays and an indexer between them —
one array for directories, which also carries a DAG, and the other for files. This was my project
called "Onto Directory".

While improving the architecture by separating the concerns and responsibilities into interfaces I
called "Segmentations", the AI chatbot told me the names of the segments I develop in formal
mathematics. For example the DAG was formerly called "Hierarchy", and the File Catalog was formerly
called "Element Data". And I realized everything I develop can be represented as a list of sets of
data plus modules to utilize them. This project is meant to allow building any kind of structure like
lego blocks, with ease.

It assumes no topology about the classification, neither the definition of classes nor of elements.
It just holds integers as identities for classes and elements, and indexes classes to elements using
an indexer, of which the user can choose any variant.

## Client side

You can use this engine in any programming language: give the code in this repo to your AI agent and
have it rewrite the engine for you (mostly it can).

## The application

The engine is a substrate. The application it is built toward: **OntoDirectory**, the sibling
project, is a file manager where a person curates an ontology by organizing and tagging their own
archive. That curated structure becomes the retrieval backbone for a RAG over paragraphs of facts,
with an **ingest agent** that digests PDFs into the store and an **answer agent** that responds from
it. Target use: a personal learning assistant; the same store works as durable agent memory.

Why it is interesting: published graph-RAG systems pay an LLM to *invent* their graph, and that
extraction is the field's central cost. Here the ontology is curated, personal and free — a side
effect of filing. Ontology-grounded RAG work reports large gains from schema-first retrieval but
assumes an institutional domain ontology; nobody else has the user's own hand-curated hierarchy as
the schema. That is a product position, not a research one.

## The question that decides it

Take a few hundred pages actually being studied, and test whether retrieval grounded in the
hand-curated hierarchy beats plain hybrid vector search on questions actually asked. If it wins,
there is a result nobody else can easily reproduce. If it loses, that is known early and cheaply.

The engine stands on its own either way and is not justified by the application: a personal
assistant's corpus is orders of magnitude smaller than the scale these representations target.

## The goal

In the author's words:

> I like the idea of ACS: it will be able to express everything with ease. I wonder if there were AI
> agents that build or audit it. So I have a scholar system: if I give a few hundred books on the
> field of CS, it would likely better organize and define CS from the entirety of the professors. So
> I want to build a system that builds paragraphs of knowledge, and then I will give a massive amount
> of rigorous books on the fields.

Whatever builds those paragraphs is a client: the engine still supplies no interpretation of its own.
