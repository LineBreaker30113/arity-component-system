# acs/arity: entries to review

# Unreviewed

- A client gets a new record's index by capturing it; `registerIf` is no request. *(Author, 2026-10-20:
  "`registerIf` must go, and client must call capture which returns an index.")* Commons: "Core is the
  only caller of removal requests (from outside)" (acs 16).
  Section: Identities and removal
