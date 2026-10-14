# Novelty study: an agent-organized, multi-arity FCA database

Written 2026-10-10, by Claude, at the author's request, read-only on the repository. Companion
files, kept locally outside the repository (`reference/novelty/`, gitignored): `checkpoints.md`
(protocol, evidence notes, the Uniqueness-file table), `search-log.md` (169 rows). Scripts:
`<claude>/scripts/` (`gpatents.py`, `openalex.py`, `oa_abstract.py`, `fetchpdf.sh`,
`novelty_logrow.sh`); patent output: `<claude>/results/novelty-patents-2026-10-10_0515.md`, where
`<claude>` is Claude's own folder, named in Claude's memory.

**Revised 2026-10-14.** Later passes (2026-10-11 to 2026-10-13: procedures, a deeper pass, PostgreSQL,
RAG structures; kept locally) proved some claims below false or too strong. They are corrected in
place:
- §1: the best single source is PostgreSQL taken as a whole (five elements present), not
  Kötters & Eklund (four); C2 and C3 are not weaker than C1.
- §6: stronger prior art for E2, E3, E6, E7, E8, E11 and E14.
- §7: OB2 is medium-strong for relational stores; OB3's "nothing measures it" was wrong (DIGIMON).
- §8: I6's "all prior code-writing agents rewrite whole memory programs or whole engines" was wrong
  (SpacetimeDB, Convex Chef); I1 and I7 name closer prior art.
- No later pass found a source holding every element: the "not anticipated" verdicts stand.

Terms used below, defined once:
- **anticipation**: one prior source holds every element of a claim, arranged as claimed.
- **obviousness**: a skilled practitioner would have combined known sources, for a reason that can be
  named. Used here as a discipline, not as law.
- **claim chart**: a table of claim elements against one source, each cell backed by a quoted passage.
- **snowballing**: following a source's references (backward) and the works citing it (forward).
- **grey literature**: documentation, blogs and repositories rather than peer-reviewed papers.
- **capture–recapture**: estimating how much a search missed from the overlap between two
  independent search channels.
- **MDL** (minimum description length): prefer the description that makes the data shortest.
- **FCA terms**: a *formal context* is objects × attributes with an incidence; a *concept* is a
  closed pair (objects, attributes); *RCA* (relational concept analysis) runs FCA over several
  contexts linked by relations; *n-adic / polyadic* contexts have an n-ary incidence.

Evidence grades: **A** read in full, **B** read past the abstract (relevant sections), **C** abstract,
README or docs page, **D** secondary source.

## 1. Answer

The claim, as you put it: "a very modular data base architecture so an AI agent can organize the
data base architecture. I wonder if this is ever done." Tested as three nested claims (§2).

- **Not anticipated.** No source found holds every element of C1 (the database), C2 (C1 organized by
  an agent) or C3 (C2 plus books-to-paragraphs and topology retrieval). The best single source is
  PostgreSQL taken as a whole (its documentation, read 2026-10-12): 5 of the 11 elements of C1 at
  "present" level (E1, E2, E3, E5, E9). The best FCA source covers 4 (Kötters & Eklund 2018, an FCA
  navigation layer over a relational database). Evidence: strong for the 13 charted candidates (11 read past the
  abstract), medium overall (§10: the wider neighbourhood is far from exhausted, but snowballing added
  no new research line).
- **Every element exists somewhere.** Nothing in your architecture is new *as a part*. Dense
  per-sort identities and column storage: ACSets (2022), anchor modeling (2010). Plug-in modules that
  maintain one relation's rule with their own algorithm: Modular Materialisation (2019), Soufflé
  `eqrel`, Cyc's HL modules. Undo by per-record-type inverse functions: Berkeley DB. A comment per
  transaction: Datomic, TerminusDB; with a revert per transaction, pgMemento on PostgreSQL. A core
  extended by user-written types, functions and access methods: PostgreSQL, since POSTGRES (1986).
  Multi-arity FCA: n-RCA (IJAR 2026). Agents redesigning a store's schema or storage: EvoOntology,
  TypeDB/Neon MCP, SpecDB, MemEvolve, M*, MemCodex (all 2025–26).
- **"FCA4J but as data base and multi-arity."** The multi-arity half now exists on the analysis
  side: n-RCA "directly supports n-ary relations between multiple relational contexts" (2026). The
  "as data base" half — persistent, mutable, journalled, served, with structures as plug-ins — was not
  found in the FCA literature; the closest are read-only layers over a relational database
  (TOSCANA, Kötters & Eklund) and a single-sort file system (LISFS).
- **Obviousness** (§7): C1 is arguable as a five-source combination, medium strength. C2 is
  medium-strong when the store is relational: the PostgreSQL ecosystem already has a comment and a
  revert per transaction, and agents that change schema, storage and indexes. It stays weaker for
  structures carrying mathematical invariants. C3 is medium. Each link and its reason is named;
  none is a refutation.
- **Where the distance is** (§8): the parts specific to your substrate — structure components that
  guard mathematical invariants across N arities, a journal of inverse steps with a comment per
  request as the agent's and auditor's interface, and concept computation as the query service over
  a live store. The bare framing "an agent organizes the architecture" is crowded: MemEvolve alone
  gathered 85 citing papers in ten months. Later passes kept these distances and narrowed the first:
  LogicBlox (SIGMOD 2015) enforces declared rules in one engine, so what differs is the component
  form, and its benefit has to be measured.

## 2. The claim

Elements (D = `decisions.md`, M = your message, B = the study brief); full quotes in
`checkpoints.md` → Checkpoint 0.

