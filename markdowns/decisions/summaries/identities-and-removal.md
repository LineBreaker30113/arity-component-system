# Identities and removal — who removes an identity, and what happens to what pointed at it?

Quotes of `citable.md` → Identities and removal, written 2026-10-21. Rules: `CLAUDE.md` → Decisions.
"Vacuum" is "compacten" since 2026-10-15; the older entries say "identity" and "index".

1. “Core is the only caller of removal requests (from outside)” · “arity is the one removing” ·
   “arities remove their elements on their own” · “calls the core forget for delegating it”
2. “forgotten (but journalled) the moment an element is removed” · “Forget needs to be undoable” ·
   “must make a "forget" call” · “doesn't shift all the identities”
3. “optimally for memory and time” · “imv values can be removed” · “better off not shifting data, so
   they point at the index”
4. “the index of an identity is converted to identity” · “it auto skips the removed ones” · “would
   not cause any problem” · “transform the identity number before using it”
5. “maybe few other cases I do not remember right now” · “they check if they exist, when necessary”
   · “I have a belief the structures do not need to check”
6. “call ArityComponent's remove” · “that is the recursive step” · “No structure compactens a dead
   identity”
7. “We will not need a function like "swap"” · “more on that later” · “I think we do not need to
   make pending removals”
8. “recording the relation is enough” · “can be done by temporarily storing, then compacten” · “not
   bound to be the only” · “direct storing”
9. “the calculated arities must be dependent on a structure” · “can actually raise errors”
10. “binary search [over the hole list] likely will never get that slow” · “Structures are not
    responsible for the element count”
11. “I think the compacten will be a rare call” · “We do not need to make a "truncate" right now.
    But later we can make this.” · “(may rename to "compress")”
12. “intersect the range with the occupied set once” · “skip the validation of identities, when they
    likely know it” · “exactly when it leaves the structure holding it” · “If checking is requested
    by client”
13. “must allow a way of capturing the lowest positive excluded value” · “if the list is not empty”
14. “out of scope right now” · “planned to be the horizon of undo”
