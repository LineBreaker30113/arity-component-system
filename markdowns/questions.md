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


## Docs

- **What licence does the repository carry?** There is no `LICENSE` file and no `<licenses>` block in
  `pom.xml` (checked 2026-09-25), so the default is all rights reserved — which sits badly with a
  README that invites people to reimplement the idea in their own language. The choice also decides
  what this project may take in from elsewhere, and what others may do with it.
  - Author: That's a real question, I would like a lisence that is like a bomb and forces every one responsible to open source their code. Research this well and later we can discuss.


## Old plan — confirm, change, or reject

I didn't get few parts but didn't find it worth to remove, so I covered them with `(=(<thing>)=)` likely misses context.
Things added later are in `(!(<thing>)!)`

The settled positions are in `decisions.md`.

#### Author declines


- Bulk over per-element everywhere; structures must also support range reads. (!(What does it even mean?)!)
  - Author: Keep this, out of scope, but if there will be a bulk function there needs to be a reason.


# Open questions

The step before a decision. Each question lists its options and who holds each. Rules: `CLAUDE.md` →
Markdown files.

## Architecture

## Removal and undo

## Journal

## Identity sets and catalogs

## Filtering

## Docs

## Old plan — confirm, change, or reject

Positions of the old `ARCHITECTURE.md`, written largely by Claude.
