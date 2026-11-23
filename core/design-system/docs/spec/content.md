---
units: [design-system:blocks, design-system:charts]
plans: [001-reader-foundation, 005-stats, 006-sync, 009-note-format, 011-screen-copy, 012-feature-modules]
decisions: []
---
## blocks — Note body blocks
### Behaviour
- `NoteContent(blocks, modifier, state)` draws a note body as a full-width column with 12 dp between blocks; the reader is a plain vertical scroll with no lazy list, so every block, table cell and sub-block is measured when the note opens, and the renderers finish their measuring in one pass.
- Heading levels 1 and 2 use the heading-2 style and levels 3 to 6 the heading-3 style; every heading, also inside a list item, is read as a heading (009-note-format: NF-3).
- A paragraph uses the body style; a code block never wraps and scrolls sideways; a quote has a 3 dp accent bar and body text in italic; a divider is a 1 dp line with 8 dp above and below; a spacer is 8 dp (001-reader-foundation: RD-12).
- Text shapes come as spans: normal, bold and code, each combinable with italic and strike. Code spans are monospace in the accent colour on a soft fill with one space on each side, never italic, and take only the strike-through (009-note-format: NF-4).
- Inside a quote, italic spans stand upright and code spans stand, so they stay distinguishable from the quote's own italic (009-note-format: NF-4).
- A text with strike-through spans is read aloud with the words "취소선" before and "취소선 끝" after each run; neighbouring runs of different shape count as one run; a text without strike-through keeps its normal spoken text (009-note-format: NF-4).
- Lists are bullet or numbered, numbered from `start`; the bullet glyph cycles by real depth (•, ◦, ▪) and items are 6 dp apart (009-note-format: NF-6).
- The marker column of a list is measured from the actual markers of that list (widest number plus the checkbox cell if any; in a bullet list the wider of the bullet and the checkbox cell), and item text and sub-blocks start at its end; markers align to the first baseline of the item text (009-note-format: NF-6).
- A checkbox item shows an empty or checked box before the text (after the number in numbered lists); the item text carries the state "체크됨" or "체크 안 됨", and for an item without text the box itself carries it (009-note-format: NF-6).
- A list item can hold sub-blocks (lists, paragraphs, code, quotes, tables); the visible indent stops after three levels: from the fourth level a list is pulled back by its parent's marker column, while glyphs still follow the real depth (009-note-format: NF-6).
- A table is drawn by one custom layout: columns of header and body cells, one outer border with rounded corners, 1 dp lines, a tinted header row with semi-bold text, cell padding, and per-column alignment (none means start); header cells are read as headings (009-note-format: NF-5).
- Short rows are padded with empty cells; the column count is the larger of the header and the longest row; a table without columns has no height (009-note-format: NF-5).
- Column widths: target is the longest line capped at 16 em, minimum is the longest word but at least 6 em (and never above the target). If all targets fit the body width, targets are used; if only minimums fit, the spare width is shared in proportion to target minus minimum and the widths add up to the body width without passing any target; if even minimums do not fit, minimums are used and the table scrolls sideways (009-note-format: NF-5).
- A table wider than the body scrolls sideways with a fade on the hidden edge(s); cells in a row share the lowest first baseline; the column widths come from the cells' intrinsic widths and the cells are then measured once, at fixed widths, with no subcompose (009-note-format: NF-5).
- A table is read with its row and column counts, and every cell with its row and column position (009-note-format: NF-5).
- An unsupported block is a dashed 1 dp box instead of a body block; it shows a notice by kind and, when salvaged text exists, that text below it; one unreadable block never hides the rest of the note (006-sync: SY-K1).
- Unknown type: "이 앱 버전에 없는 형식이라 글자만 보여 줍니다." (or "…보여 줄 수 없습니다." when nothing was salvaged) followed by "앱을 업데이트하면 보일 수 있습니다."; broken block: "형식이 맞지 않아 글자만 보여 줍니다." (or "…보여 줄 수 없습니다.") without the update hint.
- Whole-note kinds show a title and a body: a broken note says it could not be read, and a limit-exceeded note names the three limits it took from the kind (nesting depth, file KB, table cells in the note) and never holds numbers of its own (009-note-format: NF-2, NF-8) (011-screen-copy: SC-K5).
- When a box has original text, a quiet "원문 보기" button (with a turning chevron) opens a panel; the button then reads "원문 숨기기". Without original text there is no button.
- The raw panel first shows up to 40 lines; if more exist, a button "N줄 더 보기" widens it to at most 200 lines or 10,000 characters; if text still remains it says "앞 N줄만 보여 줍니다." with the shown line count.
- Raw lines are cut by a character budget of 10,000 including line breaks: a line that does not fit is dropped whole, except a first line over the budget, which is cut at 10,000; lines split on LF and CRLF, a final empty line is dropped, and an empty original has no lines.
- Raw lines are one text per line in a selectable container, so copying keeps line breaks; wrapped lines hang under the original indent plus two cells (a tab counts as two); for screen readers the panel is one merged item.
- Collapsing the panel also drops the "all 200 lines" step, so opening again starts at 40 lines; expand and collapse animate the size (240 ms and 180 ms) and the chevron (160 ms), and the panel fades in over 160 ms; under reduced motion the size and chevron change at once and only the fade-in still plays.
- Expanded and "show all" are kept per top-level block position in `NoteContentState`, which survives process and window-class changes through its saver; the caller owns the instance and passes it in, because state held inside the blocks is lost when a window-class change rebuilds the screen (012-feature-modules: FM-4).
- An unsupported block nested in a list item is drawn without its original text and without a button, collapsed.
### Public API
- `NoteContent(blocks, modifier, state)` — draws an `ImmutableList<NoteContentBlock>`.
- `NoteContentState`, `rememberNoteContentState()`, `NoteContentState.Saver` — per-block expansion state and its saved form.
- `NoteContentBlock` — closed set: `Heading`, `Paragraph`, `Code`, `Quote`, `BulletList`, `OrderedList`, `Table`, `Divider`, `Spacer`, `Unsupported`.
- `UnsupportedKind` — `UnknownType`, `BrokenBlock`, `BrokenNote`, `LimitExceeded(maxDepth, maxKilobytes, maxTableCells)`.
- `NoteContentListItem`, `NoteContentCell`, `NoteContentSpan`, `NoteSpanStyle`, `NoteColumnAlign` — model parts.
### Guarantees
- Expansion is per block, collapsing resets "show all", and the state survives a save and restore: `NoteContentStateTest`.
- Raw cutting by lines and characters, CRLF, tabs and boundary cases: `RawSliceTest`.
- Column width rules: `TableColumnWidthsTest`; bullet glyph by depth and the indent stop at level 3: `NoteListMarkerTest`; strike-through reading and upright italic or code in quotes: `NoteSpansTest`.
- Heading, checkbox state, table row and column information and the strike-through spoken text reach the semantics tree: `NoteContentSemanticsTest` (instrumented).
### Not covered
- Turning raw notes into blocks, limits and format-version rules — `format`; mapping domain blocks to this model (a nested box becomes plain text there, empty `salvaged` or `raw` become null) — `notes`.
- Where limit numbers come from — `format`; they are carried in `LimitExceeded` and never repeated in this module.
- Adding a block kind: the Markdown converter, cache model, domain, mapper, this sealed set and the `when` in `NoteContent` move together (see the `format` module guide); the JSON format 1 set is not extended.
- Reader chrome, reading width and scroll position — `notes` and `app`.

