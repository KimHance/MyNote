---
units: [design-system:glass-nav, design-system:widgets, design-system:brand]
plans: [001-reader-foundation, 004-bookmarks, 010-brand, 012-feature-modules, 013-local-notes]
decisions: [D-18, D-19]
---
## glass-nav — Glass surface and floating navigation
### Behaviour
- `GlassSurface` is the only place that draws the liquid-glass effect (blur, lens refraction, highlight, shadow); the bar, rail, scroll-to-top button, status pill and the quiz setup dialog all use it, and the effect chain is never rebuilt outside it.
- The effect needs two ends to agree: the refracted content carries `layerBackdrop` (set by the app shell, `app:shell`) and the glass surface draws with the same `Backdrop` instance. If the content does not carry it the glass shows none of the content behind it and no error or log appears; a surface given no backdrop takes the opaque path below.
- Trap: only the navigation display may sit inside the `layerBackdrop` box. The glass bar, rail, pill and any overlay must stay outside it, or the glass samples itself (012-feature-modules: FM-K8).
- With `backdrop = null`, the default, the surface is an opaque fallback: clipped shape, opaque glass colour and a border. Previews take this path; every new parameter must handle both paths.
- Degrading by SDK (refraction from API 33, blur from 31, opaque below) is decided by the effect library; callers do not branch on SDK.
- `blurRadius` and `refractionRatio` trade against each other: blur removes the detail refraction would push, so the default blur is small. Large reading surfaces (bar, rail) keep the defaults; a small droplet (quiz segment) sets a small blur (4 dp), the droplet refraction ratio and no shadow.
- Refraction amount is the radius times the ratio, so changing the radius changes the refraction strength too. A given `tint` replaces the glass colour (over the background colour on the opaque path).
- `GlassSurface` calls its content exactly once; branching happens in modifiers so the content slot keeps its state.
- `FloatingNavBar` (bottom) and `FloatingNavRail` (right) show a list of items with one selected; each item is a tab: the selected item of the bar shows its label beside the icon, other bar items are icon only with the label as description, and every rail item is icon only with the label as description (001-reader-foundation: RD-23).
- Selection animates with a bouncy spring on a pill with gradient, caustic, rim and specular highlight; a press darkens the item. `FloatingNav` does not use `PressIndication` on purpose, because it draws all of this itself and reads selection and pressed state as lambdas in the draw phase.
- `hidden = true` slides the bar down and fades it (340 ms move, 200 ms fade), or the rail to the right by its width plus the right margin; hiding changes only position and alpha and does not remove the items from composition.
- Selecting calls `onSelect(id)` with the item id; the items, hidden flag and window-class choice (bar or rail) come from the caller (`app:shell-chrome`).
- `FloatingNavScrollState` turns scroll deltas into a hidden flag: movement under 6 dp is ignored as jitter, it never hides within the top 48 dp, the accumulated offset never goes below 0, scrolling down past the zone hides and scrolling up shows (001-reader-foundation: RD-14).
- `reset()` returns the offset, the reference point and the hidden flag to the start; the shell calls it when the top screen changes (`app:shell`) and when scroll-to-top is pressed (`app:shell-chrome`).
- Its nested-scroll connection reports every pre-scroll to the state and consumes nothing, so it can be attached above any scrolling content.
- `ScrollToTopButton(visible, onClick)` is a 46 dp glass circle labelled "맨 위로" that scales and fades in (220 ms) and out (160 ms); whether it is visible (reader scrolled past 400 dp, list once three items have scrolled off the top) is the caller's decision, made in `ui:shell` (001-reader-foundation: RD-14).
- `FloatingStatusPill` is a 40 dp glass pill with a ring or an icon, a one-line label that ellipsises and an optional mono trailing text; it is clickable (button role) only when `onClick` is given.
- The pill ring is `StatusRing`: with a progress between 0 and 1 (clamped) it fills clockwise from the top; with null progress it is an endless spinning arc. The spinning arc has no reduced-motion check.
### Public API
- `GlassSurface(radius, backdrop, modifier, blurRadius, refractionRatio, tint, shadow, content)` — the glass container; `GLASS_BLUR_RADIUS`, `LENS_AMOUNT_RATIO`, `DROPLET_REFRACTION_RATIO` and `GlassShadow` are its defaults and droplet values.
- `FloatingNavItem(id, label, icon)`, `FloatingNavDefaults` (`BarHeight`, `RailWidth`) — item model and sizes.
- `FloatingNavBar(items, selectedId, onSelect, hidden, modifier, backdrop)` and `FloatingNavRail(..., rightMargin, ...)` — the two navigation forms.
- `FloatingNavScrollState`, `rememberFloatingNavScrollState()` — hide and show rules with a `nestedScrollConnection`, `onScroll`, `reset`, `hidden`.
- `ScrollToTopButton(visible, onClick, modifier, backdrop)`, `ScrollToTopDefaults.Size` — scroll-to-top.
- `FloatingStatusPill(label, tone, modifier, trailing, progress, icon, backdrop, onClick)` and `StatusRing(progress, tone, modifier, diameter)` — status pill and ring.
### Guarantees
- The hide and show rules (jitter, safe zone, floor at 0, reset) hold: `FloatingNavScrollStateTest`. The class holds only two pixel values and needs no density, so that the rules are testable; keep new rules in that shape.
### Not covered
- Creating the `layerBackdrop` box and ordering the shell and back handling — `app:shell`; which screens show a nav — `app:shell-chrome`.
- Choosing bar or rail by window class and the right margin — `app:shell-chrome` using `design-foundation:adaptive` tokens.
- Glass colours, radii and shadows as values — DESIGN.md section 4.4.
- The quiz setup dialog content — `quiz:overlay`.
### Unconfirmed
- Whether a hidden bar or rail still takes touches: nothing here disables input while hidden, and whether the shell does was not checked.

