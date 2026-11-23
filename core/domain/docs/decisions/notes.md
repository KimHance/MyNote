# Decisions

### BM-D16 · 2026-10 · BM-3, BM-4
- decision: The added time is the wall clock in epoch milliseconds, ordered newest first with ties broken by note key.
- why: The common `Clock` is monotonic since boot and cannot survive a reboot; the wall clock can move backwards, so ties need a fixed rule.
- rejected: Using the common `Clock`.
- evidence: `AddBookmarkUseCase`, `BookmarkUseCaseTest`, `BookmarkMapperTest`.

### DEC-3 · 2026-10 · domain:notes
- decision: A note key is "<sourceId>/<docId>" and the source id is an opaque id, not repository coordinates. Bookmarks, study records and the note cache all use it. The pre-existing source is "s0", device notes use "local".
- why: Moving the notes repository or adding sources must not change bookmark and record keys.
- rejected: Keys built from owner and repository.
- evidence: NoteKey in core/domain (Legacy = s0, DEVICE_ID = local); the key rewrite ran in the v5 migration.