| # | Element | Source |
|---|---|---|
| E1 | database managed by commands through a requester/service | D "managed with commands" |
| E2 | thin core + arity, catalog, structure components with contracts | D "components like arity, catalog or structure components" |
| E3 | component classes loaded at runtime | D "loading folder and class methods must exist" |
| E4 | N identity spaces, dense identities local to each | D "Arities are the first type of element" |
| E5 | catalogs per arity, table hand-written or modular | D "define [the] table itself or use a modular table" |
| E6 | structures over any number of arities, guarding their own rules | D; M "The structures ensure their rules themselfs" |
| E7 | core delegates (plugins, forget, journal, persistence, filtering, analytics) | D "The core handles creation-addition of plugins, delegations, ..." |
| E8 | journal: steps with inverses, undo/redo, request begin/end, comment per request | D "store a comment ... for every action" |
| E9 | query service: logical filters + catalog/structure-dependent leaves | D "essentially a query service" |
| E10 | agents and humans at every step | D "Must allow agents at every step ... humans in every step" |
| E11 | an agent organizes the architecture: (a) types per arity/structure, (b) storage modules, (d) retire with data migrated | M "so an AI agent can organize the data base architecture"; M "a, b(...), d" |
| E12 | books → KG → audit → cited paragraphs | D "The first agent will build the personal knowledge graph from the books ..." |
| E13 | the agent grows or retires structure types | B |
| E14 | topology as a retrieval signal for RAG | B; D "good specifically for RAG" |
| E15 | universality | D "The engine must be able to express anything." |
| E16 | concept computation (closure, implications) through the query service | M "filtering service will unavoidable be a strong query service" |

C1 = E1–E9, E15, E16. C2 = C1 + E10, E11, E13. C3 = C2 + E12, E14. Writing component code (Q2) is
treated as an improvement (§8), not as an element.

## 3. Method as run

Protocol first (`checkpoints.md` → 0), per Kitchenham & Charters 2007. Anticipation by the
all-elements rule (MPEP 2131; EPO G-VI 1, no combining sources); obviousness only with a named
combination, an MPEP 2143 rationale and a stated reason (In re Kahn); improvements by the EPO
problem–solution approach (G-VII 5). Channels: web search (103), Google Patents through its query
endpoint (9 incl. 5 scripted), Espacenet (1, refused), GitHub (7), OpenAlex (10 sweeps), and 10
closing gap searches: 140 queries. Snowballing: 12 rounds, forward via OpenAlex (older seeds) and
Semantic Scholar (2025–26 seeds), backward via the reference lists of 10 downloaded PDFs; about 160
citing works and 370 references screened by title. 17 fetch rows: 12 papers read past the abstract
(LatWeave was read in the step that found it), 3 documentation pages, 1 proceedings index, 1 refused. Grey sources graded by Garousi et al. Guideline 11. Stopping: effort bound per query plus
saturation (Garousi Guideline 8); capture–recapture check in §10. Deviations: DBLP's API sits behind
a bot check and Semantic Scholar rate-limits, so venue sweeps used OpenAlex and web search;
Espacenet's REST interface returned 403.

## 4. Candidates charted

| # | Candidate | Date | Grade | Nearest claim |
|---|---|---|---|---|
| K1 | ACSets / Catlab (Patterson, Lynch, Fairbanks) | 2022 | A−/B | C1 |
| K2 | Anchor Modeling (Rönnbäck, Regardt et al.) | 2010 | A− | C1 |
| K3 | OpenCog Hyperon + Distributed AtomSpace | 2023 | B | C1, C2 |
| K4 | TypeDB + its MCP server | 2025–26 | C | C1, C2 |
| K5 | FCA4J + n-RCA (Bendjeddou, Kahn, Sekhari, Wang) | 2022, 2026 | A / C | C1 (FCA) |
| K6 | Kötters & Eklund + TOSCANA lineage | 2018 / 1995 | B / C | C1 (FCA as DB) |
| K7 | EvoOntology (Chong, Zhang, Fan, Du) | Sep 2026 | A− | C2 |
| K8 | MemEvolve (+ M*, MemCodex) | Dec 2025–Sep 2026 | B / C | C2 |
| K9 | ALMA (Xiong, Hu, Clune) | Feb 2026 | B | C2 |
| K10 | SpecDB (+ Bespoke OLAP) | 2026 | B / C | C2 |
| K11 | OaK, ontology-as-a-kernel (Zhang, Sun, et al.) | Aug 2026 | B | C2 |
| K12 | LatWeave (Ren et al.) | Sep 2026 | B | C3 |
| K13 | StructRAG (Li et al.) | ICLR 2025 | C | C3 |

## 5. Claim charts

P present, p partly, — absent, ? not determinable from what was read.

| | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 | E11 | E12 | E13 | E14 | E15 | E16 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| K1 ACSets | — | p | p | P | P | p | — | — | p | — | — | — | — | — | p | — |
| K2 Anchor | p | — | — | P | P | p | — | p | p | — | — | — | — | — | p | — |
| K3 Hyperon | P | p | p | — | — | p | p | ? | P | p | p | — | p | — | P | — |
| K4 TypeDB+MCP | P | — | — | p | p | p | — | ? | P | p | p | — | p | — | p | — |
| K5 FCA4J+n-RCA | p | p | — | P | p | p | — | — | p | — | — | — | — | — | — | P |
| K6 Kötters/TOSCANA | p | — | p | P | P | p | — | — | P | p | — | — | — | — | — | P |
| K7 EvoOntology | P | p | p | — | — | p | — | p | p | p | p | — | p | — | — | — |
| K8 MemEvolve/M* | — | p | p | — | — | — | — | — | p | p | p | — | p | — | — | — |
| K9 ALMA | — | p | p | — | — | — | — | — | p | p | p | — | p | — | — | — |
| K10 SpecDB | P | p | — | — | p | — | — | p | P | p | p | — | — | — | — | — |
| K11 OaK | — | p | p | — | — | p | — | — | P | p | p | — | p | — | — | — |
| K12 LatWeave | — | — | — | p | — | p | — | p | P | p | — | p | — | P | — | p |
| K13 StructRAG | — | — | — | — | — | — | — | — | p | p | p | p | — | p | — | — |