## widgets — Cards, rows and small widgets
### Behaviour
- `NoteCard` shows a category chip, the date, an optional "이 기기" mark and an optional trailing slot on one meta row, and the title below; the whole card is one button.
- On the meta row the category chip is shortened with an ellipsis before the date; the row always keeps 24 dp free at its end whether or not the note is bookmarked, so toggling a bookmark never moves the ellipsis or changes the card height (004-bookmarks: BM-5).
- A bookmarked card shows a ribbon at its top right that overlaps the card border and fades in and out over 160 ms; the ribbon has no description of its own and the card carries the state description "북마크됨" (004-bookmarks: BM-5).
- `NoteCard` takes `Modifier` slots for the chip, date and title so a feature can attach shared-element transitions; the defaults keep previews working.
- `NoteRow` is a list-panel row for one note: the active row is selected for accessibility and shown with the soft accent surface; trailing glyphs are the device mark then the bookmark mark, each with its own description (004-bookmarks: BM-6) (001-reader-foundation: RD-23).
- `CategoryRow` shows a rotating caret (decorative, hidden from reading), the name and the count; it states "펼침" or "접힘" as its state and is a button (001-reader-foundation: RD-23).
- The source mark appears only on device notes: `NoteCard(device = true)` shows the label and `NoteRow(device = true)` the glyph; repository notes get neither (013-local-notes: LN-5).
- The source mark is a glyph plus the words "이 기기", so it is readable without colour; the label's glyph is decorative, while the stand-alone glyph carries "이 기기" as its description.
- `BookmarkRibbon` is 12 by 17 dp with a notch at the bottom; `BookmarkGlyph` is the small filled bookmark with the description "북마크됨".
- `CategoryChip` is one line with an ellipsis, accent text on a soft accent fill with a border.
- `SearchField` is a one-line 40 dp field: the placeholder (default "노트 검색", taken from resources when null) shows only while the query is empty, every edit calls `onQueryChange`, and the search key just clears focus; there is no clear button and no debounce here.
- `SectionLabel` is read as a heading and merged; an optional count and an optional hint follow on the same baseline.
- `NumberedRow` shows a two-digit zero-padded number, a one-line title and a one-line subtitle (both ellipsised), an optional trailing slot and a divider; the row reads as one item using the caller's `contentDescription`, has a 44 dp minimum content height, and `compactHeight` shrinks its vertical padding.
- `MyNoteSnackbarHost` and `MyNoteSnackbar` draw a snackbar from theme tokens, because the stock Material snackbar would take inverse colour-scheme colours that this app does not provide.
- Clickable boxes carry a role (button or tab), decorative icons have no description, and a parameter default is never a string literal: text defaults are null with a string resource fallback (001-reader-foundation: RD-23).
### Public API
- `NoteCard(category, date, title, onClick, modifier, chipModifier, dateModifier, titleModifier, bookmarked, device, trailing)` — home and bookmark card.
- `NoteRow(title, active, onClick, modifier, bookmarked, device)`, `CategoryRow(name, count, expanded, onClick, modifier)` — list panel rows.
- `CategoryChip(text, modifier)`, `SectionLabel(text, modifier, count, hint)`, `NumberedRow(...)` — small shared pieces.
- `SearchField(query, onQueryChange, modifier, placeholder)` — search input.
- `MyNoteSnackbarHost(hostState, modifier)`, `MyNoteSnackbar(data, modifier)` — themed snackbar.
- `BookmarkRibbon`, `BookmarkRibbonDefaults`, `BookmarkGlyph`, `BookmarkGlyphDefaults` — bookmark marks.
- `DeviceOriginLabel`, `DeviceGlyph`, `DeviceOriginDefaults` — device source marks.
### Guarantees
- A category row states its expansion and hides its caret from reading; only the row of the open note is selected: `ListRowSemanticsTest` (instrumented).
### Not covered
- Deciding which notes are bookmarked or device notes — `notes:state`; the shared-element wiring on cards — `notes:home` and `notes:reader`; the transition scope — `navigation:shared-elements`.
- The list panel screen — `notes:list-panel`; the bookmarks screen and its row variants — `notes:bookmarks`.
- The bookmarks screen's own row, which is separate from `NumberedRow` — `notes:bookmarks`.

