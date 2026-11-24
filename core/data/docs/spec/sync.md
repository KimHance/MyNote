---
units: [data:contracts, data:sync]
plans: [001-reader-foundation, 004-bookmarks, 006-sync, 007-repo-connect, 008-note-template, 009-note-format, 013-local-notes]
decisions: [SY-D1, SY-D2, SY-D4, SY-D6, NT-D28, DEC-1, DEC-25, DEC-36b]
---
## contracts — Data-source contracts
### Behaviour
- The module owns every interface its outer modules implement: `NoteSourceAdapter` (implemented by `network:contents`), `LocalNotesDataSource` (`database:notes`), `BookmarkStore` (`database:bookmarks`), `ConnectionStateStore`, `TokenStore`, `SettingsStore` (`database:prefs`), `StudyLogStore` (`database:study-log`), `InsightStore` and `InsightStatusStore` (`database:insights`).
- No Room entity, DAO, Retrofit or OkHttp type crosses this boundary; remote data arrives as `SourceEntry` and the original text as a plain string.
- A note source adapter has a kind, lists a whole source as entries (doc id, path, revision, group, optional title and date) and fetches one entry's original as an unparsed string; it may throw, and the caller treats any throw as a transfer failure.
- A note key is `<source id>/<doc id>`; the first repository source is `s0`, the device source is `local`, a source created by a connection has a generated id.
- The local notes source promises: reading an original returns nothing when the original is absent or over the given byte limit; the list projection never carries originals.
- It applies one source's sync in one transaction and only while that source row still exists, reporting whether it wrote anything (007-repo-connect: RC-17).
- It reads per source only that source's state (doc id to revision, whether an original is stored, first-seen time), so deletion candidates never come from another source (006-sync: SY-4).
- It lists stale rows (parse version below the given one, original stored and within the byte limit) and rewrites only their derived columns (009-note-format: NF-9).
- It deletes notes whose source row is missing, and promotes a pending source while deleting the replaced sources' notes and rows in one transaction (007-repo-connect: RC-15).
- Device-note writes, edits, deletes and edit reads touch only rows of the device source; inserting a device note creates the device source row in the same transaction when it is missing (013-local-notes: LN-12).
- The bookmark store maps note keys to added time and can count and remove by key prefix (`<source id>/`); the connection state store keeps used source ids, the attempt record, the disconnect marker, the seed block and the bookmark sweep list.
- `IndexNoteDto` is the list file's item shape: id, title, date, path, hash, all strings and all required (009-note-format: NF-K2).
- `NotesJson` ignores unknown keys and coerces invalid values to defaults.
### Public API
- `NoteSourceAdapter`, `SourceRecord`, `SourceEntry` — the remote note source seam and its types.
- `LocalNotesDataSource` — the local notes seam; read types `ListedNote`, `StaleNote`, `CachedNoteState`, `DeviceNoteRow`; write types `CachedNote`, `RederivedNote`, `DeviceNoteInsert`, `DeviceNoteEdit`.
- `BookmarkStore`, `TokenStore`, `SettingsStore`, `StudyLogStore`, `InsightStore`, `InsightStatusStore` — store seams.
- `ConnectionStateStore`, `AttemptRecord`, `ConnectionIdGenerator` — connection bookkeeping seam and the source id generator.
- `ContentNoteFileReader` — public, but described under `device-notes`.
- `IndexNoteDto` — list file item; `NotesJson` — the shared `Json` instance.
### Not covered
- `CachedNote` has no fields for the unused placeholder columns of the notes table (kind, summary, aliases, source URL, tags, warnings); a feature that fills them adds them here first — the table side is `database:notes`.
- Implementations of these interfaces — `network:contents`, `database:notes`, `database:bookmarks`, `database:prefs`, `database:study-log` and `database:insights`.
- Order of notes and categories — it comes from the query of `database:notes` (date, then first-seen time, then title).
- Block DTOs and the block cache — `format:note-format`; parse rules — `format:markdown` and `format:json-format`.

