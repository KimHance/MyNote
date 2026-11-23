# Decisions

### LN-D6 · 2026-10 · LN-10
- decision: Unsaved text is not saved automatically; only the leave confirmation and rotation or fold survival exist, and the body is not put into state restore.
- why: Stage-1 scope; a body of up to 512KB does not belong in state restore.
- rejected: Autosaving drafts and restoring the body after the app dies.
- evidence: `feature/editor/src/main/java/com/hancekim/mynote/feature/editor/EditorViewModel.kt`, `feature/editor/src/test/java/com/hancekim/mynote/feature/editor/EditorViewModelTest.kt`.

### LN-D7 · 2026-10 · LN-11
- decision: The only safeguard of deleting a device note is one confirmation dialog, with no undo.
- why: Deleting is a thing that has no original to restore.
- rejected: An undo snackbar for device note deletion.
- evidence: `core/data/src/test/java/com/hancekim/mynote/core/data/repository/DeviceNotesTest.kt` (`LN-11 …`), `feature/editor/src/main/java/com/hancekim/mynote/feature/editor/EditorViewModel.kt`.

### DEC-38a · 2026-10 · LN-9, LN-12
- decision: Repository notes are not editable in the app; only device notes are written, edited and deleted.
- why: Editing repository notes would create the push-back conflicts of the later upload stage right now.
- rejected: Editing repository notes in the app.
- evidence: Root AGENTS.md and the local-notes spec (decision 2); sync touches only GitHub-kind sources (LN-12).

### DEC-38b · 2026-10 · LN-3
- decision: The editor is plain Markdown text with a preview that reuses the reader's renderer; there are no formatting buttons, auto-complete or toolbar.
- why: Keeps stage one minimal and reuses the reader.
- rejected: A formatting toolbar.
- evidence: local-notes spec decision 1 and its not-doing table; feature/editor module.
