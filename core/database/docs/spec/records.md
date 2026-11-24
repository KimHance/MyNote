---
units: [database:study-log, database:insights, database:bookmarks, database:prefs]
plans: [001-reader-foundation, 004-bookmarks, 005-stats, 007-repo-connect]
decisions: [BM-D4, D-34, D-35, D-58]
---
## study-log — Study records
### Behaviour
- Four tables hold the raw records behind statistics: read sessions (note key, category at record time, start, end, active milliseconds, highest share seen), quiz attempts (note key, category, start, finish, asked, correct), quiz answers and bookmark events (note key, category, kind, time) (005-stats: ST-1, ST-3, ST-4).
- Records point at notes by key text with no foreign key, so records of a removed note stay; no query here checks that the note exists (007-repo-connect: RC-16).
- An attempt and its answers are written in one transaction; the new attempt id is filled into each answer. An answer row is keyed by attempt and position, is deleted with its attempt, and keeps the question, three or four choice cells, the right index and the picked index (005-stats: ST-3).
- Missing choices below the third are stored as empty text; the fourth cell is empty (NULL) when the question has three choices.
- Every attempt row carries the default format `llm`. The store has no input for a format, and `card` is defined but never written here.
- A bookmark event kind is stored as the fixed text `added` or `removed`; a row with any other text is skipped when read. An event insert returns its row id and one event can be deleted by id, which is how an undone removal leaves no event (005-stats: ST-4).
- A window of records is read as three lists: sessions that started, attempts that finished and events that happened in the half-open range from the first to the last time (005-stats: ST-9).
- The latest-session list gives, for each note, the session with the latest end. The attempt list gives all attempts without answers. The first record time is the earliest session start, attempt start or event time, and is empty when nothing is recorded (005-stats: ST-10, ST-11).
- Wrong answers since a time are answers whose picked index differs from the right index, newest attempt first and then by position, up to a limit; each carries note key, category, question, the picked and right choice text and the time. A matching count is also offered. An index outside 0 to 3 reads as empty text (005-stats: ST-16).
- No sums or day, week and month buckets are computed in queries; the domain statistics code (`domain:stats`) does that in the device time zone (005-stats: ST-9).
- Clearing deletes sessions, answers, attempts and bookmark events and also the weekly summaries and quiz patterns, in one transaction, answers before attempts without relying on cascade. A derived sentence never outlives the records it came from (005-stats: ST-6).
- Pruning before a time deletes sessions that started earlier, answers and attempts that finished earlier and events that happened earlier, in one transaction. Answer pruning alone deletes only the answer rows of attempts that finished earlier; the attempt rows stay (005-stats: ST-7).
- The retention periods themselves (365 days for records, 90 days for answer text) are chosen by the caller (`domain:study-log`), not here (005-stats: ST-7).
### Public API
- `StudyLogStoreImpl` — the `StudyLogStore` implementation: insert a session, attempt or bookmark event; delete an event; observe a window, the latest sessions, the attempts and the first record time; wrong answers and their count; clear; prune; prune answers. Its constructor is internal.
### Guarantees
- Kind text is fixed and unknown text is dropped: `BookmarkEventKindColumnTest`.
- Answers are deleted with their attempt and the tables survive migration: `NotesDatabaseMigrationTest`.
### Not covered
- Deciding when a session counts, the 30-second and 60-minute rules and the logging switch — see `domain:study-log`; feeding the tracker from the screens — see `notes:state`.
- Statistics maths — see `domain:stats`; the stats screens — see `stats:stats` and `stats:screen`.
- The summary and pattern tables — see `insights`.
### Unconfirmed
- Window, latest-session, first-record, wrong-answer and prune queries have no direct database test in this module.

## insights — Summary cache and worker status
### Behaviour
- Two tables cache generated text per week: weekly summaries (headline, detail, optional focus) and quiz patterns (one to three tags and a sentence). The key is the week start, a Monday as an epoch day. Writing the same week again replaces it (005-stats: ST-20).
- Every row is stamped with the model commit and the output version it came from. Every read takes the current stamp and sees only rows with that stamp, so sentences from another model or output version never show. The module itself knows no model (005-stats: ST-20).
- The latest summary or pattern is the one with the latest week start, and among equals the latest generation time. A late-finalised last-week summary therefore never beats this week's. The list of summaries is newest week first (005-stats: ST-20, ST-22).
- A pattern keeps at most three tags in fixed cells; more are dropped. A pattern with no tag throws when stored.
- Pruning before a week start deletes older rows from both tables in one transaction; callers run it at start (`app:startup`) and after the worker saves (`ai:insight-worker`) (005-stats: ST-7).
- Worker status lives in its own preferences file with six values: last result (`success`, `failure` or `skipped`), last success time, last failure time, last failure kind (`parse`, `engine` or `timeout`), last skip time and a log epoch. Unknown stored text reads as no value (005-stats: ST-20).
- A success records the result and its time. A failure records the result and kind, and writes the failure time only when the summary failed; a pattern-only failure leaves the failure time alone so the card does not say the summary failed. A skip records result and skip time and never touches failure values, because giving up is not a failure (005-stats: ST-20).
- The log epoch is raised by one, starting from zero, and the new value returned. Clearing the status removes everything except the epoch, so a worker running during a clear can see the changed epoch before saving and drop its result (005-stats: ST-6).
- A damaged status file is replaced by an empty one (001-reader-foundation: RD-22).
### Public API
- `InsightStoreImpl` — the `InsightStore` implementation: observe or list summaries, observe or read the latest pattern, upsert a summary or pattern with a stamp, prune. Its constructor is internal.
- `DataStoreInsightStatusStore` — the `InsightStatusStore` implementation: observe status, mark success, failure or skip, bump epoch, clear.
### Guarantees
- Result and failure-kind text is fixed and unknown text reads as none: `InsightStatusColumnsTest`.
- Tables exist after migration and a same-week upsert overwrites: `NotesDatabaseMigrationTest`.
### Not covered
- Generating summaries — see `ai:insight-generation`; the worker's schedule and give-up rules — see `ai:insight-worker`; what the stamp is — see `ai:insight-worker` and `domain:insights`.
- Showing summaries and the failure line — see `stats:summary`.
- Clearing records, which also clears these tables — see `study-log`.