## sync — Repository-note sync
### Behaviour
- The note list is the cache grouped by group name in the order the cache query returns; this unit sorts nothing (001-reader-foundation: RD-1).
- Every cache emission decodes all notes' blocks again, off the main thread; a note's id is its key and a missing date reads as an empty string.
- The list never reads originals; an original is read one row at a time only for a whole-note box or an unreadable block cache, and only when it is within 1 MB, else the box has no original (009-note-format: NF-8).
- An unreadable block cache falls back to parsing the stored original by the row's path; with no original the note stays listed as a broken note.
- A sync first waits for connection start recovery, then re-derives stale rows, then reconciles each syncable source in turn and sums the outcomes.
- Re-deriving takes rows whose parse version is below the current one and whose original is within 1 MB, parses original and path again and rewrites only the derived columns, with no network request (009-note-format: NF-9, NF-K7).
- Re-deriving is committed on its own before any request, so it applies offline and even when the later reconcile fails; a row that fails to parse is stored as a box at the new version (009-note-format: NF-9).
- Re-deriving covers every source, device notes included (013-local-notes: LN-14).
- A GitHub source is synced only when its stored connection decodes and is not pending; another kind is synced only when an adapter of that kind exists, so device (`local`) and link sources are neither synced nor deleted (006-sync: SY-K6) (013-local-notes: LN-12).
- Without a confirmed connection a sync sends no request, raises no error, creates no source row and returns an empty outcome (006-sync: SY-3) (013-local-notes: LN-K2).
- Reconciling one source lists it, then loads that source's local state only (006-sync: SY-4).
- An empty list while that source still has local notes fails the sync and deletes nothing; an empty list with no local notes of that source succeeds (006-sync: SY-K3).
- A body is fetched when the entry is new, its revision (the list hash) differs, or the cached row has no stored original; an equal revision with an original is not fetched (006-sync: SY-K4, SY-K5).
- The app compares hashes only; it never computes them.
- Bodies are fetched concurrently (up to 16 at once) and any single failed transfer fails the whole source with nothing written (006-sync: SY-K2) (009-note-format: NF-K1).
- Each fetched original becomes blocks and an excerpt through the format entry point (`format:note-format`) with the entry's path; parsing never throws, so a broken note is stored as a box and the rest are applied (006-sync: SY-K1) (009-note-format: NF-2, NF-K1) (013-local-notes: LN-K3).
- A stored row keeps the original, the path, the revision, the fetch time, the current parse version, the title (the doc id when the list has none) and the date with its epoch day.
- The epoch day is read from a date starting with `YYYY.MM.DD` or `YYYY-MM-DD` that is a real day; anything else gives none.
- The first-seen time is one clock reading per source sync shared by all new notes, and is never changed by an update or an original refill.
- Deletion candidates are that source's local doc ids missing from the list; with nothing to write no write is made, else one transaction writes upserts and deletes (006-sync: SY-4).
- If the source row disappeared meanwhile (disconnect or replacement), nothing is written and the source contributes an empty outcome, so a running sync never revives removed notes (007-repo-connect: RC-17).
- The outcome counts added (not cached before) and updated (revision changed) notes and lists deleted note keys; refilling an original and re-deriving are not counted (006-sync: SY-K5) (009-note-format: NF-9, NF-K6).
- Any failure, including a failed local write, comes back as a failed result and is never thrown; cancellation passes through and is never reported as a failure.
- A failure kind thrown by the adapter reaches the caller unchanged and leaves that source's notes unchanged (006-sync: SY-5).
- Parsing and encoding run on a background dispatcher, never on the caller's main thread.
### Public API
- `NotesRepositoryImpl` — domain `NotesRepository`: `observeCategories()` (the cached note list as categories) and `sync()` (a `Result` of `SyncOutcome`).
### Guarantees
- Add, skip, update, delete, own-source deletion, delete protection, unknown-kind sources, format isolation, raw refill not counted, first-seen time, epoch day, excerpt, broken cache fallback and concurrent fetch (a gated fake adapter times out if fetching becomes sequential): `NotesReconcileTest`.
- Format chosen by path in every place an original becomes blocks, over-limit and failed parses boxing one note only, JSON rows unchanged by re-deriving: `NoteFormatSyncTest`.
- With the real Markdown engine, a large in-limit note does not stop sync, an old boxed Markdown row is rebuilt offline, and moving the 22 repository notes to `.md` syncs as 0 added, 0 deleted, 22 updated with bookmarks kept: `MarkdownSyncTest` (009-note-format: NF-16).
- Adapter failure kinds pass through unchanged: `SyncFailureKindTest`.
- The template's example notes are received in full by this sync and read with no demoted block, no box, every drawn shape used and no text-only syntax: `TemplateNotesTest`, which reads the template folder as a test input, so a change there reruns it (008-note-template: NT-6) (009-note-format: NF-K5).
### Not covered
- When sync runs (app start, refresh, applied-token change) and cancelling the older run — `notes:state` (its view model) and `domain:connection` (the sync runs).
- The connect flow's first sync and confirmation — `connection`.
- Snackbar wording — `ui:notices`; showing the failure kind — `settings:repo`.
- Request building and mapping HTTP errors to kinds — `network:contents`.
- No automatic retry, no periodic background sync, no conditional requests, no several repositories at once, no folder walking without a list file.
- Removing bookmarks of notes a sync deleted — the view model of `notes:state` does it from `deleted`, not this unit (004-bookmarks: BM-18).
