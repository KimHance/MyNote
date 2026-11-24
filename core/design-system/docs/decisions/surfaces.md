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

### RD-D6 · 2026-09 · design-system:glass-nav, RD-15
- decision: The floating navigation and scroll-to-top button draw a glass surface with the backdrop library (refraction), with the backdrop layer applied only to a box that holds the `NavDisplay`, the surface reading the same backdrop instance, and no drop shadow on the selected pill.
- why: Refraction bends the background near the edges like a lens, which a flat blur cannot; if the backdrop layer sits on a box that also holds the overlays the glass samples itself, and with a shadow the pill flashes dark while its alpha is still near zero. A strong blur erases the detail refraction would move, so the blur is lowered.
- rejected: A frosted-blur library (no refraction); the backdrop layer on the shared-transition layout (the layer ends up empty and the glass vanishes); a drop shadow under the selected pill.
- evidence: `core/design-system` `FloatingNav`, `GlassSurface`; the rule in `core/design-system/AGENTS.md` that only `NavDisplay` sits inside the backdrop box.