## charts — Statistics charts
### Behaviour
- Five Canvas charts: `TimeBars`, `CategoryDonut`, `CategoryStackBar`, `HBarRow` and `DivergingBars`. Each reads a `progress` function (0 to 1) only while drawing and takes one `contentDescription`; the whole chart is read as that single text and its parts are hidden from screen readers (005-stats: ST-25).
- `rememberChartProgress(key)` runs 0 to 1 over 540 ms; a new key or a change of reduced motion starts it again, and with reduced motion it starts at 1 so nothing animates.
- Motion timing is one object: bars start 20 ms apart (at most 120 ms of delay), grow 300 ms with the standard curve, value labels fade in 120 ms after their bar, and a sweep (donut, stacked bar) runs 300 ms.
- `ChartBarKind` is past, current or pending: the current bar is drawn in the accent colour, past bars in a second chart colour, and a pending bar as a dashed flat line (no value) in the future colour.
- `TimeBars`: the tallest bar fills `maxBarHeight` and the others scale to the largest value (at least 1); a bar of value 0 is a 2 dp stub, other bars are at least 4 dp tall; bars are at most 28 dp wide with 12 dp gaps; labels sit under every bar and the value above non-pending bars with a value over 0; an empty list draws nothing.
- `CategoryDonut`: a 120 dp ring starting at the top and running clockwise with the total and unit in the middle; slices of 0 or less are skipped; when the sum is not positive a full ring in the border colour shows.
- `CategoryStackBar`: a 12 dp pill split by slice values with 2 dp gaps, revealed left to right by progress; zero slices are skipped and a non-positive sum draws a border-coloured bar.
- `HBarRow`: title and subtitle on the left (title bold when highlighted), a thin track with a bar of `fraction` (null counts as 0, clamped to 0..1; accent when highlighted, secondary text colour otherwise), and the value right-aligned; `index` staggers the grow.
- `DivergingBars`: an up bar in the accent colour above a baseline and a down bar below it for each bucket; one shared unit scales both sides so the largest up and down both fit their areas; a pending bucket has a dashed baseline; labels sit underneath; no value labels.
- Text widths are measured only when the data changes; paths and dash patterns are kept between frames.
### Public API
- `rememberChartProgress(key)` — the progress function for a chart.
- `ChartBarKind`, `ChartBar(label, value, kind)`, `TimeBars(bars, maxBarHeight, progress, contentDescription, modifier)`.
- `ChartSlice(value, color)`, `CategoryDonut(slices, total, unit, progress, contentDescription, modifier)`, `CategoryStackBar(slices, progress, contentDescription, modifier)`.
- `HBarRow(title, subtitle, value, fraction, highlighted, progress, contentDescription, modifier, index)`.
- `DivergingBar(label, up, down, kind)`, `DivergingBars(bars, progress, contentDescription, modifier)`.
### Not covered
- What the numbers mean, periods, bucket labels and the text of each description — `stats`.
- Section labels and numbered rows around the charts — `widgets`.
- The chart colour tokens — `design-foundation`; layout of the stats screen — `stats`.
- Unit tests: none exist for the charts; they are checked on the stats screen.
