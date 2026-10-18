My better wording for the inferences:


The ACS Project:
- Vision of the ACS related projects and the goal, also all the Important ACS projects.
  ACS: The contract interfaces, since they elegantly formalize thing into modules.
  ACSE: Few good implementations of the ACS contracts for solidifying the project.
  ACSES: A client side ACSE implementation for managing ACS based databases. ACSE contains few default implementations.
  ACS-alHikmet: Agents for building and auditing an ACSES database of paragraphs.
  ACSAH viewer: A GUI application like OntoDirectory to analyze the product of ACS-alHikmet.
  OntoDirectory: Where it all started, aimed to be integrated with ACSE instead of ontology package int it's source code, later can be implemented with AI agents like ACS-alHikmet.

# Inferred decisions

Decisions Claude read into the author's words, each quoting its source. Rules: `CLAUDE.md` → Decisions.
Reduced on 2026-10-18 at the author's request ("please reduce them"): done, replaced and duplicated
items were removed; the version before is in Claude's results folder.

# Confirmed

### Handoff — decided, not done yet

Claude's wording unless quoted; facts checked 2026-10-18. *(Author: "The handoffs are things that are
decided to be done and urgent.")* The last five, and the reading below, were confirmed on 2026-10-18.
*(Author: "I read and confirm it.")*

- DAG tests in `acse/src/test/` (the author writes them): encoding transitions, root list after every
  phase, cycle refusal, slot reuse, reduction preserving reachability.
- [The `OE_` prefix, `OntologyEngineCore` and the package `org.halim.ontology`:] now the name is
  "ACS" [now `acse`], replace them with "ACS" [`acse`, except the `OE_` prefix, which becomes `ACS_`:
  see Layers and names, 2026-10-14]. — `OE_` is gone. Done for the live work, 2026-10-18;
  `OntologyEngineCore.java` and `org.halim.ontology` remain in `deprecatedCode/`.
- [The `Module` at the end of `SetHierarchyDirectedAcyclicIntGraphModule`:] replace with
  `Component`. — Still so named, in `structurer_depr/`.
- [Naming the judgment-call existence method so it looks deliberate, like `identityAtUnchecked`:]
  this naming convention is good, but out of scope; this is boilerplate polishing. — Not named yet.
- Test the README's "2^28 files of records … a few GiB" claim; nothing measures it. *(Author: "Record
  it, we need to test it.")*
- Move the decisions into the Citable (`CLAUDE.md` → Decisions): `decisions.md` and its topic files
  joined into `citable.md`, words unchanged; this file joined in, Unreviewed at its end as
  Unconfirmed; summaries cut to about a quarter, plus `summaries/overview.md`; the summary check run.
  Where the handoff goes is the author's call.
- Later: the procedure contract, and more structure components, the cartesian (ternary) and the
  transitions arities among them (`decisions.md` → Architecture).
- A "String" catalog type, a string subversion of the Object one. Its allowed types are spread over
  the type constants of `ACS_ArityComponentBase` (copied in `ACS_ArityCatalogRowBase`), `typeName`,
  both bases' default accessors, and the journaled writes' type check and `bitsOf` in
  `ACSE_ComposableArityCatalog`. *(Author, 2026-10-14: "Right now we have object as a valid catalog
  option, we still need to make a string subversion of it. The problem with it is the allowed types
  of those catalogs are not in one place.")*
- The commercial licence's text, drafted by a lawyer before it is sold, and a contributor agreement
  before the first outside contribution.
- Delete `reference/msgfilter.py` once the author says so; it is gitignored.

### Readings

- "It will not be usable on its own" means no working database comes from `acse` alone, since `acses`
  runs it, but a Java program can still use `acse` directly. *(Author: "(q1 a)".)*

## Unreviewed

Agents append new inferences here (`CLAUDE.md` → Decisions).

- Later, in `acses`: commands that interact with the arities and that query data, and custom class
  files handled properly, with an option mechanism beside a list function. *(Author, 2026-10-18, in
  `plan.md` → P2: "Since the database is really modular, and we made the Composable Arity instance, we
  must implement a way of interacting with the arities. Also, there must be methods of querying
  data."; answering P2: "since we plan allowing custom class files, we need to handle it properly,
  but later. Certainly an option mechanism along side a list function necessary.")*
- The interfaces in `acs` start with `ACS_`, the core, journal, persistence manager and description
  manager included. *(Author, 2026-10-18: "Hereby interfaces in the acs start by ACS_ instead.")*
  Conflicts: `layers-and-names.md` → [The prefixes:] (2026-10-14) would lose "except the core,
  journal and most importantly the persistence manager". Commons: "do it as "OE_ -> ACS_""
  (`decisions.md`). Complements: "ACS: The contract interfaces" (atop this file). `persistence.md`'s
  "make an "ACSE_PersistenceManager" class" keeps the name it had.
