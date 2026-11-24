# Decisions

### BM-D6 · 2026-10 · BM-12, BM-13
- decision: Removal reaches the store immediately and the list animates the disappearance and re-placement by itself.
- why: Deferring the write for an animation window would let rotation, back or process death leave the screen and the store disagreeing.
- rejected: A screen-local "removing" state for about 180 ms before the write, and ignoring other removals during it.
- evidence: `BookmarksScreen` (row motion), `NotesViewModel` (`RemoveBookmark`).

### BM-D7 · 2026-10 · BM-12
- decision: Undo is valid exactly while the snackbar is visible and only for the last removal.
- why: It matches clearing the token, and restoring is idempotent so a late press is harmless.
- rejected: A separate 3.2 second timer.
- evidence: `NotesViewModel` (`UndoRemoveBookmark`), `NotesViewModelTest` "FM-9 해제한 북마크를 되돌린다".

### D-22 · 2026-10 · BM-9
- decision: The bookmark list in the UI state is a list of `BookmarkUi` (note, category name, added time); the screen reads the first as the large card and the rest as rows.
- why: The card and rows must show the added time, and keeping a precomputed "earlier" list would copy the list on every emission.
- rejected: Separate `featured` and `earlier` fields in the state.
- evidence: `BookmarkMapper.kt`, `BookmarkMapperTest`.

### D-26 · 2026-10 · BM-16
- decision: The compact large-card variant (tighter padding, one-line title, two-line excerpt) is chosen by a low-height window, whatever the column count.
- why: The design assumed landscape phones use two columns, but with the panel open they use one; the card must fit on one screen because of height.
- rejected: Choosing the variant by two-column mode.
- evidence: `FeaturedBookmarkCard.kt` (`featuredVariantFor`).

### D-27 · 2026-10 · BM-9
- decision: Row ordinals and the count beside the "earlier" label are formatted in code, with no string resource.
- why: A number is not wording, and a resource with one placeholder has nothing to translate.
- rejected: A count resource with a placeholder.
- evidence: `BookmarkRow.kt`, `BookmarksScreen.kt`.

### D-30 · 2026-10 · BM-13
- decision: Removing the last bookmark fades the card out, then fades the empty state in (about 180 then 240 ms; instant with reduced motion); the store is still updated at once.
- why: Motion was unspecified in the first round and the owner's design asked for the sequence.
- rejected: The first round's immediate empty state.
- evidence: `BookmarksScreen.kt` (`AnimatedContent` with `rememberReduceMotion`).
