---
units: [database:notes, database:schema]
plans: [001-reader-foundation, 006-sync, 007-repo-connect, 009-note-format, 013-local-notes]
decisions: [D-36, NF-D29, DEC-2]
---
## notes — Notes table
### Behaviour
- One table holds the notes of every source: one row per note, keyed by the note key `<source id>/<doc id>`, unique on (source id, doc id). Screens list notes from this table, never from the network (001-reader-foundation: RD-1).
- A row keeps the original text (`raw`), the derived blocks as one JSON cell, the excerpt, the parse version, the content revision, path, group, title, display date, epoch day, first-seen time and fetch time. Body text is not split into tables.
- A second table lists sources: id, kind and connection config. Kind is stored as the fixed strings `github`, `link`, `local`, never as enum names; a row with any other kind string is dropped from source lists without an error.
- The list query returns key, source id, path, group, title, date and blocks JSON. It never reads the original, so a row bigger than the 2 MB cursor window cannot break the list (009-note-format: NF-8).
- Order is epoch day newest first with unreadable dates (NULL) last, then later first-seen first, then title in binary order. The home category and note order is derived from this query (013-local-notes: LN-4).
- Notes of a source whose config contains the pending marker are hidden from the list but still counted by the note count (007-repo-connect: RC-15).
- Promoting a source deletes the replaced sources with their notes and writes the new config in one transaction, so a subscriber never sees old and new notes together (007-repo-connect: RC-15).
- A sync result is applied in one transaction: it first checks that the source row still exists and returns false without writing when it does not; otherwise it upserts and then deletes, so the list emits once (006-sync: SY-4) (007-repo-connect: RC-17).
- Deletes name both source id and doc id and run in chunks of 900 ids (the SQLite bind limit is 999). A source can only delete its own notes; the same doc id under another source survives (006-sync: SY-4).
- An upsert replaces the whole row. The reserved columns (kind, summary, aliases, source URL, tags, warnings) are written NULL by every upsert; nothing fills them today.
- There is no foreign key from notes to sources; ownership is enforced by the queries.
- Deleting a source deletes its notes and its row together and leaves study records alone. Notes with no source row are removed on request (007-repo-connect: RC-16, RC-17).
- Source state per source is the doc id, revision, whether an original is stored and the first-seen time of each note.
- The stale-row query returns rows whose parse version is lower than the given one, whose original is stored and whose original is at most the given UTF-8 byte size. The size is measured in bytes, not characters (009-note-format: NF-8, NF-9).
- Re-derivation writes only blocks JSON, excerpt and parse version, and only on rows whose stored parse version is lower than the new one. Original, revision and first-seen time are untouched, so an edit saved at the current version is never overwritten by an older reading (009-note-format: NF-9) (013-local-notes: LN-9).
- Reading one original for the reader returns null when the note has none or when it is over the given byte size (009-note-format: NF-8).
- The worst row inside the limits (original 512 KB, cache 1 MB, other columns) still fits one cursor window with room to spare (009-note-format: NF-8).
- Inserting a device note creates the device source row (id `local`, kind local, no config) in the same transaction if it is missing; the source row's added time is the first note's first-seen time. Revision and fetch time of a device row are NULL. A duplicate key aborts the insert (013-local-notes: LN-5).
- Editing a device note changes group, title, original, blocks JSON, excerpt and parse version only; key, date, epoch day, first-seen time and path stay. It returns false when the key is not a device note (013-local-notes: LN-9).
- Deleting a device note returns whether a row was removed; a repository note key is never removed this way (013-local-notes: LN-11).
- Loading a device note for editing returns key, group, title, date and original; it returns null for a repository note, a missing original, or an original over the byte size (013-local-notes: LN-9).
- Sync, source delete, promote and orphan clean-up leave device rows in place (013-local-notes: LN-12, LN-K1).
### Public API
- `LocalNotesDataSourceImpl` — the `LocalNotesDataSource` implementation bound by the schema unit: observe notes and sources, load raw, source state, apply sync, stale rows and re-derive, source create/delete/promote/config, and the four device-note calls. Its constructor is internal.
- None other: entities, row types, the DAO and the kind column mapping are internal.
### Guarantees
- A source's sync, delete or promote never removes another source's notes or a device note: `NoteDaoTest`, `DeviceNoteDaoTest`.
- Order, pending hiding, the single emission on promote, the 900-id chunking and the re-derive filter are checked on a real Room database: `NoteDaoTest`.
- A re-derive cannot overwrite an edit saved at the current parse version; an edit keeps date, first-seen time and every other column: `DeviceNoteDaoTest`.
- The worst-case row stays under the cursor window: `NoteRowBudgetTest`, `NoteDaoTest`.
- Kind strings round-trip and unknown strings are dropped: `SourceKindColumnTest`.
### Not covered
- Deciding what to add, update, skip or delete in a sync, and the "no write on transfer failure" rule — see `data`.
- Parsing originals and the parse version number — see `format:note-format`.
- Schema version, migrations and the file name — see `schema`.
- Device-note fields, key and group rules — see `editor` and `domain`.
- Bookmarks of removed notes — see `bookmarks`.
### Unconfirmed
- Whether a future feature that fills the reserved columns will add them to the mapping first; today any upsert erases them.

## schema — Schema and migrations
### Behaviour
- The database file is `notes.db`, schema version 5, with the schema exported as JSON for versions 1 to 5.
- Versions 1→2 (study-record tables), 2→3 (weekly summaries) and 3→4 (quiz patterns) are automatic migrations that only add tables and indexes; notes and records are untouched.
- Version 4→5 is hand-written because it changes keys and converts values. It runs in one transaction; on failure the database stays at version 4.
- 4→5 creates the sources table with one row `s0` (kind github, no config), copies every old note into the new notes table with key `s0/<old id>` and drops the old table. Copying keeps the cache, so notes show offline right after the update.
- A copied row has no original and parse version 0, so the next sync fills the original once. Its revision is the old content hash; its epoch day is computed from the date string and is NULL when the date cannot be read.
- A copied row's first-seen time is the migration time in milliseconds minus the old row id, so notes with the same date keep the order they had before (v4 ordered ties by row id).
- 4→5 prefixes `s0/` on the note id of read sessions, quiz attempts and bookmark events that have no `/` in it, and adds the quiz attempt format column with default `llm`. Quiz answers, summaries and patterns are kept.
- A version 1 database reaches version 5 through the automatic steps and then the manual one.
- There is no destructive fallback: changing an entity and raising the version without a migration crashes when the database is opened. The notes table holds the only copy of device notes, so a rebuild would lose user data (013-local-notes: LN-13).
- Downgrading is unsupported: a build older than version 5 installed over a version 5 database crashes at its first database access.
- The module's Hilt wiring provides the database as a singleton with its three DAOs, and binds the eight store contracts in this module to their implementations.
### Public API
- `DatabaseModule` — Hilt object providing the database and its DAOs; the provider functions are internal.
- `DatabaseBindsModule` — Hilt module binding `LocalNotesDataSource`, `TokenStore`, `ConnectionStateStore`, `SettingsStore`, `BookmarkStore`, `StudyLogStore`, `InsightStore` and `InsightStatusStore`.
### Guarantees
- Each migration step, the v1→v5 path, note and record survival, the `s0` source, the epoch day conversion and the preserved same-date order are checked on a device SQLite: `NotesDatabaseMigrationTest` (instrumented; run with the module's connected test task).
### Not covered
- Backup rules for the database and DataStore files, and the build version code that does not block downgrades — see `app`.
- What the tables mean and how queries behave — see `notes`, `study-log` and `insights`.
