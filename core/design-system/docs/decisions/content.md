# Decisions

### D-65 · 2026-10 · design-system:charts, ST-11
- decision: Charts are five Canvas components in the design system, each taking its appearance progress as a function and reading it inside the draw phase; numbered rows and section labels are new design-system components; the bookmark row and label stay private to the bookmarks screen.
- why: A float argument recomposes every frame; the bookmark row has a remove button built in and its label is private, so the handoff's "reuse" did not match the code.
- rejected: A float progress parameter; reusing the bookmark row and label.
- evidence: `core/design-system/.../chart/`, `NumberedRow.kt`, `SectionLabel.kt`.
