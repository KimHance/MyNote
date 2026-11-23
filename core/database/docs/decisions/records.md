# Decisions

### BM-D4 · 2026-10 · BM-2
- decision: Bookmarks live in a preferences DataStore file of their own where each key is a note and the value is its added time in milliseconds.
- why: `:core:database` has no serialization plugin, and removing or restoring one bookmark is a single key edit.
- rejected: One JSON string under one key; a Proto DataStore.
- evidence: `DataStoreBookmarkStore`, `BookmarkRepositoryImplTest`.

### D-34 · 2026-10 · database:study-log
- decision: Room entities and DAOs for the study log stay internal to `:core:database`; the store contracts live in `:core:data`; the repository interfaces and models live in `:core:domain`; records and summary caches have separate contracts because they have different writers.
- why: Room types never leave their module, and the log (written often) and the summary cache (written by the worker only) change independently.
- rejected: Placing the entities in `:core:data` as the design handoff suggested; one combined store.
- evidence: `StudyLogStoreImpl`, `InsightStoreImpl`, `StudyLogStore`, `InsightStore`.

### D-35 · 2026-10 · ST-1, ST-3
- decision: One log model in four tables (read sessions, quiz attempts, quiz answers, bookmark events); each row stores its category as it was when written; times are wall-clock milliseconds while durations are measured on the monotonic clock; quiz choices are fixed columns.
- why: Deleted or moved notes must not rewrite past statistics; wall-clock durations drift when the clock changes; the parser guarantees three or four choices so JSON is not needed.
- rejected: Looking categories up at read time; measuring duration by wall clock; storing choices as JSON.
- evidence: `StudyLogEntities.kt`, `ReadSessionTrackerTest`.

### D-51 · 2026-10 · ST-11
- decision: Resuming a stalled note travels as a one-shot request (note, fraction, id) that the shell consumes after the reader is measured, and not inside the destination key.
- why: A position in the key would make the same note a different key and break destination equality.
- rejected: Adding the position to the reader destination key.
- evidence: `FeatureSignals.reportReaderOpened`, `ReaderScrollEffectsTest`, `FeatureSignalsTest`.

### D-58 · 2026-10 · ST-20
- decision: Worker status keeps the last failure time apart from the last give-up time; the card shows a failure only when the failure is newer than the shown summary, and a give-up never changes the screen.
- why: A single "last attempt" pair would let a later give-up overwrite a failure.
- rejected: One last-attempt time and result.
- evidence: `InsightStatusColumns.kt`, `SummaryCardRulesTest`.

### D-65 · 2026-10 · ST-11
- decision: Charts are five Canvas components in the design system, each taking its appearance progress as a function and reading it inside the draw phase; numbered rows and section labels are new design-system components; the bookmark row and label stay private to the bookmarks screen.
- why: A float argument recomposes every frame; the bookmark row has a remove button built in and its label is private, so the handoff's "reuse" did not match the code.
- rejected: A float progress parameter; reusing the bookmark row and label.
- evidence: `core/design-system/.../chart/`, `NumberedRow.kt`, `SectionLabel.kt`.
