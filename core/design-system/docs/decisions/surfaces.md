# Decisions

### D-18 · 2026-10 · BM-5
- decision: `NoteCard` keeps its trailing slot and Home passes none; without a slot no spacer or 12dp gap is drawn.
- why: Removing the slot changes the public design-system API for nothing and keeps the glyph-versus-ribbon choice a one-line switch.
- rejected: Deleting the trailing slot.
- evidence: `core/design-system/.../component/NoteCard.kt`.

### D-19 · 2026-10 · BM-5
- decision: The ribbon is not a shared element between the home card and the reader.
- why: The reader header has no counterpart and an element sticking 1dp outside the clip flickers at the boundary during the transition.
- rejected: Moving the ribbon with the shared-element transition.
- evidence: `BookmarkRibbon.kt`.
