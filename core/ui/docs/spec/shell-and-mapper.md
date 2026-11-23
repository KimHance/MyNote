---
units: [ui:shell, ui:mapper]
plans: [004-bookmarks, 007-repo-connect, 009-note-format, 012-feature-modules, 013-local-notes]
decisions: [LN-D14]
---
## shell — Shell frame and top bar
### Behaviour
- A registered destination receives from the shell only the frame values: top, bottom and end content padding, a two-pane flag, a stacked-scene flag, the scroll holder, the token drafts and the snackbar host (012-feature-modules: FM-4).
- Top padding is the room reserved for the model pill, bottom padding is the floating bar or navigation bar, end padding is the floating rail. The two-pane flag is measured without an open fixed panel. Scenes are stacked only on narrow windows (012-feature-modules: FM-4).
- Frame values are read where a screen uses them, so an animated value (the pill reserve) recomposes only the screens that read it.
- The holders (scroll states, reader source expansion, token drafts) are kept by the shell above the window-class branch, so folding, unfolding and rotation keep them (012-feature-modules: FM-4).
- A feature's own state, its entries and values tied up by `app` (connection present, shell back) are not frame values; they are separate parameters of its registration function.
- `sceneEdgeModifier` draws the scene edge (corner and shadow, following the scene progress) only when scenes are stacked; otherwise it adds nothing.
- `NotesScrollState` keeps one state each for the home grid, bookmarks list, stats list, stats side list, stats history, the reader scroll and the reader content. Which one the scroll-to-top button follows is derived: an open note wins, then bookmarks, stats, stats history, else home.
- The scroll-to-top button shows when the reader is scrolled more than 400 dp, or when a list's first visible item is the fourth or later (index 3 and up). Scrolling to top animates; on stats it scrolls the main and the side list together.
- Reader scroll and content state are created per note id, and the last id is kept while no note is open, so the state of the note left a moment ago is not replaced by an empty one.
- The top bar has a one-line title (ellipsis), height from the adaptive header height, and a leading control: a back arrow when `showBack`, else a `☰` menu named `노트 목록` (the glyph itself is hidden from accessibility).
- A disabled back arrow stays visible at 40% opacity; the connect screen disables it during the first sync (007-repo-connect: RC-24).
- Actions, left to right: bookmark toggle (only when the bookmarked value is not null), edit (`고치기`) and delete (`지우기`) when their callbacks are given, refresh (`새로고침`, shown by default).
- The bookmark toggle has checkbox semantics named `북마크` with state `북마크됨` or `북마크 안 됨`, a 44 dp touch area, a tint change over 160 ms and a 1.2x bounce over 240 ms only when a bookmark is added and motion is not reduced. Presses are throttled so rapid presses count once (004-bookmarks: BM-1).
- Edit and delete exist for device notes only; whether they show is decided by the shell, and delete opens the editor feature's confirmation instead of deleting (013-local-notes: LN-11).
### Public API
- `ShellFrame` — the frame interface (`contentTopPadding`, `contentBottomPadding`, `contentEndPadding`, `twoPane`, `stackedScenes`, `scroll`, `drafts`, `snackbarHostState`).
- `ShellDestinations` — type alias for a feature's destination registration over `ShellFrame`.
- `ShellFrame.sceneEdgeModifier()` — scene edge for stacked scenes.
- `NotesScrollState` — holder with `homeGrid`, `bookmarksList`, `statsList`, `statsSide`, `statsHistory`, `reader`, `readerContent`, `showScrollToTop`, `scrollToTop()`; created only through `rememberNotesScrollState(activeNoteId, isBookmarksTop, isStatsTop, isStatsHistoryTop)`.
- `NotesTopBar(...)` — the top bar composable with the parameters above (`title`, `showBack`, `bookmarked`, `onMenu`, `onBack`, `onRefresh`, `onToggleBookmark`, `showRefresh`, `backEnabled`, `onEdit`, `onDelete`).
### Guarantees
- Every destination key has exactly one registered screen and the shell passes only frame values (`ShellDestinationsTest` in `app`, FM-4 names) (012-feature-modules: FM-4).
### Not covered
- Implementing the frame, hiding the bar and navigation on the connect screen, header content and back order — `app`.
- Registering a feature's screens — each feature module's registration file, for example `connect:flow`.
- Navigation keys, the scene strategy and the scene edge itself — `navigation`.

## mapper — Block and font-scale mappers
### Behaviour
- `toContentBlocks` maps domain note blocks to reader blocks one to one, in order, so the reader and the editor preview draw a note with the same renderer (013-local-notes: LN-3).
- Headings keep their level; code keeps only its text; ordered lists keep the start number; list items keep the checked state and their child blocks (009-note-format: NF-6).
- Text runs keep text, bold or code style, italic and strike-through; overlapping shapes map as they are (009-note-format: NF-4).
- Tables keep the header, the rows and the per-column alignment (none, left, center, right) (009-note-format: NF-5).
- An unsupported block stays at its place with a kind: unknown type, broken block, broken note, or limit exceeded; the box wording numbers (depth, kilobytes, table cells) come from the limits passed in, default `NoteLimits.Default` (009-note-format: NF-8).
- The salvaged text and the original are carried only when not blank, so an empty original gives no `원문 보기` button.
- Inside a list item an unsupported child block becomes a plain paragraph of its salvaged text, or is dropped when there is none (009-note-format: NF-6).
- A 32-level nested list still maps (009-note-format: NF-6).
- Font scale maps both ways between the domain (`Normal`, `Large`) and the design system's scale.
### Public API
- `List<NoteBlock>.toContentBlocks(limits)` — the reader blocks as an immutable list.
- `FontScale.toUi()` and `MyNoteFontScale.toDomain()` — font scale conversion.
### Guarantees
- The mapping of reasons, salvage, order, shapes, tables, children, deep nesting and limit numbers is fixed by `NoteBlockMapperTest`.
### Not covered
- Producing the domain blocks from text — `format`.
- Drawing the reader blocks — `design-system`.
- Where the limits are decided — `domain` and `format`.
