---
units: [data:records, data:data]
plans: [004-bookmarks]
decisions: []
---
## records — Pass-through repositories
### Behaviour
- Bookmarks, study log, insights, settings and token repositories only delegate to their stores; they add no rule, cache or thread switch of their own.
- Bookmarks come out as the store's entries (note key and added time) in the store's order, unsorted; adding writes the key with its added time, overwriting an existing one (004-bookmarks: BM-2, BM-3).
- Removing bookmarks takes a collection of keys; keys that are not bookmarked are ignored (004-bookmarks: BM-3).
- Note facts for statistics combine the latest reading session per note with the quiz attempt summaries into one stream.
- The insight status read once is the status stream's current value.
- The token repository returns and observes the stored token, not the token applied to requests (the applied one is `network:token`).
### Public API
- `BookmarkRepositoryImpl` — domain `BookmarkRepository`: observe, add, remove.
- `StudyLogRepositoryImpl` — domain `StudyLogRepository`: record sessions, attempts and bookmark events, delete a bookmark event, observe windows, note facts and first record time, wrong answers, clear and prune.
- `InsightRepositoryImpl` — domain `InsightRepository`: weekly summaries, quiz patterns and worker status, save, mark, bump epoch, clear, prune.
- `SettingsRepositoryImpl` — domain `SettingsRepository`: text size and the study-log switch.
- `TokenRepositoryImpl` — domain `TokenRepository`: current, save and observe the stored token.
### Guarantees
- The store's map becomes bookmarks unsorted, add writes the key with its time, a sync's deleted keys remove only those entries: `BookmarkRepositoryImplTest`.
### Not covered
- Storage, file names, backup rules, retention numbers and corrupt-file handling — the `database` stores.
- Bookmark ordering and hiding bookmarks without a note — domain and `notes`.
- Recording rules (when a session counts, the logging switch gate) — domain and `stats`.

## data — Data module root
### Behaviour
- Binds the domain repositories to this module's implementations, all as singletons: notes, connection, device notes, token, settings, bookmarks, study log, insights; the file reader is bound unscoped.
- Provides the one `Json` instance of the graph (unknown keys ignored, invalid values coerced); only `network`'s converters inject it, and a second `Json` provider anywhere breaks the build with a duplicate binding.
- Provides the device note text rules (the Markdown rules of `format`) and a UUID source id generator.
- `NoteFormat` has no binding here; it is constructor-injected, and tests pass a fake converter through the internal constructors.
- Implementations of the data-source interfaces are bound by `network` and `database`; the full graph exists only in `app`.
- Shared test fakes (adapter with failure injection and a concurrency gate, local source, stores) live with the tests of this module.
### Public API
- `DataModule` — Hilt module with the bindings above.
### Not covered
- Bindings of stores and the adapter — `database`, `network`.
- Starting token sync — `app`.
