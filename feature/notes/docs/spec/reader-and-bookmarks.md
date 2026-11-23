---
units: [notes:reader, notes:bookmarks]
plans: [001-reader-foundation, 004-bookmarks, 009-note-format, 010-brand, 013-local-notes]
decisions: [BM-D6, BM-D7, BM-D13, D-22, D-24, D-25, D-26, D-27, D-30]
---
## reader — Reader

### Behaviour
- The reader body is one scrolling column: a header row (category chip, date, and the source mark for a device note), the note title as a heading, the note blocks, then the reader-end slot (001-reader-foundation: RD-12) (013-local-notes: LN-5).
- The reader-end slot is drawn only for a readable note; a note that could not be read as a whole (broken or over a limit) gets no slot at all, because there is no plain text to build a quiz from (009-note-format: NF-8).
- A note whose blocks are only partly unreadable still gets the slot and draws the unreadable blocks as boxes; the box rendering belongs to `design-system` and the mapping to `ui` (009-note-format: NF-8).
- From the Expanded class up the column is capped at the reading width and centred; narrower windows use the full width with the page padding (001-reader-foundation: RD-6).
- The right-hand rail reserve is taken off the end of the whole column, so a wide window keeps its reading width; the bottom padding is 24 dp plus the floating-bar reserve.
- The note title grows with the window's note-title size and its line height follows at 1.25 times the size (010-brand: BR-8).
- Chip, date and title carry the shared-element keys `chip-`, `date-` and `title-` plus the note id, the same keys the home card uses (001-reader-foundation: RD-13).
- The destination draws nothing when the note id is not in the notes list, for example after the note was deleted or removed by a sync (001-reader-foundation: RD-12).
- The scroll state and the expanded-raw state come from the shell, so the position survives a window-class change; the reader keeps no scroll state of its own.
- A debug-only preview file draws the reader with 14 sample notes (text shapes, nested blocks, checklists, alerts, over-limit box, narrow and wide tables) at Compact, 852 dp and large text; it is not in release builds.

### Public API
- None — `NoteReaderScreen` is internal; the shell reaches it through `notes:destinations`.

### Guarantees
- The title size and line height rule is proven by `ReaderTitleTest` (010-brand: BR-8).

### Not covered
- The reader top bar (bookmark toggle, refresh, edit, delete) — drawn by `ui` and placed by `app:shell`; edit and delete exist only for device notes.
- Quiz card and quiz overlay — `quiz`; this unit only gives the slot.
- Block drawing, code scrolling, table layout — `design-system`; block mapping — `ui`.
- Recording the read session and the scroll percentage — `notes:state` and `app:shell-chrome`.
- No UI test in this module draws the reader; `app` instrumented tests cover back order and title fonts.

## bookmarks — Bookmarks

### Behaviour
- The Bookmarks tab shows the newest bookmark as a large card under the label "가장 최근" (004-bookmarks: BM-9).
- Older bookmarks follow as numbered rows from 02 under the label "그전에 모은 노트" with their count; the label and rows exist only when at least two bookmarks exist (004-bookmarks: BM-9).
- Bookmarks are ordered newest added first, ties by note key; a bookmark whose note is not in the list is not shown (004-bookmarks: BM-4) (013-local-notes: LN-11).
- The large card shows the category chip, the added time in its long form, the title and the first-sentence excerpt; the excerpt is absent when the note has none and is left out of the accessibility node (004-bookmarks: BM-9, BM-20).
- A row shows the two-digit number, the title on one line, "category · time" in the short form, and the remove button; the number is the position and is not a key, so rows renumber after a removal (004-bookmarks: BM-9).
- The card variant follows the window: Compact or Medium width at normal height uses a 21 sp title and a 3-line excerpt; Expanded width one pane 24 sp and 3 lines; two panes 26 sp and a 5-line excerpt; low height 18 sp, a one-line title and a 2-line excerpt with tighter padding (004-bookmarks: BM-16).
- The title takes at most two lines except in the low-height variant, which takes one (004-bookmarks: BM-9).
- Rows use 12 dp of vertical padding and 6 dp at low height, around a 44 dp minimum content height (004-bookmarks: BM-9).
- With two panes the card column on the left does not scroll and the rows on the right scroll alone, split 1.15 to 1 with a 28 dp gap (24 dp at low height); one bookmark leaves the right column empty (004-bookmarks: BM-16).
- Whether two panes are used is decided by the shell and passed in; this unit never measures the window itself (004-bookmarks: BM-16).
- One pane is a single list capped at the reading width; the list state is shared by one and two panes, and the shell keeps the scroll position (004-bookmarks: BM-21).
- The remove button on a card or row removes at once through the remove intent without opening the note; its icon is 20 dp on the card and 18 dp on a row inside a 44 dp touch target, never changes the item height, and is throttled (004-bookmarks: BM-12).
- Removing the featured bookmark promotes the next one in place: the card keeps its list key, its content cross-fades over 200 ms and its height animates over 300 ms (004-bookmarks: BM-13).
- Rows fade out over 180 ms, move over 300 ms and fade in over 240 ms; with reduced motion all moves are instant and only fades stay as snaps (004-bookmarks: BM-13).
- Pressing a card or a row opens the reader with no shared-element move, and back returns to the Bookmarks list (004-bookmarks: BM-15).
- The card reads as one item "가장 최근, title, category, time" and a row as "n번째, title, category, time", each with the remove button separate (004-bookmarks: BM-20).
- With no bookmarks the screen shows a tile, "북마크한 노트가 없습니다", a hint and a "홈에서 노트 고르기" button that switches to the Home tab; going between empty and non-empty cross-fades (004-bookmarks: BM-14).
- While the first load runs and no notes are known yet, a spinner shows instead of the empty state (004-bookmarks: BM-14).
- The undo for a removal is raised by the notes state as a snackbar notice; this screen only sends the intent (004-bookmarks: BM-12).

### Public API
- None — the Bookmarks screen and its card and row are internal; the shell reaches them through `notes:destinations`.

### Guarantees
- Ordering, the tie rule and hiding of note-less bookmarks are proven by `BookmarkMapperTest` (004-bookmarks: BM-4) (013-local-notes: LN-11).

### Not covered
- Adding and removing bookmarks, undo, and the study-log events — `notes:state`.
- The bookmark toggle in the reader — `ui` top bar, handled by `notes:state`.
- Time label wording (today, yesterday, this year, other year) — `ui`.
- Bookmark storage and clean-up when notes are deleted — `data` and `database`.
- No UI test covers this screen; it was checked by hand on the folding emulator.

### Unconfirmed
- Pixel sizes of the card variants come from the code constants and the owner's planning notes and were not re-measured on a device.