## brand — Hero, logo and icons
### Behaviour
- `MyNoteHero` stacks the A mark, the lower-case "mynote" wordmark, an optional repository line and an optional hint; its vertical padding is fixed and its height does not depend on the repository name's length (010-brand: BR-6).
- The mark and the hint are drawn only when the adaptive tokens give a hero logo size; at Compact height the mark and the hint are absent while wordmark and repository line stay (010-brand: BR-6, BR-K4).
- The wordmark is sized from the hero title size token, not from the user's text size, so it does not grow with the font scale (010-brand: BR-6).
- The wordmark reads as a heading named by the `title` argument (MyNote); the mark is decorative (010-brand: BR-6).
- The repository line shows the name exactly as given in one line with a middle ellipsis; its accessibility text is the full name; with `repoName = null` the line is absent and nothing replaces it (010-brand: BR-6).
- The logo vectors are built from the current accent and text colours and are never tinted: the mark's window frame and its light are different colours, and a tint would flatten the light.
- `MyNoteIcons` holds the icon set as `ImageVector`s on a 24 viewport, drawn in white to be tinted at the use site: most are line icons with a 1.7 stroke, while `BookmarkFilled`, `Sparkle` and `CheckBoxChecked` are filled shapes.
- The six device-note icons (new note, import, edit, delete, device, paste) are single no-fill stroke paths taken from the design hand-off.
- Icons are created lazily on first use.
### Public API
- `MyNoteHero(title, repoName, modifier, hint)` — the home hero.
- `MyNoteIcons` — `Home`, `Search`, `Bookmark`, `BookmarkFilled`, `Stats`, `Settings`, `Key`, `ArrowUp`, `Sparkle`, `Check`, `CheckBox`, `CheckBoxChecked`, `Close`, `ChevronRight`, `Warn`, `NewNote`, `Import`, `Edit`, `Delete`, `Device`, `Paste`.
### Guarantees
- A long repository name stays on one line, the frame height equals the short-name height, the title is a heading, the full name is readable, there is no mark at Compact height, no repository line without a name, and the wordmark does not scale with text size: `MyNoteHeroTest` (instrumented, 010-brand: BR-6). That test also saves screenshots to the test app's external files folder.
- The six device-note icons are single no-fill paths of stroke 1.7 on a 24 viewport, and the trash icon reads five moves and four arcs: `MyNoteIconsTest`.
### Not covered
- The launcher icon, adaptive icon layers and splash drawing — `app:build`; the splash exit — `app:launch`.
- The logo colours as tokens (the logo-only daylight gradient) — `design-foundation:palette`.
- Where the hero sits on home and what the hint says — `notes:home`.
