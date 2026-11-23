---
units: [domain:notes, domain:excerpt, domain:bookmarks]
plans: [004-bookmarks, 006-sync, 007-repo-connect, 009-note-format, 013-local-notes]
decisions: [BM-D16, DEC-3]
---
## notes — Notes and sync
### Behaviour
- A category is a name plus its notes; screens observe one list of categories, which is the cached tree, never the network.
- A note id is the text form of a note key, `<sourceId>/<docId>`. Bookmarks, study records, deleted-note lists and navigation all use that string, and consumers treat it as opaque (013-local-notes: LN-5).
- Repository notes of the first generation keep the source id `s0` (for example `s0/graph`); device notes use the source id `local`, which never collides with `s0`, has no `/`, and is the prefix of every stored bookmark and study-record key for device notes (013-local-notes: LN-5).
- The `s0` id carries no repository address, so moving the notes repository does not change any key.
- Parsing a key cuts at the first `/`; the source part never holds a `/`, the document part may. A key with no `/`, an empty source part or an empty document part parses to nothing.
- A note's origin comes from its key prefix: prefix `local` is Device, everything else (including an id that does not parse) is Repository. Only Device notes are editable; repository notes are read-only in the app (013-local-notes: LN-5).
- The connect-hint band counts repository notes only while the connection is known to be absent; with a connection, or while the state is still unknown, the count is 0, and device notes are never counted (007-repo-connect: RC-1).
- A note body is a list of blocks of ten kinds: heading, paragraph, code, quote, bullet list, ordered list, table, divider, spacer and unsupported. Inline text is spans with a style (text, bold, code) plus italic and strike flags; the converter gives code spans the strike flag only.
- List items may be checkboxes (checked, unchecked or not a checkbox) and may hold child blocks; an ordered list has a start number; a table has header cells, body rows and a per-column alignment.
- An unsupported block is an app-internal box with a reason, the salvaged text (may be absent) and the original (empty means no original). Reasons are unknown type, broken block, broken note and limit exceeded; the last two stand for the whole note, and a whole-note box is the note's only block.
- The only place that holds note limits is the limits value: container nesting 32, original 512 KB (524,288 bytes), table cells 4,096 per note, cache column 1 MB (1,048,576 bytes). A note over a limit becomes a limit-exceeded box (009-note-format: NF-8).
- Changing a limit does not re-judge notes already cached; they keep the old verdict until the next re-conversion in `format`.
- A sync outcome tells how many notes were added and updated, and lists the keys (not a count) of deleted notes; it has changes when any of the three is non-empty. Deleted keys let screens close a deleted note and drop its bookmarks.
- Sync failures are six distinct types: offline, token rejected, access denied, rate limited, server and list format. Their messages are English diagnostics, not screen text, and screens branch on the type (006-sync: SY-5).
- Reading the categories and running a sync are one-line pass-throughs: no ordering, filtering, retry or scheduling is added here.
### Public API
- `Category`, `Note` (`origin`), `NoteOrigin` (`editable`, `of(noteId)`), `List<Category>.connectBandNoteCount(connection)` — the note tree and the band count.
- `NoteBlock` (ten kinds), `NoteListItem`, `TableCell`, `TableAlign`, `NoteSpan`, `SpanStyle`, `UnsupportedReason` (`wholeNote`) — the block model.
- `NoteKey` (`value`, `parse`), `SourceId` (`Legacy`, `Device`, `DEVICE_ID`), `SourceKind` (GitHub, Link, Local) — keys and sources.
- `NoteLimits` (`Default`, `maxBytes`, `maxCacheBytes`) — the limit values.
- `SyncOutcome` (`changed`, `hasChanges`) and `SyncFailure` with `Offline`, `TokenRejected`, `AccessDenied`, `RateLimited`, `Server`, `Format`.
- `NotesRepository` — `observeCategories()` and `sync()`; implemented in `data`.
- `GetCategoriesUseCase`, `SyncNotesUseCase` — the two pass-throughs.
### Guarantees
- Key parsing and the device source id are covered by `NoteKeyTest`; the limit values and which reasons stand for a whole note by `NoteLimitsTest`; the origin rule by `DeviceNoteTest`.
- The six failure kinds are distinct types (`ConnectionUseCaseTest`).
### Not covered
- Reading or writing the note cache, the sync algorithm and the diff rules (which notes change or are deleted) — `data` and `database`.
- Turning Markdown or JSON into blocks, and the parse version — `format`.
- When a sync runs and what it announces — unit `connection` (sync runs).
- Drawing blocks, the box wording and the limit text — `design-system`, the notes feature module.

## excerpt — First sentence
### Behaviour
- The first sentence comes from the first top-level paragraph that has any non-blank text; headings, code, quotes, tables and dividers are never candidates (009-note-format: NF-10).
- With no such paragraph, the first list item with non-blank text is used, walking bullet and ordered lists in document order: an item, then its nested list items, then the next item. A paragraph inside an item is not a candidate (009-note-format: NF-10).
- An item with empty text is skipped, so its nested items are looked at before the next item (009-note-format: NF-10).
- Spans are joined as plain text: bold, italic, strike and checkbox marks never appear, and every run of whitespace becomes one space with the ends trimmed (009-note-format: NF-10).
- The sentence ends at the first `.`, `?`, `!` or `。` that is followed by a space or the end of the text; with no end found the whole paragraph is used.
- A sentence-end mark is not an end inside a code span; a `.` is also not an end between two digits or after `e.g.`, `i.e.`, `etc.` or `vs.` (case ignored).
- A sentence longer than the limit (140 characters unless the caller passes another) is cut at the last space inside the limit and ends with an ellipsis; with no space it is cut at the limit and gets the ellipsis.
- A blank result, or no candidate at all, gives none; there is no fallback to the title.
- Available on a note and on a plain block list, with the same result for both (004-bookmarks: BM-10).
### Public API
- `Note.firstSentence(maxChars)` — the sentence of a note, or null.
- `List<NoteBlock>.firstSentence(maxChars)` — the same for a block list.
### Guarantees
- Candidate order, the sentence-end rules, the 140-character cut and the nested-item order are covered by `NoteExcerptTest`.
### Not covered
- Where the sentence shows (bookmark card, list rows) and storing it on cached rows — the notes feature module and `data`.
- The plain text built for quiz prompts, which keeps strike marks — unit `quiz`.

## bookmarks — Bookmarks
### Behaviour
- A bookmark is a note key plus the time it was added in epoch milliseconds.
- Adding takes its time from the clock unless the caller gives one; undo passes the original time to restore it (004-bookmarks: BM-3).
- Adding a key that already exists overwrites its time (004-bookmarks: BM-3).
- Removing takes any number of keys in one call; keys that are not bookmarked are ignored (004-bookmarks: BM-3).
- Observing hands back the repository's list as it is; the order (newest first, ties by key) and hiding bookmarks of notes missing from the cache are done by the screen, not here.
- No study-log event is written here; the caller records added and removed events through unit `study-log`.
### Public API
- `Bookmark` — note id and addedAt.
- `BookmarkRepository` — `observeBookmarks()`, `add`, `remove`; implemented in `data`.
- `AddBookmarkUseCase`, `RemoveBookmarksUseCase`, `ObserveBookmarksUseCase`.
### Guarantees
- The clock default, caller-supplied time, overwrite and multi-key removal are covered by `BookmarkUseCaseTest` (against a fake repository, so the overwrite itself is the repository's contract).
### Not covered
- Storing bookmarks and removing them with a deleted note or a disconnected source — `data` and `database`.
- The bookmarks screen, undo and ribbons — the notes feature module.
