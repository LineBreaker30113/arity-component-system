# Overview — what has the author decided about ACS?

The top of the summary tree, written 2026-10-21: each line quotes the topic bullets it cites, as
(topic bullet numbers). Read a topic summary next, and the Citable entry a quote comes from last.
Rules: `CLAUDE.md` → Decisions.

- “a database that can be managed with commands” · “Must allow agents at every step” · “nearly 512GB
  text using agents and ACSES” (goal 1, 4, 6)
- “I believe we can represent any system” · “My vision likely always was that we can express any
  database-like system” (vision 1, 2)
- “Arities are the first type of element” · “the second type of elements” · “a third type of
  components, the "procedures"” (architecture 1)
- “1 library, 2 database application, 3 requester, 4 GUI or 4 Agent” · “ACS: The contract
  interfaces” · “ACSES uses it for [the] database service” (layers-and-names 1, 4, 8)
- “they must pass it through the core” · “The ACS doesn't have a core” · “We cannot "insert" arities
  or structures” (architecture 3, 6)
- “must know only a mostly contiguous set of identities” · “Structures use the imv since it is
  stable” · “sets map the iui to imv” (vision 3 · words 4)
- “Core is the only caller of removal requests (from outside)” · “forgotten (but journalled) the
  moment an element is removed” (identities-and-removal 1, 2)
- “the "check and call checkless functions" pattern” · “Cross-cutting concerns come from a journal”
  · “For every request about ACS, it must be able to store a comment” (journal-and-undo 1, 2)
- “acses will use only classes from the database” · “take a target folder and store everything in
  their files” (persistence 3, 4)
- “if and only if (boot is the command and there is not anyone running)” · “Right now we do not make
  access control” (layers-and-names 6, 15)
- “must describe each instance of [a] component” · “essentially a query service” · “the client can
  implement a specific identity set” (descriptions 1 · filtering 1 · identity-sets 2)
- “If it is near nothing, go with AGPL-3.0-only with a commercial licence” · “we code the classes to
  design; later we will add the boilerplate” (licence 2 · process 1)
- “Edits may be slow; reads may not.” · “The engine must be able to express anything”
  (target-use-case 1)
