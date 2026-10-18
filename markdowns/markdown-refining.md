# Markdown refining

How to refine a markdown file. `CLAUDE.md` holds the fundamentals about the markdowns; this file is
for refining them. Claude uses it when the author wants a refined markdown file.

## The author's rules

- When there are multiple bullet points that talk about the same thing, the agent must compare them
  for Commons, Conflicts, Complements, and Unrelated, then separate them into bullet points while
  asking about conflicts. *(2026-10-14)*
- [Your review time becomes the budget:] that's the point, it [decisions.md] was already 500 lines.
  I would rather it [the agent] forget (at most) 20 percent of important details. *(2026-10-14)*
- I want them [the agents that organize the decisions files] to bring me numbered options for
  workarounds if there are multiple; it makes things way easier. *(2026-10-14)*
- AI must write summaries citing my own words; this is more compact and protects the adherence.
  *(2026-10-18)*

## Method

Claude's wording, from the research of 2026-10-14, written at the author's request ("Can you
implement everything good and related into the refining .md?"). The author's rules above win.
Since 2026-10-18 it no longer cuts the author's words ("we do not cut my words"): it applies to the
summaries and to the Unconfirmed inferences (`CLAUDE.md` → Decisions).

### Steps

1. **Split.** Each entry becomes bullets of one claim each, citing the entry it comes from. Added
   words go in [brackets] and never add a claim. An entry whose reading is unclear stays whole and is
   asked about. *(Claimify; DnDScore)*
2. **Group.** Bullets that answer the same [bracketed question], or share its key terms, form a
   group; pairs are compared only inside a group. Author's words and inferences never share a group.
   *(VeriSpec; CST Bank found plain word overlap enough to find candidate pairs)*
3. **Compare** each pair in a group, then act by label:
   - Commons: keep one existing sentence word for word, the one with more content, citing both
     entries. No new sentence is written. *(Mem0; fused sentences lose faithfulness, Lebanoff 2019)*
   - Complements: the shared part once, each addition its own bullet.
   - Conflicts: asked, both quotes side by side, with numbered options. The model's own preference
     never settles one; models favour the repeated and the recent. *(Conflicting Needles)*
   - Unrelated: nothing.
4. **Set check.** A bullet that follows from the rest of its group taken together is redundant, even
   when no single bullet matches it. Test oldest first, so the newer wording survives. *(Minimal
   cover of functional dependencies; FCA's Duquenne–Guigues basis)*
5. **Dependents.** When a bullet is superseded, list the bullets that use its names or terms and flag
   them, as the "[Since then: …]" notes do by hand. *(Truth maintenance: each belief records what
   justifies it; PACMI)*
6. **Change list.** The output is a numbered list of changes (merge, split, remove, flag), never a
   rewritten file; it is applied item by item. A conflict is answered before its item is applied.
   *(ACE: one full rewrite cut 18,282 tokens of context to 122 and lost accuracy)*

### Questions to the author

- Every question quotes each entry in full, says in plain words what it is about with an example,
  and says what the file keeps and loses under each option. *(The author, on the pilot's questions:
  "This shows me the fact the agents do not give enough detail."; "Once again, I didn't get it.")*

### Runs

- Several independent runs in fresh contexts, at least one on a different model. A label stands
  when most runs agree; otherwise the pair is asked about. *(Kim et al. 2025: one model's runs share
  errors; mixing models helped every debate method in "Stop Overvaluing Multi-Agent Debate")*
- The author: "I want a run on [the] latest Opus, another on the older Opus, and three Sonnet runs
  (each [a] different version), also one Haiku run. [It] must also do some weighing on them. You said
  Opus have cut the most aggressively, I liked that part." *(2026-10-15)*
- The weights and the threshold they must reach are not set by the author yet.

### Checks by code

These cost no review time; they can run as a Claude Code hook after an edit (not set up).

- Every bullet cites an entry; every entry is cited by some bullet. An uncited entry is a dropped
  one, counted exactly.
- Every hedge, negation, scope and time word of an entry survives in its bullets (the list in
  `CLAUDE.md` → The quote test). Losing one changes the meaning, so it is outside the 20 percent.
- Every [bracket] traces to its entry; no file is over 50 lines; entry and line counts are
  reported before and after.

### The 20 percent

- Details lost inside a merge are measured by sampling: the author checks random entries against
  the bullets citing them. No loss in 14 checks, or at most 5 in 50, shows the loss is under
  20 percent at 95% confidence (0.8^14 ≈ 0.044).
- A different model may check every entry instead, the author sampling its verdicts.
- Git keeps every removed entry.

### Sources

[Claimify](https://aclanthology.org/2025.acl-long.348) ·
[DnDScore](https://arxiv.org/html/2412.13175v1) ·
[VeriSpec](https://arxiv.org/abs/2610.01847) ·
[CST Bank](https://aclanthology.org/L04-1239/) ·
[Mem0](https://arxiv.org/html/2504.19413v1) ·
[Lebanoff 2019](https://arxiv.org/pdf/1910.00203) ·
[Conflicting Needles](https://aclanthology.org/2025.emnlp-main.1742/) ·
[Minimal cover](https://personal.utdallas.edu/~muratk/courses/dbslides/functional-dependency.pdf) ·
[FCA bases with constraints](https://peerj.com/articles/cs-1806) ·
[PACMI](https://arxiv.org/abs/2610.05732) ·
[ACE](https://arxiv.org/html/2510.04618v3) ·
[Kim et al. 2025](https://proceedings.mlr.press/v267/kim25e.html) ·
[Stop Overvaluing Multi-Agent Debate](https://arxiv.org/abs/2502.08788)
