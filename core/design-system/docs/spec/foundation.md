---
units: [design-system:theme, design-system:font, design-system:modifiers]
plans: [002-quiz, 010-brand]
decisions: [BR-D4]
---
## theme — Theme and motion
### Behaviour
- `MyNoteTheme` hands four values to everything inside it: colours, adaptive tokens, text styles and the body font scale, read as `MyNoteTheme.colors`, `.adaptive`, `.textStyles` and `.fontScale`.
- The colours are one fixed dark palette: the theme never reads the system dark or light setting, there is no light palette, and no Material colour scheme is provided.
- Called with no arguments it applies Compact width, Medium height and Normal font scale; a preview that needs another form factor must pass the classes.
- Reading colours, adaptive tokens or text styles outside `MyNoteTheme` throws with a message telling you to wrap in it; only the font scale has a default (Normal).
- Adaptive tokens are rebuilt only when the width or height class changes; text styles only when font scale, height class or title font changes. A changed value re-composes the whole subtree under the theme, so callers must pass stable classes rather than continuously changing values.
- Material3 widgets (`Text`, `Icon`, `Surface`, `Snackbar`) work but read no colour scheme from here: colours and styles must be passed from tokens, or Material defaults show and clash with the theme.
- `ProvideTitleFontFamily` sits outside the theme and sets the family the title text styles use; without it titles use the device font, so previews and tests need no wrapper.
- `MyNoteEasing` and `MyNoteExitEasing` are the two shared curves (values in DESIGN.md section 6.1); other motion values live next to the component that plays them.
- `rememberReduceMotion()` is true in preview and inspection mode and when the system animator duration scale is 0; it is read once per composition and does not follow a later change.
- Only the chart progress and the raw-text box of an unsupported block consult it here; the looping or entering modifiers (`aiShine`, `riseIn`, `flip`, `StatusRing`) do not, so callers skip them themselves (002-quiz: QZ-19).
### Public API
- `MyNoteTheme { }` — provides the four values; arguments `windowClass`, `heightClass`, `fontScale`.
- `MyNoteTheme.colors`, `.adaptive`, `.textStyles`, `.fontScale` — the current token values.
- `ProvideTitleFontFamily(fontFamily) { }` — sets the title font family for themes inside it.
- `MyNoteEasing`, `MyNoteExitEasing` — the standard and exit curves.
- `rememberReduceMotion()` — whether animations should be skipped.
### Not covered
- The palette, text style list, spacing, radius and adaptive token values — `design-foundation`, described in DESIGN.md.
- Deciding the window and height class from the container size — the app screen root.
- Choosing the title font — see `font`.
### Unconfirmed
- Whether the app wraps `MyNoteTheme` in exactly one place at the screen root was not checked; the intent is a single wrap.

## font — Title font gate
### Behaviour
- The title font is Gothic A1 weight 700, received at run time from the Google Play services font provider; no font file is in the APK (010-brand: BR-8).
- The font is decided once per process: it is used if it arrives before the wait ends, otherwise the device font is used for the whole process; a font that arrives late is dropped (010-brand: BR-8).
- Download and provider check start together. No provider, a failed download or an exception (other than cancellation) gives the device font at once, without waiting for the end of the wait (010-brand: BR-8).
- The wait ends at the later of the first app frame being ready and the end of the launch lamp animation; the app passes that wait in (010-brand: BR-8).
- The time left until the lamp ends is measured from the launch request: process start on a cold start (screen created less than 5 seconds after process start), the screen creation time otherwise. It can be negative, meaning no wait (010-brand: BR-8).
- The family is loaded as a ready typeface, so it never changes while a screen is drawn; the provider answers on the calling thread's looper, so loading runs on a separate looper thread instead of the main thread (010-brand: BR-8).
- A name other than Gothic A1 can be given when building the source; the app uses this for a debug build that simulates a font that cannot be received.
- Nothing here shows an error or blocks: every failure path ends in the device font.
- A second `decide` call returns the first result whatever source it passes; concurrent callers wait for the first to finish.
### Public API
- `TitleFontSource` — interface: `isAvailable()` provider present, `load()` the family or throws.
- `googleTitleFontSource(context, fontName)` — the Google provider source; null name means Gothic A1.
- `decideTitleFont(source, giveUp)` — races the load against `giveUp`; returns the family or `FontFamily.Default`.
- `TitleFontGate` — holds the decision; `decide(source, giveUp)` and the nullable `fontFamily`.
- `awaitFirstFrameAndLamp(firstFrame, lampRemainingMillis)` — the `giveUp` the app uses: waits for the first frame, then for the remaining lamp time read after it.
- `titleFontWaitMillis(processStartMillis, createdMillis, nowMillis, lampMillis)` — time left until the lamp ends.
### Guarantees
- Within the cap the font is Gothic A1; failure, lateness or no provider gives the device font; a decided font never changes; remaining time follows the launch request: `TitleFontGateTest`.
### Not covered
- Which text styles are title styles and their line-height rules — `design-foundation` and DESIGN.md.
- Starting the decision and handing the family to the theme — the app main activity; see `theme` for `ProvideTitleFontFamily`.
- The splash and lamp animation themselves — the app.

