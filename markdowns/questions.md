# Viewed Questions

Questions viewed by the author that haven't been put to decisions. May have doubts or better naming.
The author has the most authority.

## Identity sets and catalogs


- **In what order do the identity-set changes land?**
  - Claude: write `IdentitiesSetPositiveInt` and `PositiveLong` before deleting the narrow variants,
    since the positive byte and short sets are the only positive ones today.


## Filtering

- **Which data-dependent filters does the engine ship?**
  - Claude: structural leaves — catalog comparisons (`eq`, `lt`, `between`, …) and reachability
    (`childOf`, `descendantOf`, …). Interpreting leaves (`namedLike`) stay the client's.
  - Author: We can ship many leaves, the "equals" for integer like ones or "near" for floating types are cool. Is there a concern with making "String" filtering ways provided by the engine?


- **How to restructure AND / OR / NOT trees for the fastest evaluation?**
  - Author: "Say we give the client the "AND", "OR", "NOT": can we find a way to better restructure
    those claims for the fastest computation just by looking at them, other than the
    "isEnumeratePreferred" workaround in the legacy?"
  - Claude: normalise, plan, execute — push NOT to the leaves, flatten to n-ary, merge ranges on the
    same catalog, order by estimated size and cost, push candidate sets down (needs gather).
  - Author: Your suggestions are cool, please better define them as plan, since they are not urgent.
    I do think we can make few things, like if there is a "Not" inside an "And" the other leave can be enumerated then the ones satisfying not are removed.
    But that is a topic with a lot of things that can be done. One of most important is that we need to make a temporal "isEnumeratePrefered" method that calculates it using recursion on each filter node. That was the former design in the legace, read and bring it to me for us to discuss.


## Old plan — confirm, change, or reject

Things added later are in `(!(<thing>)!)`

The settled positions are in `decisions.md`.

#### Author declines


- Bulk over per-element everywhere; structures must also support range reads. (!(What does it even mean?)!)
  - Author: Keep this, out of scope, but if there will be a bulk function there needs to be a reason.


# Open questions

The step before a decision. Each question lists its options and who holds each. Rules: `CLAUDE.md` →
Markdown files.

## Architecture

- **Which ways of accepting requests may carry the management and loading requests (stop, status,
  loading a folder, loading a class)?** The author has said they must exist, not who may call them.
  - Claude: only the way the requester uses on the same machine. A request that names a path lets
    whoever can send requests run any class file on the disk.
  - Author: "it must have a way to manage it"; "loading folder and class methods must exist".
- **What exactly is a "paragraph of knowledge", and how does a paragraph cite its sources and
  reasoning?**
  - Author: "We can talk about it later." The three-agent pipeline is under Goal in `decisions.md`.

## Identity sets and catalogs

- **How is a calculated arity's size defined, as for the cartesian arity?**
  - Author: "I think we need to find ways to define arity sizes"; "the calculated arities must be
    dependent on a structure".
  - Claude: by how full the product is. Full and fixed: identity = a·|B| + b, size |A|·|B|. Full and
    growing: a fixed stride for B in place of |B|, holes where B is short. Sparse: mint a pair
    identity only when the pair exists, with a ternary structure holding (pair, a, b).

## Journal

- **Should the journal get a "not undoable, can be ignored" step?**
  - Author: "until we had "0" as "Not undoable", and the one left open was "-128", what about we
    adding a "Not undoable, can be ignored."?" … "I still didn't decide on the "Break undo history",
    I just have an suggestion of an improvement."
  - Claude: use -128, the one byte with no inverse: undo and redo skip it, and undo history stays
    intact. About two lines in the journal.

## acses

- **What is acses's device config called, and what does it hold?**
  - Author: "by copying from the device config or equals"; "from device config file (or whatever it
    is called)".
  - Claude: it holds `acse-persistence.jar` and the base classes' jar that a new database starts from.