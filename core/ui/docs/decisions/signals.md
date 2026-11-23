# Decisions

### BM-D17 · 2026-10 · BM-12
- decision: A notice carries the intent its snackbar action sends (`undo`), not a boolean.
- why: Two kinds of undo would otherwise force a branch in the screen.
- rejected: A boolean `undoable` with the intent hard-coded in the route.
- evidence: `core/ui/.../element/NoticeUi.kt`.

### FM-D13 · 2026-10 · FM-6, ui
- decision: Cross-feature signals that a ViewModel receives go through one app-wide contract in `:core:ui` (a state flow plus an increasing id); values only a screen uses are tied up from ViewModel state by `:app` and handed down.
- why: Both run outer to inner; passing through composition arrives one frame late, which shifts the read-session record time.
- rejected: Passing the signals through composition.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/signal/FeatureSignals.kt`, `app/src/test/java/com/hancekim/mynote/shell/FeatureSignalsTest.kt`.