## bookmarks — Bookmark store
### Behaviour
- Bookmarks live in a device-local preferences file named `bookmarks`: the key is the note key and the value is the time it was added in milliseconds. Nothing is written to any repository, and the file survives restarts (004-bookmarks: BM-2).
- Observing gives a map of key to time and ignores any entry whose value is not a number; it emits only when the map changes (004-bookmarks: BM-2).
- Putting a key that exists overwrites its time. Removing keys that do not exist is ignored, and removing an empty list does nothing (004-bookmarks: BM-3).
- Keys can be counted and removed by prefix, so a source change or disconnect removes only that source's bookmarks and can warn with the count first (004-bookmarks: BM-19) (007-repo-connect: RC-15, RC-17).
- Opening the file migrates old keys that have no `/` to `s0/<key>`. If `s0/<key>` already exists its time wins and the old key is dropped; values that are not numbers and keys that already have a prefix are left alone. The migration can run again safely, so a backup that still holds unprefixed keys restores cleanly.
- The preferences library applies the migration before it emits its first value. If it fails, the file is not changed, the failure comes out of the observation, and the next access tries again; the screen shows no bookmarks for that session and nothing is lost.
- The Room key rewrite and this migration are separate: each is complete and repeatable on its own, so a half-finished upgrade is fixed by the next start.
- A damaged file is replaced by an empty one (001-reader-foundation: RD-22).
### Public API
- `DataStoreBookmarkStore` — the `BookmarkStore` implementation: observe, put, remove, count by prefix, remove by prefix.
### Guarantees
- Prefix, collision, untouched entries and repeat runs of the key migration: `BookmarkKeyMigrationTest`.
### Not covered
- Which bookmarks show, ordering, undo and the screens — see `notes:bookmarks` and `notes:state`; removing bookmarks of deleted or synced notes — see `data:connection` (source change and disconnect, by prefix), `data:device-notes` (device note delete) and `notes:state` (notes a sync deleted).
- Backup inclusion of this file — see `app:build`.
### Unconfirmed
- The store class itself needs a Context and has no test; only the migration is tested.

## prefs — Settings, token and connection files
### Behaviour
- Settings are one preferences file with two values: the text size and the study-log switch. Text size is stored as the fixed text `normal` or `large`, never an enum name, so renaming the enum cannot invalidate saved values (001-reader-foundation: RD-19).
- A missing, empty or unknown stored text size reads as normal and never throws. The study-log switch reads as on when it was never written. Observed values emit only on change (001-reader-foundation: RD-19) (005-stats: ST-5).
- The access token is one value in its own file. Blank text counts as no token on read, and saving null or blank removes the key. The token is plain text in the app-private folder, not encrypted (001-reader-foundation: RD-20).
- Connection state is two files. One holds the used source ids, the unfinished attempt (source id, attempt id, whether a candidate token was used), the disconnect marker, a flag that blocks the debug seed, and the ids whose bookmarks still have to be swept. The other holds only the token that was set aside before a connection attempt (007-repo-connect: RC-6).
- The set-aside token has its own file so that backup rules can leave token files out while still backing up the connection state. A file name change needs the backup rules in `app:build` changed together.
- Saving an attempt writes the set-aside token first, then the attempt, and removes any copy of the token from the state file. Reading an attempt takes the set-aside token from its file and falls back to a copy in the state file; it returns nothing when the source id or attempt id is missing. Clearing an attempt clears both files (007-repo-connect: RC-6).
- Marking a disconnect writes the marker and sets the seed-blocked flag together; clearing the marker alone leaves the flag, and there is a call that clears both. The seed flag keeps the debug seed from reviving a disconnected connection (007-repo-connect: RC-17, RC-19).
- Used source ids only grow, and the bookmark sweep set only grows until it is cleared; the sweep set is what lets a disconnect interrupted by a crash finish at the next start (007-repo-connect: RC-17).
- A damaged file of any of these is replaced by an empty one, so the app opens with defaults (001-reader-foundation: RD-22).
### Public API
- `DataStoreSettingsStore` — the `SettingsStore` implementation: observe and set text size and the study-log switch.
- `DataStoreTokenStore` — the `TokenStore` implementation: current token, save token, observe token.
- `DataStoreConnectionStateStore` — the `ConnectionStateStore` implementation: used ids, attempt, disconnect marker, seed block, bookmark sweep.
### Guarantees
- Text size mapping, defaults and unknown values: `FontScalePreferenceTest`.
### Not covered
- Applying the token to requests — see `network:token`; the token masking and editing — see `settings:repo`; what each connection state means — see `data:connection` and `domain:connection`.
- Encryption at rest: not done; the narrow token interface is where it would be swapped in.
- Backup inclusion rules — see `app:build`.
### Unconfirmed
- The token, connection-state and settings classes need a Context and have no test in this module.
