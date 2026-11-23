---
units: [design-foundation:palette, design-foundation:typography, design-foundation:adaptive]
plans: [001-reader-foundation, 010-brand]
decisions: [RD-D5, BR-D15]
---
## palette — Colours and radius
### Behaviour
- There is one palette, `StudyNotesDarkColors`, a dark theme. There is no light palette; values that carry alpha (glass, scrim, nav item highlights) rely on that.
- This is the only place where a colour literal may be written. Other modules use the names; a value is never re-created elsewhere.
- Adding a colour means one new field in `MyNoteColors` and its value in `StudyNotesDarkColors`; the theme holds the whole holder, so it needs no change. Only a new kind of holder needs a new composition local in the design system.
- A glass surface is a bundle, not one colour: navigation glass, panel glass (dialogs, blurred differently), an opaque fallback used when no backdrop exists, a glass border, and six values for the selected navigation pill (top, bottom, rim, specular, caustic, pressed).
- `correct` and `wrong` are made not to speak by colour alone; they are normally used with an icon or text. `warn` and `warnText` mean "not a failure, but worth knowing" (for example fewer questions were made than asked, or waiting for Wi-Fi), so they are not red.
- The lamp colours `LampTop` and `LampBottom` of `MyNoteBrandColors` are outside the palette on purpose. Only the hero mark's light gradient reads them; they are not used for surfaces, text or state, and they are not added to `MyNoteColors`, where they would look like ordinary tokens and leak onto screens. The night and icon background colours are Android colour resources in the app module, not here. (010-brand: BR-6)
- `MyNoteRadius` holds four constants (card, list, chip, inline code), read directly rather than through the theme. Most corner radii in the app are literals chosen in components, not tokens; the inline-code radius has no user today.
- There is no spacing scale; spacing is dp literals plus the page, gap and vertical-padding values of `adaptive`.
- A token nobody uses is deleted rather than kept, because a leftover name reads as an approved value.
- Colour holders are marked immutable; `MyNoteRadius`, an object of constants, is not marked.
### Public API
- `MyNoteColors` — immutable holder of all palette colours.
- `StudyNotesDarkColors` — the one palette value.
- `MyNoteBrandColors` — `LampTop`, `LampBottom`.
- `MyNoteRadius` — `card`, `list`, `chip`, `inlineCode`.
### Not covered
- The actual colour and radius values — the root `DESIGN.md` (colour and radius sections).
- Providing colours to the composition and reading them as `MyNoteTheme.colors` — `design-system:theme`.
- Glass drawing — the design system's glass components.
- No test in this module checks colour or radius values.

## typography — Typography
### Behaviour
- `myNoteTextStyles(bodySizeSp, heightClass, titleFontFamily)` returns the 19 named styles of `MyNoteTextStyles`; `heightClass` defaults to Medium and the font to the device font. The style names and sizes are listed in the root `DESIGN.md`.
- Two inputs vary styles, and they touch different styles. The reading size (`MyNoteFontScale`: Normal 16 sp, Large 18 sp) changes the body style (line height 1.78 times its size) and the two Markdown heading styles (body size plus 5 and plus 1). A short window (height class Compact) shrinks `display` from 52 to 40 sp and `screenTitle` from 19 to 17 sp. All other styles are fixed.
- A new style that must shrink on short windows takes the short-window branch; one that must grow with the body takes the body size. Mixing the two gives a value that follows neither.
- The title font applies to exactly four styles: `noteTitle`, `title`, `sectionTitle` and `screenTitle`. Every other style keeps the device font, and the three mono styles keep the monospace font, whatever font is passed. (010-brand: BR-8)
- Those four styles fix their line height and use a centred line-height style that trims nothing, so Gothic A1's line box equals the line height. With the device font, Korean text falls back to another font whose line box (about 1.45 em) can exceed the line height and leave extra space; the number of lines is the same. (010-brand: BR-8)
- Hero and note title sizes are not text styles; they are sizes in `adaptive` that vary continuously with the window. A screen applies one with `resized`.
- `TextStyle.resized(size)` sets the font size and scales a line height given in sp by the same ratio, so a bigger title does not get lines that touch. A line height that is missing or in em is left alone and only the size changes. (010-brand: BR-8)
- A screen does not keep a private type scale: a screen that bypasses the system drifts and creates one value under two names.
- Adding or removing a title-font style also means changing the style table in the brand design document; a Python test checks that table.
- The module does not know how the title font is obtained or whether it has arrived; the design system's font gate decides and passes a font family in.
### Public API
- `MyNoteFontScale` — `Normal` and `Large`, each with `bodySizeSp`.
- `MyNoteTextStyles` — immutable holder of the 19 styles.
- `myNoteTextStyles(bodySizeSp, heightClass, titleFontFamily)` — builds the holder.
- `TextStyle.resized(fontSize)` — the size-and-line-height helper.
### Guarantees
- Only the four title styles take the title font, everything else is equal to the device-font result for every reading size and height class, and the four fix line height and line-height style: `TitleFontStylesTest`.
- `resized` scales sp line heights, keeps the line-height style, and leaves a missing or em line height alone: `ResizedTextStyleTest`.
### Not covered
- Style values (sizes, weights, spacing) — the root `DESIGN.md`, typography section.
- Downloading and gating Gothic A1 — `design-system:font`.
- The reading-size setting and its storage — `settings` and `database`.

