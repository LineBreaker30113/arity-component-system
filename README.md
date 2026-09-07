# Ontology Engine

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