Notes per candidate: the evidence behind the P cells, the difference in your framework that
matters, and the re-check of whether the candidate covers that difference after all.

**K1 ACSets** ([arXiv 2106.04703](https://arxiv.org/abs/2106.04703);
[ACSets.jl API](https://docs.juliahub.com/General/ACSets/stable/api/)).
- E4 P: each schema object is a set of parts with integer ids; parts types `DenseParts`,
  `MarkAsDeleted` ("deletions are lazy and arrays are not resized until garbage collection"),
  `BitSetParts`, `IntParts`; `rem_part!`: "the removed part is actually replaced by the last part,
  which is then deleted". Difference: you keep identities stable on removal (forget) and compact
  only at vacuum. Re-check: `MarkAsDeleted` + `gc!` is exactly that split, so the difference is
  covered — ACSets anticipates your identity layer, including vacuum-by-swap.
- E5 P: attributes and foreign keys stored column-wise. Difference: interchangeable storage module
  per column, or a hand-written table. Re-check: storage is generated per acset type; choice exists
  per parts type and per indexed hom ("Only keys that are frequently queried should be indexed"),
  not per column. Difference holds, partly.
- E6 p: relations of any arity as spans, schema equations; but "it is the user's responsibility to
  ensure that any direct mutation of the acset data structure preserves the equations" (§6, listed
  as future work). Difference holds: no component guards a rule (no acyclic DAG, no reduction).
- Absent: service, journal/undo, per-request comment, plug-in loading, agents, FCA.

**K2 Anchor Modeling** ([DKE 2010](https://www.anchormodeling.com/wp-content/uploads/2011/05/Anchor-Modeling.pdf)).
- E4 P anchors = identity sets with surrogate keys; E5 P one table per attribute (6NF); E6 p ties
  over two or more anchors with roles; schema evolution "on-line and non-destructively".
- E8 p: "it is always possible to find the rows that belong to a certain batch, came from a specific
  source system, were loaded by a particular account". Difference: no undo by inverse steps, no
  request comment. Re-check: errors are removed "by scripts"; difference holds.
- Absent: plug-in components, structures with rules, agents, FCA. A modeling method over an RDBMS.

**K3 Hyperon / DAS** ([arXiv 2310.18318](https://arxiv.org/abs/2310.18318) §3.1, §3.3).
- E1 P (DAS runs as a server); E9 P (pattern matching, subgraph queries); E15 P (metagraph, links of
  any arity, links to links).
- E10/E11/E13 p: "the way the AI agents interact with DAS is always by using it as the source of
  knowledge and the container of any computational result"; MeTTa programs "rewrite portions of
  Atomspace"; types are sub-metagraphs; caching policy set at runtime by subgraph patterns.
  Difference: no demonstrated agent that chooses storage modules or structure kinds; a later search
  found the type-creation capacity only as theory (reflective metagraph rewriting, arXiv 2112.08272).
- Absent: per-sort dense identities, catalogs as columns, structure components with own algorithms,
  journal with comments (not found), FCA.

**K4 TypeDB + MCP** ([hypergraph docs](https://typedb.com/docs/use-cases/hypergraph/index.md);
[MCP server](https://typedb.com/docs/tools/mcp-server/index.md);
[Cursor + MCP blog](https://typedb.com/blog/ai-assisted-typedb-development-with-cursor-and-mcp)).
- E1 P, E9 P; E6 p: typed n-ary relations with roles; relations play roles.
- E11(a)/E13 p: through MCP an agent ran schema queries, built a schema, and "refactored a booking
  concept from an entity into a relation". Difference: no structure kinds with algorithms, no
  storage choice (b), no retirement with migration (d) shown. Re-check: not covered.
- Grade C: TypeDB's identity representation and constraint annotations were not checked.

**K5 FCA4J + n-RCA** ([FCA4J paper](https://ceur-ws.org/Vol-3308/Paper17.pdf); clone read;
[n-RCA, IJAR 2026](https://doi.org/10.1016/j.ijar.2026.109782)).
- E16 P: lattices, AOC-posets, Duquenne–Guigues and D-bases; n-RCA "directly supports n-ary
  relations between multiple relational contexts", tested on Knomana's ternary relations. E4 P:
  objects and attributes by int index; several object sets in an RCA family. E2 p: integer-set
  implementations chosen through a factory "to select the most appropriate one".
- Difference: concepts over a live, mutable store. Re-check: incremental lattice maintenance
  (AddIntent, FastDeletion 2015, distributed updates 2025) and on-demand RCA (Bazin et al. 2018)
  supply the techniques. So "live FCA" is a known combination; what is missing is the store itself.
- Absent: persistence (file in, file out), mutation API as a database, journal (grep of the clone:
  no undo or journal code), agents.

**K6 Kötters & Eklund + TOSCANA** ([CEUR 2149/7](https://ceur-ws.org/Vol-2149/paper7.pdf);
[Stumme 1998](https://www.kde.cs.uni-kassel.de/stumme/papers/1998/FODO98.pdf)).
- "a relational database is transformed into a family of n-ary contexts (a so called power context
  family)" — E4 P (sorts = table names), E5 P (many-valued contexts = tables), E9 P (conjunctive
  queries with facets), E16 P (concepts as query/table pairs). Follow-ups: conjunctive query pattern
  structures (DAM 2019), Granada (2019).
- Difference: read-only over an external RDB; no own storage, mutation, plug-ins, agents.
  Re-check: not covered. This is the nearest "FCA as database + multi-arity".

**K7 EvoOntology** ([arXiv 2609.15779](https://arxiv.org/abs/2609.15779), read in full except appendix).
- E1 P (MCP server). E11(a)/E13 p: "maintains a versioned ontology state comprising content, schema,
  and tool layers"; one-level typed edits kept only after paired validation, otherwise the state
  rolls back; "Tool-level edits account for 57% of the cumulative gain across six accepted rounds".
  E8 p: versions and Evidence objects; no step journal or request comment.
- Difference: its "schema" is the admissible node and relation types of a semantic layer over
  external tables and files; yours is arities, catalogs and structure components with invariants
  and storage forms. Re-check: not covered.

**K8 MemEvolve + M* + MemCodex** ([2512.18746](https://arxiv.org/abs/2512.18746) B;
[M* 2604.11811](https://arxiv.org/abs/2604.11811) C; [MemCodex 2609.39765](https://arxiv.org/abs/2609.39765) C).
- MemEvolve: "decomposing any memory architecture into four key components: Encode, Store,
  Retrieve, Manage" and "evolves the programmatic implementations of these modules". M*: "models
  an agent memory system as a memory program written in Python ... the data Schema, the storage
  Logic, and the agent workflow Instructions". MemCodex rewrites "how each layer is constructed,
  indexed, retrieved, and routed".
- E11(a)(b) p and E13 p, for agent experience memory. Difference: no shared multi-sort substrate, no
  structures with invariants, no journal, no human audit; architectures are searched offline per
  task. Re-check: not covered.

**K9 ALMA** ([arXiv 2602.07755](https://arxiv.org/abs/2602.07755) §3). A meta agent writes memory
designs as code over `general_update()` / `general_retrieve()`; "each sub-module implements its
update() and retrieve() logic, and optionally maintains its own database". Same difference as K8.

**K10 SpecDB (+ Bespoke OLAP)** ([2605.31097](https://arxiv.org/abs/2605.31097) B;
[2603.02001](https://arxiv.org/abs/2603.02001) C). Ten functional modules × implementation variants
(FODA); "a companion selection component translates a natural-language workload description into a
set of implementation variants"; a subagent writes each module. E11(b) p, at generation time, for a
relational engine. Difference: no runtime organization of a live store, no KR structures, no FCA.

**K11 OaK** ([2608.22974](https://arxiv.org/abs/2608.22974) §1–2). Kernel K = (schema S, typed
functions F); "compiles generic operators into executable domain functions"; "requirements on
computation and workflow are enforced by the control flow of functions". E6 p (operations guarded by
functions, close to "structures guard their rules"), E11(a)/E13 p. Difference: a per-task kernel
frozen for inference, not a persistent store. Re-check: not covered.

**K12 LatWeave** ([2609.27225](https://arxiv.org/abs/2609.27225) §1–2). "the LLM is restricted to a
'builder + query translator,' and answers are produced deterministically by three operators on the
lattice"; "the hit instances form the answer, each carrying its source_text". E14 P (lattice algebra
is the retrieval space), E9 P, E12 p (Web text, not books; citations per hit). Difference: a product
lattice of value dimensions, not FCA concepts over N arities; no modular store, no journal.

**K13 StructRAG** ([2410.08815](https://arxiv.org/abs/2410.08815)). A DPO-trained router picks one
of five structure types (table, graph, algorithm, catalogue, chunk) per task, and an LLM structures
documents at inference time. E11(a)/E14 p, transient. Difference: no persistent store, fixed menu
of five types.

## 6. Where each element exists alone

| Element | Strongest prior art found (grade) |
|---|---|
| E1 | DAS server; TypeDB; every DBMS. Agent-facing: TypeDB MCP, Neon MCP (C) |
| E2 | PostgreSQL: "extensible because its operation is catalog-driven", with user-added types, functions, operators and access methods, a design goal since POSTGRES 1986 (B, 2026-10-12); Dittrich & Geppert, *Component Database Systems* (2000): plug-in components, configurable DBMS (C); Hyperon Space API (B) |
| E3 | PostgreSQL incorporates "user-written code into itself through dynamic loading" (B); flecs `ecs_import_from_library` loads modules from shared libraries at runtime (A); SpacetimeDB publishes compiled modules into a running database (B); DuckDB `INSTALL`/`LOAD` (C) |
| E4 | ACSets parts types (A−); anchor modeling anchors (A−); flecs/ECS entity ids (reference clone) |
| E5 | ACSets columns (A−); anchor 6NF (A−); Bevy `Table`/`SparseSet` per component (B) |
| E6 | Soufflé `eqrel`, "a self-computing data-structure" (B); Cyc SBHL keeps the closure of `genls` (C); Modular Materialisation, "four pluggable functions" per module (B); flecs relationship traits kept by the store (Symmetric, Exclusive, OneOf; `Acyclic` detects but need not refuse), pairs only (A); LogicBlox integrity constraints that abort violating transactions (B); PostgreSQL uniqueness and exclusion enforced inside index access methods (B); Agrawal–Borgida–Jagadish 1989 compressed closure (D) |
| E7 | Modular Materialisation (B); flecs cleanup traits propagate removals so the store holds no dangling references (A); Cyc: "the most efficient one which can make progress on it does so" (D); DAS routing by index (B) |
| E8 | pgMemento on PostgreSQL: a client message stored per transaction (`pgmemento.session_info`) and `revert_transactions`, reverting row deltas (B); Berkeley DB application-specific recovery: each record type's function must "either reapply or undo the update" (B); PostgreSQL custom WAL resource managers (B); Datomic transaction metadata (C); TerminusDB commit author + message per write (C); EMF command stack (C); Protégé change history with rollback (C) |
| E9 | TypeQL; Hyperon pattern matching; Kötters conjunctive queries (B) |
| E10 | SSKG Hub: source span per relation, actor and time per edit, expert review (C); Modern Relay Omnigraph, agents propose on branches and humans merge (C, grey) |
| E11 | (a) EvoOntology (A−), TypeDB MCP (C), OaK (B); (b) PostgreSQL table access methods, switched by `ALTER TABLE … SET ACCESS METHOD` (B), Postgres MCP Pro, where an agent chooses indexes with hypothetical indexes (C), SpecDB (B), Bespoke OLAP (C), AgenticDB (C), M* (C), CollectionSwitch (C), RelC auto-tuner (C); (d) Neon MCP branch migrations (C), CQL Σ/Δ/Π migrations (C), anchor modeling evolution (A−) |
| E12 | KARMA nine-agent KG enrichment with verification (C); GrOIL decision chain per term (C); AutoformBot / Munkres formalized by agents (C); MASEO (C) |
| E13 | MINDSET: each episode may "reinforce, supersede, split or create a schema" (C); EvoOntology schema edits (A−); AutoSchemaKG (C); KGist and SUBDUE by MDL (C) |
| E14 | DIGIMON (PVLDB 2025): 12 graph-RAG methods compared over five graph types and 19 retrieval operators on one testbed (B, 2026-10-13); HippoRAG (A, in reference); LatWeave (B); ContextRAG, a fuzzy concept lattice over chunk embeddings as the retrieval graph (C); SimGRAG structural retrieval (C); StructRAG (C) |
| E15 | Hyperon metagraph (B); ACSets "any finitely presented schema" (A−) |
| E16 | FCA4J, conexp-clj (A, clones); n-RCA (C); incremental AddIntent/FastDeletion (C); on-demand RCA (A, in reference); lattice views over SPARQL (C) |

## 7. Obviousness, with combinations named

Each is stated as the combination, the MPEP 2143 rationale, the reason found in the sources, and
what it leaves out. None is a refutation; each is a constraint on how the novelty is argued.

**OB1 — C1.** ACSets (K1) + Modular Materialisation + Berkeley DB recovery records + Datomic
transaction metadata + FCA4J/n-RCA, with Kötters 2019 as the bridge.
- Reasons: ACSets names its own gap (invariants under mutation, future work) → rationale G (a
  suggestion in the prior art); Modular Materialisation supplies pluggable rule modules → A
  (combining known elements); Berkeley DB supplies undo per record type → A; Datomic supplies the
  comment per transaction → A; "Conjunctive query pattern structures: a relational database model
  for FCA" (DAM 2019) gives a reason to run FCA over relational structures → G.
- Strength: medium. Five sources from four communities; no source suggests the whole arrangement.
- Left out: structures that refuse invalid writes (rather than derive consequences); catalog
  storage chosen per column; requests as journal activities with client comments while steps
  belong to the application; forget before capture-back.

**OB2 — C2.** OB1's substrate + TypeDB MCP or Neon MCP (agent changes the schema through a command
protocol, with rollback) + EvoOntology (gated typed edits) + SpecDB / M* (agent chooses or writes
storage) + MINDSET (create, split, supersede schemas by an objective).
- Reasons: "Supporting Our AI Overlords" (CIDR 2026) argues data systems should be redesigned for
  agents → G; "Harness the Memory" finds "no single substrate consistently dominates" and calls for
  substrate routing → G for (b); EvoOntology's gate applied to a new store → D (known technique, known
  product ready for improvement).
- Strength: medium-weak as stated here. It inherits OB1 (chained) and needs a step no source shows:
  an agent choosing among structure kinds that carry mathematical invariants (acyclic hierarchy,
  transitive reduction, equivalence, many-to-many indexer) across N identity spaces.
- Correction (2026-10-12): for relational stores the step is shown. PostgreSQL (an extensible core,
  storage and index plug-ins) + pgMemento (a comment and a revert per transaction) + Supabase, Neon
  or Tiger MCP and Postgres MCP Pro (agents change schema, storage choice and indexes) + pg_tle
  (installing code safely). These are combined in practice → G. That is medium-strong. The left-out
  step above still holds.

**OB3 — C3.** LatWeave + KARMA/MASEO + GrOIL + StructRAG + DIGIMON.
- Reason: all target auditable knowledge construction from documents → A.
- Strength: medium. The 2026-10-10 text said "topology across structure kinds as a retrieval
  signal" was weak because nothing measured it. That was wrong: DIGIMON (PVLDB 2025) compares
  retrieval across five graph types and 19 operators. What no source measures is retrieval across
  structure kinds that share identities in one multi-sort store.

## 8. Improvements toward uniqueness

Each respects `decisions.md`: structures guard their own rules, the core delegates and checks
nothing on their behalf. Ordered by gain over cost.

| # | Improvement | Cost | Gain | Distances from |
|---|---|---|---|---|
| I1 | **Structure descriptors as the agent's vocabulary.** Each structure component reports, through the core, the arities it spans, its op codes, the rule it keeps (in words: "acyclic", "covering relation", "equivalence") and a cost profile; catalog modules report bytes per identity (already `getEstimatedBytesPerIdentity`). Informational only — the structure still enforces its rule. The deprecated core interface already had `getDescription` / `getStructureDescription`. | low | the agent picks kinds by stated rule and cost; no store found exposes rule-carrying kinds to an agent as its vocabulary | StructRAG (5 fixed transient types), TypeDB (types without algorithms), Modular Materialisation (modules chosen by humans), flecs relationship traits (rule-carrying, pairs only, for programs) |
| I2 | **Evidence in the journal.** Each request's comment carries the source passage id (book, page, span) the agent acted on; the audit agent and humans undo by request. | low | every structure edit is cited and reversible by request: E8 + E10 + E12 at the substrate | GrOIL (decision chain per OWL term, no undo), Datomic (generic tx metadata), SSKG Hub (soft delete, no inverse steps) |
| I3 | **FCA as the agent's evidence for growing structures.** The query service computes concepts / AOC-posets over a 2-arity indexer, and n-RCA over n-ary structures, on demand; the agent proposes a hierarchy structure, or a new arity, from them, and an LLM names the concepts. | medium (closure over `IdentitiesSet` exists; n-RCA is heavy) | a formal, checkable criterion for vocabulary growth (E13) | Cimiano et al. JAIR 2005 (FCA hierarchies from text, no agent, no store); CONCEPTS 2025 LLM-assisted RCA (naming, no store); EvoOntology / MemEvolve (no FCA) |
| I4 | **MDL-gated lifecycle.** Accept or retire a structure instance when it shortens the stored description (bytes plus exceptions), measured by the core's analytics, before any costly task evaluation. | medium | a cheap, principled gate for E11(d)/E13 | MINDSET (energy over conversation schemas), KGist / SUBDUE (MDL on graphs, no agent or store), EvoOntology (accuracy gate only) |
| I5 | **What-if by journal.** An agent's tentative plan is a journal segment undone if it is rejected; "vacuum is the undo horizon" bounds it. No copy is made. | low–medium (journal already planned) | branches without copy-on-write storage | Neon (copy-on-write branches), Git4Data, agent-first branched updates |
| I6 | **(c) Agents write structure components** against the contracts, loaded through "load class", accepted only if your tests for that kind pass and the journal codec round-trips. | high | the strongest single distance, though not as a mechanism: agents already write code loaded into running databases (SpacetimeDB modules upgraded feature group by feature group, Convex Chef). None writes against structure contracts that guard invariants, with the inverse steps supplied by the components | SpacetimeDB, Convex Chef, ALMA, MemEvolve, M*, MemCodex, SpecDB, Bespoke OLAP |
| I7 | **Measure it.** Per-structure-kind and topology-only retrieval ablations on a book corpus (TAPL), against DIGIMON's testbed (12 graph-RAG methods, five graph types), LatWeave, StructRAG and HippoRAG. | medium | turns the combination into a result | DIGIMON measures retrieval by graph type, one graph per run; no source measures it over structure kinds sharing identities in a multi-sort store |

Constraints raised by these improvements, each with its workaround:
- **Expressiveness tax.** Many encodings of one fact break retrieval. Workaround: I1's descriptors,
  plus ontology-to-tools compilation (arXiv 2602.03439), so the agent writes only through registered
  kinds.
- **Scale.** RCA's known scalability wall. Workaround: on-demand concept and neighbourhood
  computation (Bazin et al. 2018); intersections in the `IdentitiesSet` algebra (Roaring).
- **Loading classes.** Loading class files by path is a code-execution channel. Workaround: the
  same-machine-only option already in `questions.md`, plus I6's test gate.
- **Drift.** Errors seed later errors (NELL). Workaround: undo by request (I2), an auditor agent, and
  MINDSET-style hysteresis.
- **Crowded framing.** "An agent organizes the architecture" alone will not read as new. Workaround:
  lead with I1–I3 and I7.

## 9. Claims of `Claude-Response-Uniqueness.txt`

The full table, U1–U18 with sources, is in `checkpoints.md` → Checkpoints 5. Summary:
- **Hold:** U1 HyperGraphRAG; U2 AutoSchemaKG (92%, 95% in one version); U3 EvoOntology including
  "57%"; U4 ALMA; U7 A-MEM; U8, U9 Buehler (U9's link points to a dataset README; the paper is
  arXiv 2601.04878); U10 EDC; U11 ontology-to-tools; U12 MIDGARD; U13 motifs via compression; U14
  GraphRAG-Bench; U15 LILO; U16 word-to-world models; U17 NELL drift; ologs and functorial data
  migration.
- **Partly:** U5 HGMem — method holds, but "ICML 2026" was not confirmed (arXiv says under review).
  U6 HippoRAG — "10–30×" is in the body, the abstract says 10–20×. AtomSpace as "nearest ancestor":
  nearest for agent self-modification only; ACSets, anchor modeling and Kötters are nearer on
  identity spaces and catalogs.
- **Fails:** U18 "retrieval by structural isomorphism ... none of these do" — true of the three
  systems named, but SimGRAG (ACL Findings 2025) and MAC/FAC (1995) do it. "The newest close one is
  ALMA" — no longer: EvoOntology, OaK, M*, MemCodex and MINDSET are newer and closer.
- **Still holds after this search:** "Not found: an agent that grows its own structure vocabulary
  over an arbitrary-arity store from books and is evaluated on comprehension", and "nothing combines
  ... a persistent store where the agent picks among mathematical structure types, built from books,
  with topology measured as the retrieval signal". Closer pieces than the file knew: TypeDB MCP,
  MINDSET, M*, LatWeave.
- **Not checked:** costs, prices and exchange rates; "steep learning curve"; the Yoneda framing.

## 10. Search statistics and completeness

- **Volume.** 169 log rows: 140 queries (113 web, 9 Google Patents, 1 Espacenet, 7 GitHub, 10 OpenAlex),
  12 snowballing rounds, 17 fetches (12 papers read past the abstract). By area: A
  (FCA) 38, D (agents) 47+, H (plug-in cores, storage) 19, P (patents) 10, V (venue sweeps) 14, S
  (snowballing) 13, B/C/E/F/G/K 4–11 each.
- **Capture–recapture**, across two channels: keyword search (A, incl. patents and GitHub) and
  snowballing (B).
  - Core set: sources covering at least two claim elements, or charted. A found 40, B found 21, and
    5 were found by both.
  - Chapman estimate: (41·22)/6 − 1 ≈ 149 core sources, of which 56 were captured (about 38%).
  - The channels are not independent (snowball seeds came from A), which biases the estimate low.
  - Plain reading: the neighbourhood is much larger than what was read, mostly in self-evolving
    agent memory (85 citers of MemEvolve, all screened by title).
- **Saturation.** The last 31 rows (snowballing and gap searches) added 25 sources and no new
  research line: every new source fell into a line already charted. That is the stopping argument
  for the anticipation question (Garousi Guideline 8). It is weaker for completeness of the
  periphery.

## 11. Not checked

- Espacenet (REST returned 403); patents only through Google Patents' English full text, 5 scripted
  queries and 4 web queries; Chinese, Japanese and Korean literature outside Google Patents (e.g.
  CNKI) not searched.
- n-RCA full text (ScienceDirect 403); "incomplete polyadic RCA" (SSRN 2026) not read.
- ICFCA, CLA, ICCS and CONCEPTS proceedings tables of contents not read page by page (DBLP behind a
  bot check); covered by OpenAlex sweeps instead.
- Grade C only: M*, MemCodex, MINDSET, GrOIL, TypeDB MCP, Neon MCP, Datomic, TerminusDB, AgenticDB,
  Bespoke OLAP, Cyc (glossary and secondary sources), PowerLoom.
- TypeDB's identity representation and constraint annotations; whether Hyperon has a demonstrated
  agent that defines new types.
- FCA4J UI, RCAexplore source, Galicia, conexp-clj's REST API; 22 of the 23 reference clones beyond
  their rows in `reference-rules.md`.
- Wohlin 2014 and Kitchenham 2007 full texts (method summaries only); Voutsadakis 2002 original.
- Your own claude.ai prompts about topology (only Claude's replies were recorded).
- Forward citations of 2026 seeds are near zero partly because they are new; a re-run in 3–6 months
  would see more.

## 12. Sources

Method: [MPEP 2131](https://www.uspto.gov/web/offices/pac/mpep/s2131.html) ·
[MPEP 2143](https://www.bitlaw.com/source/mpep/2143.html) ·
[EPO G-VI 1](https://www.epo.org/en/legal/guidelines-epc/2025/g_vi_1.html) ·
[EPO G-VII 5](https://epo.org/en/legal/guidelines-epc/2025/g_vii_5.html) ·
[PCT ISPE 15.21–28](https://www.wipo.int/pct/en/texts/ispe/15_21_28.html) ·
[Kitchenham & Charters 2007](https://madeyski.e-informatyka.pl/download/Kitchenham07Protocol.pdf) ·
[Wohlin 2014](https://www.ime.usp.br/~gerosa/papers/ESEM___Snowballing.pdf) ·
[Garousi et al. 2019](https://arxiv.org/abs/1707.02553) ·
[PRISMA-S](https://pmc.ncbi.nlm.nih.gov/articles/PMC7839230/) ·
[Kastner et al. 2009](https://experts.mcmaster.ca/scholarly-works/837966)

FCA: [FCA4J](https://ceur-ws.org/Vol-3308/Paper17.pdf) ·
[n-RCA](https://doi.org/10.1016/j.ijar.2026.109782) ·
[Kötters & Eklund 2018](https://ceur-ws.org/Vol-2149/paper7.pdf) ·
[Conjunctive query pattern structures](https://doi.org/10.1016/j.dam.2019.08.019) ·
[TOSCANA / Stumme](https://www.kde.cs.uni-kassel.de/stumme/papers/1998/FODO98.pdf) ·
[Hereth 2002](https://www.mathematik.tu-darmstadt.de/media/mathematik/forschung/preprint/preprints/2208.pdf) ·
[Ferré & Ridoux 2000](https://www.doi.org/10.1007/3-540-44957-4_69) ·
[LISFS](https://www.usenix.org/legacy/events/usenix03/tech/full_papers/padioleau/padioleau_html/) ·
[Graph-FCA](https://hal.ccsd.cnrs.fr/INRIA/hal-01196287v1) ·
[On-demand RCA](https://arxiv.org/abs/1803.07847) ·
[RCA refoundation](https://arxiv.org/abs/2310.06441) ·
[Polyadic notes](https://arxiv.org/abs/2203.14606) ·
[Cimiano et al.](https://arxiv.org/abs/1109.2140) ·
[LLM-assisted RCA](https://hal-lirmm.ccsd.cnrs.fr/lirmm-05249788) ·
[FCA for data lakes](https://arxiv.org/abs/2408.13265) ·
[ContextRAG](https://arxiv.org/abs/2605.19735) ·
[LatWeave](https://arxiv.org/abs/2609.27225)

Stores and categorical: [ACSets](https://arxiv.org/abs/2106.04703) ·
[ACSets.jl API](https://docs.juliahub.com/General/ACSets/stable/api/) ·
[Algebraic databases](https://arxiv.org/abs/1602.03501) · [Ologs](https://arxiv.org/abs/1102.1889) ·
[Anchor modeling](https://www.anchormodeling.com/wp-content/uploads/2011/05/Anchor-Modeling.pdf) ·
[Hyperon](https://arxiv.org/abs/2310.18318) ·
[Reflective metagraph rewriting](https://arxiv.org/abs/2112.08272) ·
[TypeDB hypergraph](https://typedb.com/docs/use-cases/hypergraph/index.md) ·
[TypeDB MCP](https://typedb.com/docs/tools/mcp-server/index.md) ·
[TypeDB + Cursor](https://typedb.com/blog/ai-assisted-typedb-development-with-cursor-and-mcp) ·
[HyperGraphDB](https://www.ime.usp.br/~jmstern/wp-content/uploads/2020/04/Iordanov00.pdf)

Mechanisms: [Modular Materialisation](https://arxiv.org/abs/1811.02304) ·
[Soufflé relations](https://souffle-lang.github.io/relations) ·
[Cyc HL module](https://cyc.com/archives/glossary/hl-module/) ·
[Cyc SBHL](https://cyc.com/archives/glossary/sbhl-module/) ·
[Lenat & Marcus](https://arxiv.org/abs/2308.04445) ·
[PowerLoom](https://www.isi.edu/websites/LOOM/PowerLoom/features.html) ·
[Berkeley DB recovery](https://docs.oracle.com/cd/E17276_01/html/programmer_reference/apprec.html) ·
[PG custom WAL](https://www.postgresql.org/docs/17/custom-rmgr.html) ·
[Datomic audit](https://www.shortcut.com/blog/auditing-with-reified-transactions-in-datomic-f1ea30610285) ·
[TerminusDB commits](https://terminusdb-client.hexdocs.pm/TerminusDB.Commit.html) ·
[Quit Store](https://arxiv.org/abs/1805.03721) ·
[Protégé change management](https://eprints.soton.ac.uk/261013/1/Ontology_Change_Management_in_Protege.pdf) ·
[Component Database Systems](https://www.elsevier.com/books/component-database-systems/dittrich/978-1-55860-642-5) ·
[Bevy ECS](https://docs.rs/bevy_ecs) ·
[CollectionSwitch](https://aip.ifi.uni-heidelberg.de/fileadmin/papers/2018/Collectionswitch_-_CGO2018.pdf) ·
[RelC](https://dspace.mit.edu/handle/1721.1/72442) ·
[Design continuums](https://mail.vldb.org/cidrdb/papers/2019/p143-idreos-cidr19.pdf)

Agents and stores: [EvoOntology](https://arxiv.org/abs/2609.15779) ·
[ALMA](https://arxiv.org/abs/2602.07755) · [MemEvolve](https://arxiv.org/abs/2512.18746) ·
[M*](https://arxiv.org/abs/2604.11811) · [MemCodex](https://arxiv.org/abs/2609.39765) ·
[Harness the Memory](https://arxiv.org/abs/2608.15008) · [OaK](https://arxiv.org/abs/2608.22974) ·
[SpecDB](https://arxiv.org/abs/2605.31097) · [Bespoke OLAP](https://arxiv.org/abs/2603.02001) ·
[GenDB](https://arxiv.org/abs/2603.02081) · [AgenticDB](https://arxiv.org/abs/2606.20318) ·
[Agent-first data systems](https://arxiv.org/abs/2509.00997) ·
[Neon MCP](https://neon.com/docs/ai/neon-mcp-server.md) ·
[MINDSET](https://arxiv.org/abs/2610.08586) · [GrOIL](https://arxiv.org/abs/2608.22135) ·
[SCOPE/SCION](https://arxiv.org/abs/2607.21610) · [Autogenesis](https://arxiv.org/abs/2604.15034) ·
[Git4Data](https://arxiv.org/abs/2609.02106) · [SSKG Hub](https://arxiv.org/abs/2603.00669) ·
[KARMA](https://arxiv.org/abs/2502.06472) · [LLMS4KGOE 2026](https://ceur-ws.org/Vol-4246/) ·
[AutoSchemaKG](https://arxiv.org/abs/2505.23628) · [Ontology-to-tools](https://arxiv.org/abs/2602.03439) ·
[Isabelle textbook agents](https://arxiv.org/abs/2604.15713)

Retrieval and MDL: [StructRAG](https://arxiv.org/abs/2410.08815) ·
[SimGRAG](https://arxiv.org/abs/2412.15272) · [HippoRAG](https://arxiv.org/abs/2405.14831) ·
[HyperGraphRAG](https://arxiv.org/abs/2503.21322) · [HGMem](https://arxiv.org/abs/2512.23959) ·
[GraphRAG-Bench](https://arxiv.org/abs/2506.05690) · [KGist](https://arxiv.org/abs/2003.10412) ·
[SUBDUE](https://arxiv.org/abs/cs/9402102) · [MIDGARD](https://arxiv.org/abs/2405.05189) ·
[Motifs via compression](https://arxiv.org/abs/2104.08163) · [LILO](https://arxiv.org/abs/2310.19791)

Patents and repositories: [US8195712B1](https://patents.google.com/patent/US8195712B1/en) ·
[CN105243081B](https://patents.google.com/patent/CN105243081B/en) ·
[US12073180B2](https://patents.google.com/patent/US12073180B2/en) ·
[US10360506B2](https://patents.google.com/patent/US10360506B2/en) ·
[Topos](https://github.com/Minu476/Topos) · [kogwistar](https://github.com/humblemat810/kogwistar) ·
[librarian-lightrag](https://github.com/a-simeshin/librarian-lightrag) ·
[mdl-fca](https://github.com/petfold/mdl-fca)

Sources of the 2026-10-14 revisions:
[PostgreSQL extensibility](https://www.postgresql.org/docs/current/extend-how.html) ·
[Index access methods](https://www.postgresql.org/docs/current/indexam.html) ·
[Table access methods](https://www.postgresql.org/docs/current/tableam.html) ·
[ALTER TABLE](https://www.postgresql.org/docs/current/sql-altertable.html) ·
[Design of POSTGRES](https://dsf.berkeley.edu/papers/ERL-M85-95.pdf) ·
[pgMemento session info](https://api.pgxn.org/src/pgmemento/pgmemento-0.7.4/doc/pgmemento-docs-0.7.4/User-defined-session-infos.md) ·
[Postgres MCP Pro](https://github.com/crystaldba/postgres-mcp) ·
[Supabase MCP](https://glama.ai/mcp/servers/@supabase-community/mcp-supabase) ·
[Agentic Postgres](https://infoq.com/news/2025/12/agentic-postgres-fast-forking/) ·
[pg_tle](https://github.com/aws/pg_tle/) ·
[LogicBlox](https://www.cs.ox.ac.uk/dan.olteanu/papers/logicblox-sigmod15.pdf) ·
[flecs docs](https://github.com/SanderMertens/flecs/tree/master/docs) ·
[SpacetimeDB LLM upgrade harness](https://github.com/clockworklabs/SpacetimeDB/tree/master/tools/llm-sequential-upgrade) ·
[Convex Chef](https://ship.convex.dev/changelog/chef-ai-app-builder) ·
[DIGIMON](https://arxiv.org/abs/2503.04338)
