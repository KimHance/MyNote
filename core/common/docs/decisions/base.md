# Decisions

### RD-D1 · 2026-09 · RD-21, common
- decision: Every view model keeps its state as registered values (`trigger` for what it writes, `triggerIn` for streams) and rebuilds the whole state in one `build()` whenever any of them changes; derived values (sorting, filtering) are their own registered values, not computed inside `build()`.
- why: "Why is the screen showing this value" becomes reading one function; scattered `copy()` calls can no longer overwrite each other, and several values changing at once never produce a half-stale state.
- rejected: A Molecule presenter per screen (the first design, 2026-06, replaced by this base in 2026-09); updating pieces of the state with `copy()` from handlers.
- evidence: `core/common` `MviViewModel`, `MviViewModelTest`, `MviViewModelConcurrencyTest`; every `ViewModel` in `feature/*` and `ShellViewModel`.
