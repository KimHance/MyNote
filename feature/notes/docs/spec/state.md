---
units: [notes:state, notes:destinations]
plans: [001-reader-foundation, 004-bookmarks, 005-stats, 006-sync, 007-repo-connect, 009-note-format, 010-brand, 012-feature-modules, 013-local-notes]
decisions: [BM-D5, BM-D15, DEC-36c]
---
## state — Notes state

### Behaviour
- One view model holds everything this module draws: the note list, search, expand choices, bookmarks, sync state, snackbar notices, reader scroll requests and the reading record. Every screen state is rebuilt whole from these values (001-reader-foundation: RD-21).
- It is the only class that subscribes to the sync use case; the subscription is lazy and survives the screen stopping to collect, so leaving the screen mid-sync neither restarts nor cancels it (006-sync: SY-1) (012-feature-modules: FM-7).
- Nothing is listed until the connection is first known. Each connection change restarts the note list collection; for a confirmed connection the state is "awaiting notes" until that connection's first list arrives, with the previous list kept meanwhile; a disconnect ends the wait (007-repo-connect: RC-21).
- A failing note list or connection flow reads as an empty list or as no connection; a failing bookmark flow reads as no bookmarks.
- Recent notes are every note of every category sorted by date string, newest first, equal dates in category order (001-reader-foundation: RD-5).
- The total count sums all categories and ignores the search (001-reader-foundation: RD-8).
- Search keeps a category when the query, ignoring case, is contained in a note title or in the category name; a category-name hit keeps all of its notes; the body is never read; a blank query shows everything, and the query is not trimmed (001-reader-foundation: RD-9).
- Expand choices are a map by category name; a press flips that name only, whether or not a query is set; the map keeps names of categories that are not in the list, which have no effect (001-reader-foundation: RD-8, RD-9).
- A note gets its excerpt from the domain first-sentence rule, a "readable" flag that is false only when a whole-note box (broken note or over a limit) exists, and a device flag from the key (009-note-format: NF-8) (013-local-notes: LN-5).
- The home body is derived: notes present is Notes; otherwise no confirmed connection is first run; otherwise receiving while the sync runs or notes are awaited; otherwise no notes (013-local-notes: LN-1) (007-repo-connect: RC-21).
- The hero repository line is the confirmed connection's canonical `owner/repo` and nothing without a confirmed connection (010-brand: BR-6, BR-K3).
- The connect band count is the number of repository-origin notes when the connection is known to be absent, else zero; device notes never count, and the band shows only when the count is above zero (007-repo-connect: RC-1).
- A sync success that changed notes after a user refresh raises one info message with the counts; the start run, a refresh with nothing new and a run that `domain:connection` does not mark as announced raise none (a run after a connection or token change is announced only once a refresh has been tapped in this process); a failed run or a failing sync flow raises one generic failure message and the list stays (001-reader-foundation: RD-3) (006-sync: SY-6).
- When a run finishes, the last-sync value is written for settings; if the note being read was deleted by the run, the reader is popped at once, even inside a transition; bookmarks of deleted notes are removed, and a failed clean-up is only logged (001-reader-foundation: RD-4) (004-bookmarks: BM-18) (006-sync: SY-K7).
- Refresh starts a new run and cancels the older one (001-reader-foundation: RD-2).
- Choosing a note opens the reader on top (taking settings' place when settings is on top); new note opens an empty editor on top of the current stack (an editor already in the stack is replaced); Import only raises a request for the import sheet and leaves the stack alone (001-reader-foundation: RD-11) (013-local-notes: LN-2).
- Connect is pushed unless the settings token check has locked repository actions, in which case nothing happens (012-feature-modules: FM-6).
- Moves go through the navigator's throttle: a move inside the throttle window of the last applied move is dropped (001-reader-foundation: RD-16).
- Toggling the bookmark acts on the note on top of the stack and does nothing when no reader is on top; it adds or removes, shows "북마크에 추가했습니다" or "북마크를 해제했습니다" with no undo, clears any pending undo and records an added or removed event (004-bookmarks: BM-1) (005-stats: ST-4).
- Removing a bookmark from the Bookmarks screen ignores an unknown note, removes at once, shows a notice with "되돌리기" and records a removed event; a new notice replaces the older one, so only the last removal can be undone (004-bookmarks: BM-12).
- Undo adds the bookmark back with its original added time; it deletes the recorded removal event instead of adding an event, unless the study log was cleared in between; it does nothing when the note has disappeared or nothing is remembered (004-bookmarks: BM-3, BM-12) (005-stats: ST-4).
- The reading session follows the note on top of the stack, the app being visible and a quiz covering the reader; the session maximum is kept only by the tracker, so a stop and start resumes from the last seen percentage instead of 0 (005-stats: ST-1, ST-2).
- A closed session, a bookmark event and an undo of an event are written under a non-cancellable scope, so one row is finished even if the screen's scope ends right after the app stops (005-stats: ST-1, ST-4).
- A session closed with no category gets the note's current category at that moment (005-stats: ST-1).
- A reader-opened signal from another feature carries an optional resume fraction, which becomes a scroll request, and an optional quiz flag, which raises a quiz setup request unless the note is known to be unreadable (an unknown note counts as readable); a newly created view model ignores a reader-opened signal that existed before it (012-feature-modules: FM-6).
- Notices and error or info signals are shown once: the screen reports the id it showed and the model reads that id as seen, so a recreated screen does not replay a snackbar or an old undo (012-feature-modules: FM-16).
- Times come from injected clocks and texts from an injected messages interface, so the model runs on virtual time in JVM tests (012-feature-modules: FM-8).
- An exception in an intent handler is logged and the state stays as it was (001-reader-foundation: RD-21).

### Public API
- `NotesViewModel` — Hilt view model with `state` and `onIntent`; the shell gets it with the Activity as owner.
- `NotesUiState` — the whole state: lists, bookmarks, query, expand map, sync flags, notice, scroll request, and derived `homeBody`, `homeEntries`, `heroRepoName`, `showConnectBand`, `totalCount`, `isExpanded`, `noteById`, `categoryNameOf`, `isBookmarked`.
- `NotesIntent` — everything a screen can ask: select, foreground changed, reader progress, query, toggle category, open settings, open connect, new note, import, signal shown, switch tab, toggle or remove or undo bookmark, refresh.
- `NoteUi`, `CategoryUi`, `RecentNoteUi`, `BookmarkUi`, `ReaderScrollRequest` — UI models.
- `CategoryLoad`, `settledCategories(connections, categories)` — the settling note list.

### Guarantees
- Undo of a removed bookmark and the study-log event, the injected clock for sessions, the first-run connect path and the editor and import entries are proven by `NotesViewModelTest` (004-bookmarks: BM-12) (013-local-notes: LN-1, LN-2).
- The waiting rule and the connect band count are proven by `CategoryLoadTest` (007-repo-connect: RC-21, RC-1).
- The repository line is proven by `HeroRepoTest` (010-brand: BR-6).
- The device flag, readable flag and excerpt are proven by `NotesUiMapperTest` (013-local-notes: LN-5) (009-note-format: NF-8).
- Only this class takes the sync use case: python test `FM-7 동기화 UseCase 를 받는 클래스는 NotesViewModel 하나다` in `notes-tests/test_feature_modules.py` (012-feature-modules: FM-7).

### Not covered
- How sync fetches and applies notes, and what counts as a change — `data:sync`.
- The reading-session rules (30 seconds minimum, 60 minutes cap) — `domain:study-log`; this unit only feeds the tracker.
- Reader scroll percentage measurement and the foreground observer — `app:shell-chrome` and `app:shell`.
- The snackbar host — `app:shell`; the exit hint and the start decision — `app:shell-state`.
- Backstack ownership and the throttle — `navigation:navigator`; study-log and bookmark storage — `data:records`, `database:study-log` and `database:bookmarks`.
- Bookmark file corruption handling — `database:bookmarks`; here a failing flow only reads as empty.

### Unconfirmed
- Whether the reader scroll position survives going back to an earlier note was not confirmed; the scroll state is created per last opened note id and is not saved across process death.

## destinations — Notes destinations

### Behaviour
- Registers three destinations for the shell: Home, Bookmarks and Reader; every other screen of this module (list panel) is called by the shell directly.
- Each destination reads the notes state inside its own lambda, so a change in another feature's state does not redraw it.
- Home gets the recent notes, the body, the entry buttons, the repository line and the bookmarked ids, plus the shell's paddings and grid state; the connect band is passed only when the window uses the modal drawer (007-repo-connect: RC-1).
- Bookmarks gets the bookmark list, the two-column decision, the paddings and the list state from the shell; its loading flag is the sync running with no notes known (004-bookmarks: BM-14, BM-16, BM-21).
- Reader looks the note up by id and draws nothing when it is missing; it receives the category name and the reader-end content as a slot built from the note id (001-reader-foundation: RD-12).
- The Bookmarks and Reader destinations take the frame's scene-edge modifier; Home does not.

### Public API
- `notesDestinations(state, onIntent, readerEndCta)` — the registration the shell calls; `readerEndCta(noteId)` is the slot the shell fills with the quiz card.

### Not covered
- The quiz card and its view model — `quiz:cta` and `quiz:session`; this module never depends on them (012-feature-modules: FM-2).
- Scene strategy and transitions — `navigation:scenes`; keys — `navigation:keys`.
- Editor and paste registrations — `editor:editor-screen`.
