---
units: [ui:signal, ui:notices]
plans: [001-reader-foundation, 004-bookmarks, 005-stats, 006-sync, 007-repo-connect, 008-note-template, 011-screen-copy, 012-feature-modules, 013-local-notes]
decisions: [BM-D17, FM-D13]
---
## signal — Feature signals
### Behaviour
- Features never call each other; what is not navigation goes through three singletons here, and moves between screens go by destination key (012-feature-modules: FM-6).
- `FeatureSignals` holds state flows only. Two of them are one-shot requests that carry an id from one shared increasing counter: reader opened (note id, optional resume position, whether the quiz is wanted) and quiz setup request (note id, category) (012-feature-modules: FM-6).
- A receiver treats the id it sees when it is created as already handled, so an old request does not replay when the Activity is recreated and a ViewModel is built again (012-feature-modules: FM-6).
- The other signals are plain values or counts that receivers compare against the value they saw: quiz covers reader (boolean), quiz closes, settings entries, study-log clears, import requests (counts), repo actions locked (boolean).
- Closing the quiz lowers "quiz covers reader" in the same step it bumps the close count, so a reader session that was paused under the quiz restarts on the same tick (005-stats: ST-1).
- A connection change or deleting the note a quiz was made from closes the quiz through this signal (007-repo-connect: RC-23) (013-local-notes: LN-11).
- Home's `가져오기` raises the import request and the editor feature opens its import sheet; home does not depend on the editor (013-local-notes: LN-1, LN-2, LN-6).
- Opening settings first raises the settings-entry count so settings can drop an open token edit, then pushes the Settings key (012-feature-modules: FM-6).
- `LastSyncHolder` starts at "no result". The notes ViewModel that subscribes to sync writes each result and the shell ViewModel resets it on a connection change (succeeded when connected, none otherwise); settings and the list panel only read it, because the sync flow runs again for every subscriber and a second subscription would start a second sync (012-feature-modules: FM-K10).
- `SharedTextInbox` holds at most one received share, in memory only: it is never put in an intent or destination key because a 512KB text can exceed the process-transfer limit (013-local-notes: LN-8).
- A text share carries the text and an optional subject (a name candidate). A file share carries only the file address and an empty text; reading it is left to the taker, off the main thread. A refused share (not a `content://` address, or this app's own provider) carries no address and only lets the taker report the reason (013-local-notes: LN-7, LN-8).
- A new share replaces one that nobody has taken yet; the last share over a share wins (013-local-notes: LN-8).
- `take(id)` returns the share only when the latest one still has that id and empties the inbox; a stale id, an empty inbox, or a lost race gives null, so one share is never opened twice.
- None of the three holders survives process death.
### Public API
- `FeatureSignals` — singleton of the flows above: `readerOpened`, `quizSetup`, `quizCoversReader`, `quizCloses`, `repoActionsLocked`, `settingsEntries`, `studyLogClears`, `importRequests`.
- `FeatureSignals.reportReaderOpened(noteId, resumeAt, quiz)` · `requestQuizSetup(noteId, category)` · `quizCovers(covered)` · `closeQuiz()` · `lockRepoActions(lock)` · `enterSettings()` · `studyLogCleared()` · `requestImport()` — the writers.
- `ReaderOpened` and `QuizSetupRequest` — the two request values, each with an `id`.
- `Navigator.openSettings(signals)` — raises the settings entry and pushes the Settings key.
- `LastSyncHolder` — singleton: `value` flow of `LastSync`, `set(next)`.
- `SharedTextInbox` — singleton: `pending` flow, `put(text, subject)`, `putFile(uri, subject)`, `putRefused(subject)`, `take(id)`; `SharedText` is the value (`text`, `subject`, `id`, `uri`, `refused`).
### Guarantees
- The signal paths between features are fixed by `FeatureSignalsTest` in `app:shell-state`, `settings:settings` and `quiz:session` (names starting `FM-6`).
- The inbox gives one share once and replaces untaken ones, shown through the editor ViewModel (`EditorViewModelTest`); refusing a stale id has no test of its own.
### Not covered
- Tab kinds, the open-reader command and the tab-switch command — `navigation:commands`.
- Who opens the import sheet and reads a shared file — `editor:session`.
- Writing the last sync result (sync subscription) — `notes:state`; showing it — `ui:repo-card` and `settings:repo`.

## notices — Snackbar notices and failure log
### Behaviour
- `NotesMessages` is the one place that chooses snackbar wording for thirteen cases: sync failed, quiz abandoned, token saved, token cleared, disconnect failed, download paused, download cancelled, partial file discarded, model deleted, bookmark added, bookmark removed, study log cleared, sync applied.
- The sync-applied text is `노트를 불러왔습니다 · ` followed by the parts joined with ` · `: `N개 바뀜` when something changed and `M개 지움` when notes were deleted. With neither, the text would end after the separator, so the caller is expected to ask only when something changed (001-reader-foundation: RD-3).
- The sync-failed text is one generic sentence; the kind of failure is shown on the settings repository card, not in the snackbar (006-sync: SY-5) (001-reader-foundation: RD-3).
- A notice is a message, an optional undo and an id. The undo is the intent of the feature that raised it and is sent back to that feature (004-bookmarks: BM-12) (012-feature-modules: FM-16).
- One-shot screen signals are `Error`, `Info`, `Notice`, `QuizUndo` and `ExitHint`. A screen draws a signal once: `unseen(signal, id)` gives the id when it was not shown yet and 0 when the map already holds that id for that signal; kinds are counted separately (012-feature-modules: FM-K11).
- `FailureLog` is the seam ViewModels use to report an unexpected exception with a tag and a message; the Android implementation writes an error line to the system log, with no crash reporting and no retry.
- `AndroidNotesMessages` takes `Application`, not an injected context (`AndroidFailureLog` needs no context), because the graph is built by `app:di` without the Hilt Android plugin here.
### Public API
- `NotesMessages` — interface of the thirteen wording functions; `AndroidNotesMessages` — resource-backed implementation.
- `FailureLog` — functional interface `failed(tag, message, cause)`; `AndroidFailureLog` — logs with `Log.e`.
- `NoticeUi<E>` — `message`, `undo`, `id`.
- `NotesSignal` — the five signal kinds; `unseen` — the extension on `PersistentMap<NotesSignal, Int>`.
### Guarantees
- A shown signal is not drawn again, a new id is drawn even if an older one was shown, and kinds do not interfere (`NotesSignalTest`).
- Every screen string of every module (the `strings.xml` of `:app`, all `core` and `feature` modules, not only the snackbar wording here) ends its sentences with an allowed ending, uses no forbidden expression and none of the dropped terms of the term table except the named frozen names: `notes-tests/test_screen_copy.py` reads all modules' `strings.xml` against the tables of `docs/design/screen-copy.md`, requires at least 390 strings to be found, and fails when an exception name no longer exists, so rewording any string in any unit can turn it red (011-screen-copy: SC-1, SC-2, SC-3) (012-feature-modules: FM-12) (013-local-notes: LN-K6).
- The 56 strings listed in `docs/design/screen-copy-rows.md` must equal that table's after text, and the table must keep the same format slots in before and after and a length column equal to the real change, with no growth in button, label, one-line or tab places; rewording one of them means editing its row in the same change: `notes-tests/test_screen_copy.py` (011-screen-copy: SC-4, SC-5, SC-6).
- The seven app terms the template README quotes (`connect_entry_create`, `connect_link_label`, `connect_action`, `connect_token_title`, `ui_token_howto_kind`, `ui_token_howto_scope`, `ui_token_howto_perm`) must appear verbatim in `public-repos/notes-template/README.md`, and every sentence it quotes as the app's text must occur in some string of any module: `public-repos/tests/test_template.py` and `notes-tests/test_screen_copy.py` find them by name in every module's `strings.xml`, so changing or moving such a string breaks them until the README follows (008-note-template: NT-5) (011-screen-copy: SC-8, SC-K3) (012-feature-modules: FM-12).
### Not covered
- Which snackbar a screen shows and when (home refresh, bookmark undo, quiz quit undo) — `notes:state`, `quiz:session`.
- Binding of the implementations — `app:di`.
### Unconfirmed
- That callers show the sync-applied notice only when something changed; that rule lives in `notes:state`, not here.
