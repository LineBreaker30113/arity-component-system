# Vision — summary

Claude's summary of `decisions.md` → Vision; written 2026-10-18. A reading aid, not a source: the
decision files win.
Each bullet is Claude's claim; the line under it quotes the author's own words it rests on.

- The author believes any system can be represented as data about objects, structures on them, and
  procedures about them.
  > “I believe we can represent any system”
- ArityComponent holds a group's data; the identities set's primary goal is matching an identity to
  memory, and it also handles set operations (2026-10-18).
  > “Primary goal of the Identities set is to match an identity to memory” ·
  > “while being also responsible for set operations”
- Structures hold relations and have nothing to know about data; knowing it would make another arity
  and a higher order.
  > “have nothing to know about data or record data” ·
  > “the order of the structure would [be] higher”
- Many structures may do the same job, each optimized another way.
  > “optimized in another way”
- Procedures change data or structure, depending on and changing some of each.
  > “behaviors to change data or structure”
- Likely the vision all along: any database-like system (data, rule-based filtering) can be
  expressed as a few types of beings and structures.
  > “My vision likely always was” · “any database-like system”
- A structure must know only a mostly contiguous set of identities, one per record; arities hold the
  data.
  > “must know only a mostly contiguous set of identities” · “tables named Arity”
- Records are not shifted on delete; the identities set maps identity to its data's memory address.
  > “one block left each time we delete a record is unnecessary” · “identity<->memory mapping”
- Records can be about other records; such relations are a huge part of all the structures.
  > “can be about other records”
- Procedures came later, to express more, like feed forward and backpropagation.
  > “Later I realized the addition of procedures”
- NN example: neurons and synapses are two arities; a procedure sets every non-input neuron to the
  bias (a synapse pairing a neuron with itself), adds weight times value along each synapse, and
  when all inputs are calculated the activation can
  replace the value; a few other encodings are better.
  > “one for (nodes) neurons and the other for the synapses” · “setting them to the bias” ·
  > “multiplies the first neuron's value with the weight” ·
  > “its activation function can be calculated” · “few of them are a bit better”
- It can represent databases and NNs easily, which suggests it can represent anything; the contracts
  give expressiveness, so we can reason mathematically.
  > “can represent anything” · “because of the contracts”
- FCA4J, but as a database and multi-arity (2026-10-10).
  > “FCA4J but as [a] database and multi-arity”
