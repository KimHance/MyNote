# Bookmarks

Source: rebuilt from commit 3c94b45c (2026-10-01 work, committed 2026-10-06) and the owner's earlier planning notes (untracked), checked against the code as of commit 06e0ad15.

## Plan
- why: Let the owner keep a short personal list of notes worth coming back to, on this device only, without folders, tags or memos.
- flow: While reading a note the owner taps the bookmark toggle in the top bar; the note then shows a ribbon on its home card and a glyph in the list panel, and the Bookmarks tab lists it newest first (latest as a big card, the rest as numbered rows); a remove button on the card or row takes it out and a snackbar offers undo.
- not doing: No folders, tags, memos or ordering by hand; no export, no writing to the notes repository, no sync between devices; no undo for the toggle in the reader (pressing it again is the undo); no haptics.

## Items
| ID | What | Accept |
|---|---|---|
| BM-1 | The reader top bar has a bookmark toggle (checkbox semantics, on and off state descriptions) that adds or removes the open note; a message appears with no undo action; rapid presses count once | Toggling twice on an open note adds then removes the bookmark and shows the added and removed messages without an undo button |
| BM-2 | A bookmark is a note key with the epoch-millisecond time it was added, kept in a device-local store named bookmarks; nothing is written to any repository; it survives restart | `BookmarkRepositoryImplTest` maps store entries to bookmarks; `BookmarkKeyMigrationTest` moves old plain-id keys under the legacy source |
| BM-3 | Adding a key that exists overwrites its time (used by undo); removing unknown keys is ignored; the time defaults to the wall clock | `BookmarkUseCaseTest` |
| BM-4 | Bookmarks list newest first by added time, ties by note key; a bookmark whose note is not in the cache is not shown | `BookmarkMapperTest` |
| BM-5 | A bookmarked note shows a ribbon on its home card (fades in and out) and the card is described as bookmarked; card height does not change | Bookmark a note: the ribbon appears at the card's top-right edge and the card height is unchanged |
| BM-6 | A bookmarked note shows a small glyph in the list panel row, announced as bookmarked | Bookmark a note: its panel row shows the glyph and reads out "bookmarked" |
| BM-7 | The Bookmarks tab pushes onto `[Home]`; from a reader it replaces the stack with `[Home, Bookmarks]`; pressing it while on top is ignored; back goes to Home with no exit hint | `TabSwitchTest` (bookmarks cases) |
| BM-8 | The floating nav selection is derived from the back stack (nearest Home, Bookmarks or Stats below any reader or settings), never stored | Open a reader from Bookmarks: the Bookmarks item stays selected; back returns to the list |
| BM-9 | The latest bookmark is a large card (chip, "added" time, title up to 2 lines and first-sentence excerpt up to 3 lines in the one-column card — the two-column card allows a 5-line excerpt and the low-height card a 1-line title and 2-line excerpt; no excerpt if absent); older ones are numbered rows from 02 under a label with their count, shown only when at least two exist | With 1 bookmark only the card shows; with 3 the card and rows 02 and 03 show with count 2 |
| BM-10 | The excerpt is the first sentence of the first non-blank paragraph (else first non-blank list item), skipping code spans, decimals and abbreviations as sentence ends, cut at 140 characters with an ellipsis | `NoteExcerptTest` |
| BM-11 | The added time reads as today HH:mm, yesterday, month-day this year or full date another year, in the device time zone; the card uses the long form, rows the short form | `BookmarkTimeKindTest` |
| BM-12 | The remove button on the card or row removes at once without opening the note; a snackbar offers undo; undo restores the original added time and place, only for the last removal, and does nothing if the note has since disappeared | `NotesViewModelTest` "FM-9 해제한 북마크를 되돌린다" |
| BM-13 | Removing the large card promotes the next bookmark in place with a cross-fade and renumbers the rows; removing the last one fades to the empty state; reduced motion replaces moves with instant changes | Remove the big card of 3: the next appears there as card, rows renumber; remove the last: empty state shows after the fade |
| BM-14 | With no bookmarks the screen shows a title, a hint and a "pick a note from Home" button that switches to Home; while the first load runs it shows a spinner | Remove all bookmarks, press the button: Home opens and Home is selected |
| BM-15 | Tapping a card or row opens the reader without a shared-element move; back returns to Bookmarks | Open a bookmark and press back: the Bookmarks list shows again |
| BM-16 | The shell decides two columns when content width is at least 640dp (window minus open fixed panel, page margins and the rail reserve); two columns put the card left (not scrolling) and the rows right (scrolling); otherwise one column; low-height windows use a compact card | `NotesLayoutTest` (1280 open panel true, 852 open false, 852 folded true, 891x411 open false) |
| BM-17 | In rail layouts the content keeps a reserve to the left of the right rail so remove buttons and counts are not covered | Wide window: the last column ends left of the rail |
| BM-18 | A sync that deletes notes removes their bookmarks, and an open deleted note is closed; a failed cleanup is logged and does not turn the sync into a failure | Delete a bookmarked note upstream and sync: the bookmark and its glyph are gone |
| BM-19 | Deleting a device note removes its bookmark; changing or disconnecting the repository removes only that source's bookmarks | `DeviceNotesTest` (LN-11, RC-17), `ConnectionRepositoryTest` (RC-15, RC-17) |
| BM-20 | The big card and each row read as one item (title, category, time, ordinal) with the remove button separate; snackbar text is announced politely | Screen-reader dump of Bookmarks shows one description per card or row plus a remove button each |
| BM-21 | Bookmark scroll position is kept by the shell across detours to Reader or Home; one list state serves one and two columns | Scroll Bookmarks, open a note, return: same position |
