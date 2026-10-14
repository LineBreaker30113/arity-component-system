# Vision

- I believe we can represent any system as data about the objects, the structures on them, and
  procedures about them. ArityComponent is responsible for the data of a group of objects, and the
  identities set is only responsible [for] matching an identity to memory. The structures are
  responsible for relations between identities and have nothing to know about data or memory; if
  [they did], that knowledge would be another arity and the order of the structure would [be]
  higher. There may be many structures that do the same thing but optimized in another way. The
  processes [procedures] are behaviors to change data or structure; they depend on some of the data
  and structure, and when run they change some of the data and some of the structure.
- My vision likely always was that we can express any database-like system (where data exists and
  filtering [is] done based on rules) as a few types of beings and structures across them.
- The structure must know only a dense set of identities each representing a single record, while
  the data about the identities are handled on tables named Arity. Since moving [the] rest of the
  records one block left each time we delete a record is unnecessary, the identities set handles
  identity<->memory mapping, where identity is a single record and memory is the address of memory
  where data related to it [is] stored.
- Some of the records actually can be about other records; those relations are a huge part of all
  the structures.
- Later I realized the addition of procedures would be cool and allow it to express more things,
  like feed forward and gradient descent backpropagation.
- [The NN example:] Two arities, one for (nodes) neurons and the other for the synapses
  (connections/weights). A structure that maps two identities from [the] arity of neurons to a single
  identity from [the] arity of synapses. A procedure that firstly clears every non input neuron's
  value by setting them to the bias (an identity from synapses that has two of the same identity from
  the neurons arity), then for each of the identities on the synapses arity multiplies the first
  neuron's value with the weight and adds it to the second neuron it points [to]; also when all the
  inputs of a neuron [are] calculated, its activation function can be calculated and replace the
  value on the neuron. There are other ways to describe a NN with this architecture; few of them are
  a bit better and more descriptive.
- This architecture can represent easily [a] database or a neural network, which makes me think this
  can represent anything; more importantly [it] does it with high expressiveness (because of the
  contracts), so we can mathematically reason about them.
- My architecture is FCA4J but as [a] database and multi-arity. *(2026-10-10)*