## modifiers — Press, rise, flip and fade modifiers
### Behaviour
- Rule for all motion here: animated values are read in the draw or layout phase, never in composition. `FlipState` exposes `rotationX` and `scale` as lambdas, `PressIndication`, `riseIn` and `aiShine` keep their progress inside a modifier node, and callers must not pass animated values as `Modifier.alpha(v)`-style arguments.
- `PressIndication` draws a surface and the content together and scales both: a press eases in over 90 ms, release springs back; overlapping presses count, and a cancel counts as a release.
- `PressIndication` colours: `color` is the resting surface, `pressColor` the pressed surface, `scale` the pressed scale; with both colours unspecified nothing is filled and only the scale applies.
- Trap: with `color` set the indication draws the resting surface itself, so the click must pass an explicit interaction source; with a null source Compose creates the node at the first touch and the button background is invisible until pressed. A null source is fine for `pressColor`-only or scale-only use.
- Trap: do not put a separate `background` before a `PressIndication`: it would not scale with the content and the label would look shrunken inside it. The surface goes through `color` and `pressColor`.
- `PressIndication` implements equality by hand over all its parameters; a new parameter must be added to both `equals` and `hashCode` or colour changes will not reach the node.
- `aiShine` draws a border of `baseColor` and, while `running`, a highlight that circles the border once every 3.2 s without stopping; `running = false` stops the circle and leaves the border; content is drawn on top.
- `aiShine` draws into its own layer so that its endless redraw does not re-record the parent; without that layer a reader-end card would re-record the whole note body on every frame.
- `aiShine` rebuilds its ring path and gradient only when its size changes.
- `riseIn(delayMillis, durationMillis, distance)` fades the element in and moves it up from `distance` (12 dp default) over 300 ms by default, once when it enters composition; it does not change the element's layout size and ignores reduced motion (callers skip it).
- Later changes of the delay or duration are ignored by `riseIn`; only `distance` updates.
- `rememberFlipState()` is read inside an `AnimatedVisibility` content: entering turns about the horizontal axis from a tilt of 78 degrees to flat while growing from 0.52 to full size with a bounce; leaving reverses without bounce. `flip` applies it in the draw layer.
- `fadingEdges(top, bottom)` fades content into transparency at the top and bottom edges; an edge of 0 dp is skipped.
- `horizontalScrollFade(state, width, color)` covers the hidden side(s) of a horizontally scrolling area with a fade of `color`; it is placed before `horizontalScroll`, reads the scroll state only in draw, follows right-to-left layout and never covers more than half the width. `color` must equal the background behind the area.
- `throttledProcess(minIntervalMillis, action)` returns a callback that runs the latest `action` at most once per interval (500 ms by default): the first call runs, calls inside the interval are dropped, not queued (002-quiz: QZ-3).
### Public API
- `PressIndication(shape, color, pressColor, scale)` — indication node factory for `clickable`.
- `Modifier.aiShine(color, baseColor, radius, width, running)` — circling border highlight.
- `Modifier.riseIn(delayMillis, durationMillis, distance)` and `DEFAULT_RISE_MILLIS` — enter motion.
- `FlipState`, `AnimatedVisibilityScope.rememberFlipState(...)`, `Modifier.flip(state)`, `DEFAULT_FLIP_*` constants — flip enter and exit.
- `Modifier.fadingEdges(top, bottom)`, `Modifier.horizontalScrollFade(state, width, color)` — edge fades.
- `throttledProcess(minIntervalMillis, action)` — composable that returns the throttled callback.
### Not covered
- The glass navigation press look (the `glass-nav` bar and rail draw their own pill and deliberately do not use `PressIndication`; the scroll-to-top button does).
- When to run or skip a motion for reduced motion — callers, using `rememberReduceMotion` from `theme`.
- The throttle clock and rule itself — `common`.