## adaptive — Adaptive tokens
### Behaviour
- Window width is classified into five classes: Compact under 600 dp, Medium under 840, Expanded under 1200, Large under 1600, ExtraLarge from 1600. Height is three: Compact under 480 dp, Medium under 900, Expanded from 900. A boundary value belongs to the upper class. The limits equal the Material 3 window size classes. (001-reader-foundation: RD-6)
- `adaptiveTokensFor(windowClass, heightClass)` returns the product; the height class defaults to Medium. Width fixes the horizontal structure: page padding, card gap, list pane width, reading maximum width, card minimum width and whether the list panel is a modal drawer. Height fixes the vertical rhythm: header height, vertical padding, fade height, hero logo size and hero title size. A new token goes on the axis of what it decides, and a token that needs both is a computed property. (001-reader-foundation: RD-6)
- An absent value means "none", not zero. The reading maximum width is absent at width Compact and Medium (the body is not capped), and present from Expanded. The hero logo size is absent at height Compact: the logo is not drawn. A zero would mean "draw at size 0". (001-reader-foundation: RD-5, RD-6)
- The list panel is a modal drawer only at width Compact; from Medium up it is a fixed pane. (001-reader-foundation: RD-7)
- The floating navigation is a right rail when the width class is Expanded or wider, or the height class is Compact (a bottom bar would eat the text on a low window); otherwise it is a bottom bar. (001-reader-foundation: RD-15)
- Derived values are computed properties of the token holder: rail right margin (the larger of 14 dp and page padding minus 12 dp), bar bottom margin (vertical padding plus 6 dp), panel header top padding (the larger of 10 dp and vertical padding minus 4 dp), and note title size (hero title size plus 4 sp).
- They live here, not in screens, because the rail-or-bar test is read by three places (the layout's bottom reserve, the scroll-to-top button position, the overlay choosing rail or bar); one owner cannot disagree with itself, and a body hidden behind the bar or a stray gap is avoided.
- The window is measured once, by the app shell, from the container size of the window; screens read tokens from the theme and do not measure again with a box's constraints, which describe the parent and not the window.
- Everything is a pure function of the two classes: no composable, no resources, no composition local read.
- The navigation module does not use this module, so the scene strategy cannot read the pane width; the app shell draws the pane at its width inside the panel slot.
### Public API
- `MyNoteWindowClass` — five width classes; `MyNoteWindowClass.fromWidth(dp)`.
- `MyNoteHeightClass` — three height classes; `MyNoteHeightClass.fromHeight(dp)`.
- `AdaptiveTokens` — immutable holder: the token fields plus `noteTitleSize`, `useFloatingRail`, `railRightMargin`, `barBottomMargin`, `panelHeaderPadTop`.
- `adaptiveTokensFor(windowClass, heightClass)` — the product of the two axes.
### Guarantees
- Height boundaries at 480 and 900, rail or bar for narrow-tall, wide and short windows, modal drawer only at Compact width, the reading cap starting at Expanded, the 14 dp floor of the rail margin, note title four larger than hero title, and no hero logo at Compact height: `AdaptiveTokensTest`.
### Not covered
- Token values per class — the root `DESIGN.md`, adaptive section.
- Where the classes are measured and handed to the theme — `app` shell; the theme object — `design-system:theme`.
- Laying out the card grid from the card minimum width, and drawing the rail or bar — `notes` and `app`.
### Unconfirmed
- The width limits are tested only through the height boundaries and the per-class results; the width boundaries 600, 840, 1200 and 1600 have no test of their own.
