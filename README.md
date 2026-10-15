# Arity Component System

*Formerly Ontology Engine.*

**ACS (Arity Component System) is a system, as its name states, for building arbitrary mathematical
structures over N sorts of integer-identified entities, supplying no interpretation of its own. ACSE
is its implementation, and ACSES uses it for a database service.**

## Vision

Any system can be represented as data about the objects, the structures on them, and procedures
about them:

- **Arities** hold the data of a group of objects: one table per sort of record, with its catalog
  rows as columns. A row can be stored, or calculated from the identity.
- **Identity sets** only match an identity to memory. Each record is a dense integer identity, so
  deleting a record never moves the rest.
- **Structures** hold the relations between identities, over one arity or several, and know nothing
  about data or memory. Many structures may do the same thing, each optimized another way; they keep
  their rules themselves, and an arity can take part in several at once.
- **Procedures** (planned) are behaviors: they depend on some of the data and structure, and when
  run they change some of it. They must be clear enough to be reasoned about automatically.

A neural network, for example, is two arities, neurons and synapses (weights), and a procedure that
sets every non-input neuron to its bias (a synapse from the neuron to itself), adds each synapse's
weight times its first neuron's value to its second neuron, and applies a neuron's activation once
its inputs are done.

The same architecture represents a database as easily, which suggests it can represent anything,
and with high expressiveness, because of the contracts, so the result can be reasoned about
mathematically. In short: FCA4J (a Java library for formal concept analysis), but as a database and
multi-arity. An "N arity structure builder", or "Structure of Arrays" builder.

## Goal

An ACS database is managed with commands. ACSES is one program: `acses boot` starts the service if
none is running, and every other command is a request to the running one, failing fast if there is
none.

```
acses boot                # start the service
acses loadDB ~/main       # load the database in ~/main, named "main" after its folder
acses components main     # list its components
```

Requests come from a terminal, a GUI, or an AI agent, and each carries a comment, like a commit
message: every change is made from a resource and may later be audited.

The agents build knowledge from books:

1. The first agent builds a personal knowledge graph from the books.
2. An auditing agent audits and improves it.
3. A third agent builds a new ACS of paragraphs from the first, one that describes better and is
   copyright free. Paragraphs cite their resources and reasoning.

Agents must be allowed at every step, to automate it; humans too, to study how proper knowledge
graphs are developed. The GUI is OntoDirectory, the sibling project, once improved; another GUI may
focus on the RAG structure. The architecture suits RAG specifically, since it is the most expressive
by itself.

Where it started, in the author's words:

> I like the idea of ACS: it will be able to express everything with ease. I wonder if there were AI
> agents that build or audit it. So I have a scholar system: if I give a few hundred books on the
> field of CS, it would likely better organize and define CS from the entirety of the professors. So
> I want to build a system that builds paragraphs of knowledge, and then I will give a massive amount
> of rigorous books on the fields.

## The products

#### ACSE (Arity Component System Engine)

An engine that has the responsibilities isolated into atomic interface contracts.
Has the journaling mechanism for undo/redo, checkout, and logging. While the easy and boilerplate
part of the ACSE is not developed, the engine inside is robust enough to quickly index/search
between 2^28 files of records using only about a few GiB of RAM memory.

#### ACSES (ACSE Service)

A terminal based database management application that utilizes the ACSE for creating
and managing databases in any folder. The database structure is very adaptable to the point it can
completely quit being an ACS. Creation of a database is done by copying the base classes of the ACSE
to the database, after that the ACSES doesn't care if the database uses those classes or not and it
only checks if the contracts (defined by interfaces) are satisfied. The ACSES can load any database
folder: it opens the folder's persistence manager, which builds the core from the classes in the
folder's own jar, so the database, not the ACSES, is the authority about implementation. The ACSES
does not exist yet; the library side of creating and loading does.

## Navigating the code

To understand the evolution of the project and navigate the code base, the steps are:

1. The "legacy" package was the old package I used for a single version of that goal.
2. The "incubation" package is meant to be an empty canvas for an improved architecture of it.
3. The "incubation.structurer_depr" was the first serious try.
4. The "incubation.acse" is the target of the project.

## What it aims to be

**Optimized.** Fast enough that the only thing which would dramatically beat it on your workload is
something over-engineered for your one niche case.

**Composable.** Features attach rather than being cut in. The architecture leaves plugin space by
design, so capabilities can ship as addons instead of as core.

**Universal.** Every use case these terms cover, not a chosen subset of them.

**Ontology.** In the full philosophical sense — any classification or representation of elements as
beings, not one schema language's idea of it.

**Management.** Persistence, reading, writing, analysing. The whole lifecycle, not just the query
side.

**Self-contained.** Carries its own needs. Two runtime dependencies, fastutil and RoaringBitmap;
droppable into any project.

## What it is not

- Not OWL, RDF, or a reasoner. No semantics are extracted, assumed, or supplied.
- Not an ontology learner. It does not mine structure out of data; agents that do are clients.
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
mathematics. For example the DAG was called "Hierarchy", and the File Catalog was formerly
called "Element Data". And I realized everything I develop can be represented as a list of sets of
data plus modules to utilize them. This project is meant to allow building any kind of structure like
blocks, with ease.

**Arity Component System** says what it is. An *arity* is one identity space — the occupied set plus
its columns. A *component* is a plugin the core holds and drives. The *system* is the core and the
components together. It is also the answer to the line above about ECS: same archetype layout,
without the arity-2 ceiling.

## Novelty study

A prior-art and novelty study of this architecture (2026-10-10) is at
[`markdowns/novelty-study.md`](markdowns/novelty-study.md).

## Licence

GNU Affero General Public License, version 3 only, with the Universal FOSS Exception, Version 1.0:
see [LICENSE](LICENSE) and [NOTICE](NOTICE). A commercial licence is also offered for use that does
not meet those terms; contact the copyright holder.
