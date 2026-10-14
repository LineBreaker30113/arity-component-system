# Identities and removal (2 of 2)

- [For forget before capture-back:] identity set must allow a way of capturing the lowest positive
  excluded value. I mean there is an IntArrayList of excluded [identities]: [its] first element if
  the list is not empty, or just increasing the borders. So the identities set contract is
  responsible.
- We do not need to make a "truncate" right now. But later we can make this. Also, the vacuum (may
  rename to "compress") actually can simply swap the last few entries with the dead ones and
  truncate out the dead ones.
- [Undoing a remove after a vacuum is] out of scope right now; vacuum [is] planned to be the horizon
  of undo.
- [Naming the judgment-call existence method so it looks deliberate, like `identityAtUnchecked`:]
  this naming convention is good, but out of scope; this is boilerplate polishing.
