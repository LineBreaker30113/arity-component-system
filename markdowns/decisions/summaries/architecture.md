# Architecture — what are the components, and how do they relate to the core?

Quotes of `citable.md` → Architecture, written 2026-10-21. Rules: `CLAUDE.md` → Decisions.

1. “Arities are the first type of element” · “the second type of elements” · “a third type of
   components, the "procedures"” · “close to the system core”
2. “No more "Module"” · “so "Modular" becomes "Composable"” · “plugin based is an architecture we
   left”
3. “they must pass it through the core” · “Right now the core is almost redundant” · “can work on
   their own” · “The ACS doesn't have a core”
4. “I need the modularity and "Logging, Undo-Redo, ..." at the same time” · “creation-addition of
   components, delegations, journal” · “system analytics”
5. “"legacy" is read only” · “should not reach /legacy” · “header can change, no problem; for now we
   can keep renaming”
6. “We cannot "insert" arities or structures” · “can accept it” · “for now ints are enough” · “can
   use object pointers” · “We do not go with that idea” · “must be handled with the core” · “we may
   want to add features”
7. “"different arity" == "different thing"” · “Nothing narrows silently: conversion is exact or
   throws” · “value does not fit” · “Floating narrowing rounds instead”
8. “many Structure Components” · “cartesian [product] of two other arities” · “A ternary example
   would be” · “some transitions” · “The structures ensure their rules themselves”
9. “Procedures may and will change catalog data” · “not hard, just extending the arity operations
   list” · “must allow a way of reasoning, or at least state they do not” · “must be so clear they
   can be automatically reasoned”
10. “must also have contracts about delegation; we can (right now) declare an interface” · “a
    separate file for every top tier interface” · “"identifiers", "arity", "structures.<x>ary"”
11. “we must allow the client to decide to define catalogs without modules” ·
    “"ArityCatalogRowComponent"” · “Not all the catalogs” · “they can be calculations” ·
    “"scattered" is unlikely” · “naming method is not necessary right now”
12. “holds which arities it spans; removal notification and alike” · “Structure contract must allow”
    · “via data streams; the core can manage files”
13. “All of the neurons” · “every weight is placed on arity 2” · “resembles a category” · “a single
    arity structure is always a type of graph”
