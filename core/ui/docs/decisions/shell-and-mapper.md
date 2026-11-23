# Decisions

### LN-D14 · 2026-10 · LN-9, LN-11
- decision: A device note's reader top bar carries Edit and Delete and has no refresh; a repository note's reader is unchanged.
- why: Refresh has nothing to do for a device note, and Delete is not hidden inside the editor.
- rejected: Keeping refresh on device notes and putting Delete only inside the editor.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/shell/NotesTopBar.kt`, `app/src/test/java/com/hancekim/mynote/shell/HeaderTest.kt`.
