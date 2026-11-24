---
units: [notes:home, notes:list-panel]
plans: [001-reader-foundation, 004-bookmarks, 007-repo-connect, 010-brand, 012-feature-modules, 013-local-notes]
decisions: [RC-D44, RC-D52, BR-D3, LN-D4, LN-D8, LN-D13, DEC-16]
---
## home — Home

### Behaviour
- With at least one note, home is one grid: an optional connect band, the hero, an entry row, the label "최근 노트", then one card per note, newest date first (001-reader-foundation: RD-5).
- The grid fills the width from the card minimum width of the window class and never uses a fixed column count; from the Expanded class up it is capped at the reading width (001-reader-foundation: RD-6).
- A card shows the category chip, the date and the title; pressing it opens the reader; chip, date and title travel to the reader as shared elements keyed by note id (001-reader-foundation: RD-13) (012-feature-modules: FM-K5).
- A bookmarked note's card carries the corner ribbon; a device note's card carries the source mark; repository notes carry neither (004-bookmarks: BM-5) (013-local-notes: LN-5).
- The hero shows the app title, the connected repository's canonical name when a connection is confirmed and no repository line otherwise, and a short hint (010-brand: BR-6).
- The hint differs by window: Compact tells the person to open the list with the menu button, wider windows point at the fixed list on the left (001-reader-foundation: RD-5).
- Below the hero, two secondary buttons "새 노트" and "가져오기" sit in one row, in every state that has notes (013-local-notes: LN-2).
- The connect band ("저장소를 연결하세요" with "연결하기") is the first grid item and shows only at Compact width, only when no connection exists but cached repository notes do; wider windows show it in the list panel instead (007-repo-connect: RC-1).
- The band opens the stacked connect screen; it is not shown for device notes alone (007-repo-connect: RC-1).
- When there are no notes the hero and the grid are replaced by one centred block that scrolls as a whole when large text makes it overflow; it is centred in the area left after the floating bar or rail reserve.
- Which empty block shows is decided by three facts: a note count above zero is always the grid; no connection with zero notes is first run, even while a sync runs; a connection with zero notes is receiving while the first sync runs or the new connection's notes have not arrived, otherwise no notes (007-repo-connect: RC-21) (013-local-notes: LN-1).
- First run shows a tile, the title "아직 노트가 없습니다", a short description, "새 노트" as the single primary button, "가져오기" as secondary, then a caption line and the quiet "저장소 연결" button (013-local-notes: LN-1).
- First run at Compact height drops the tile, puts the two buttons side by side and puts the caption and the quiet button on one row (013-local-notes: LN-1).
- Receiving shows the download tile, "노트를 불러오는 중입니다", a description, a progress bar and the entry row; it is announced as a polite live region (007-repo-connect: RC-21).
- No notes shows the folder tile, "저장소에 노트가 없습니다", a "새로고침" secondary button (a refresh intent) and the entry row (007-repo-connect: RC-21).
- Deleting the last device note with no connection returns home to first run (013-local-notes: LN-1).
- Every entry button keeps its visible height and gets a 48 dp touch area.
- "저장소 연결" opens the connect screen on top of home and back returns home, unless connect actions are locked by a token check in progress (013-local-notes: LN-1).
- Empty-state titles use the title style at 17 sp with a line height of 1.3 times the size, in the title font (010-brand: BR-8).
- The grid shows whenever the recent list is non-empty, whatever the body value; a Notes body with a still-empty recent list draws nothing for that frame and never shows an empty block (007-repo-connect: RC-21).

### Public API
- `HomeBody`, `homeBodyOf(connected, noteCount, syncing)` — the four home states and the pure decision (first run, receiving, no notes, notes).
- `HomeAction`, `EntryWeight`, `HomeEntry`, `homeEntriesOf(body)` — the entry buttons per state with their weight (primary, secondary, quiet).

### Guarantees
- The decision table and the one-primary-button rule are proven by `HomeBodyTest` (013-local-notes: LN-1, LN-2) (007-repo-connect: RC-21).
- The empty-state title style is proven by `EmptyStateTitleTest` (010-brand: BR-8).

### Not covered
- The hero art — `design-system:brand`; the note card, its chip and ribbon drawing — `design-system:widgets`.
- Where notes, connection and sync state come from — `notes:state`.
- The connect screen opened by the band and the quiet button — `connect:flow`.
- The start decision (reading screen or the shell; connect is never a start result) — `app:shell-state`; the floating navigation — `app:shell-chrome`.
- No UI test in this module draws these screens; visual rules are checked by hand and by previews.

## list-panel — List panel

### Behaviour
- The panel, in a drawer at Compact width or a fixed pane from Medium up, starts with "노트 N개" where N is the total of all categories, whatever the search shows (001-reader-foundation: RD-8).
- Below it is a search field; typing filters the list at once by the rules of `notes:state`, and a blank query is no search (001-reader-foundation: RD-9).
- Categories are listed in the order the notes state gives (the category with the newest note first); each row shows the name, the count and an expand toggle; all start collapsed (001-reader-foundation: RD-8).
- Pressing a category row flips only that category; an expanded category lists its notes in the order given (001-reader-foundation: RD-8).
- While a query is set every matching category shows expanded and the stored expand choices are ignored; a press on a category then still flips the stored choice, which only shows after the query is cleared (001-reader-foundation: RD-9).
- With a query and no match, "검색 결과가 없습니다" replaces the list (001-reader-foundation: RD-9).
- The note being read is shown as the active row; bookmarked notes carry the glyph and device notes the source mark (004-bookmarks: BM-6) (013-local-notes: LN-5).
- Choosing a note opens the reader (from settings it takes settings' place) (001-reader-foundation: RD-11).
- The settings row is fixed below the list and never scrolls away; the list scrolls in the space between.
- The list fades at the top (16 dp) once it can scroll back and at the bottom (52 dp) while more follows, animated over 200 ms; no divider is drawn above the settings row.
- The settings row is highlighted only in a fixed pane while settings is open.
- The settings row shows model state: a ring (with a percent when known, in the warning tone while retrying) while the model is busy; otherwise "모델 오류" after a failure, nothing when ready, "모델 없음" otherwise; "토큰 확인" is added only when the shell reports a token check and the model is not busy.
- The connect band is the first list item only in the fixed pane and only with the same condition as on home; the drawer never shows it.
- Keyboard focus is released by any intent except typing, by the start of a list scroll and by a press on an empty part of the panel; the query is kept (001-reader-foundation: RD-10).
- The fixed pane pads itself for the status bar; inside the drawer the sheet already does, and the settings row pads for the navigation bar only in the drawer.

### Public API
- `ListPanelScreen(uiState, chrome, onIntent, modifier, inFixedPanel)` — the panel the shell puts in the drawer or fixed pane.
- `ListPanelChrome` — values from other features that the shell hands in: active note id, settings open, token check, model status.

### Not covered
- The drawer, fixed pane, fold button and back handling around the panel — `app:shell`.
- Row and search field widgets — `design-system:widgets`; the edge fade — `design-system:modifiers`.
- Search matching and expand storage — `notes:state`.
- Persistence: the query and the expand choices live in view-model state only and are not saved across process death.
