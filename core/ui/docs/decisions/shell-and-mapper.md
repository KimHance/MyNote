# Decisions

### LN-D14 · 2026-10 · LN-9, LN-11
- decision: A device note's reader top bar carries Edit and Delete and has no refresh; a repository note's reader is unchanged.
- why: Refresh has nothing to do for a device note, and Delete is not hidden inside the editor.
- rejected: Keeping refresh on device notes and putting Delete only inside the editor.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/shell/NotesTopBar.kt`, `app/src/test/java/com/hancekim/mynote/shell/HeaderTest.kt`.

### BM-D13 · 2026-10 · ui:shell, BM-21
- decision: The shell keeps Bookmarks scroll state like Home, so switching tabs does not reset it to the top.
- why: The shell already owns per-destination scroll state; a different rule for one screen has no reason.
- rejected: Resetting to the top on each tab switch.
- evidence: `core/ui/.../shell/NotesScrollState.kt` (`bookmarksList`).

### D-25 · 2026-10 · ui:shell, BM-21
- decision: One list state is shared by the one-column list and the right column of the two-column layout.
- why: Two states would split the "scroll to top" decision; the grid state of the first round is gone.
- rejected: A grid state plus a separate list state.
- evidence: `NotesScrollState.bookmarksList`.
