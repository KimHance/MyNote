---
units: [ui:parts, ui:bookmark-time]
plans: [004-bookmarks, 007-repo-connect, 012-feature-modules]
decisions: [D-23, RC-D46, RC-D49, RC-D62, FM-D9, DEC-15]
---
## parts — Shared screen parts
### Behaviour
- Five buttons: `PrimaryButton` (accent fill, 40 dp), `SecondaryButton` (surface fill with border, 40 dp, optional leading and trailing icon), `DestructiveButton` (text-only in the wrong color, 40 dp), `SolidDestructiveButton` (wrong-color fill, no `enabled` parameter) and `QuietButton` (36 dp, secondary text, optional trailing icon and an accessibility name that differs from the label).
- A disabled button shows at 40% opacity. A label is one line, never wraps and is clipped; icons beside a label are decorative and not announced. Primary and solid buttons shrink to 0.98 while pressed.
- `CONTENT_MAX_WIDTH` is 560 dp, the one-column width of stacked screens such as connect.
- `KeepWordsText` puts a word joiner between two adjacent Hangul syllables so a line breaks only at spaces and never inside a word. The text shown carries the joiners, the accessibility text does not, and style spans keep their ranges.
- `monoAnnotated` fills a string resource with arguments and sets each argument in the mono font, searching each from the end of the previous one, so a repository name or a Korean folder stays highlighted at the right range.
- `ProgressRing` (16 dp by default) and `ProgressBar` animate; with reduced motion they draw a still ring at 30% and a still bar at 35% instead.
- `FieldBox` is a 48 dp field frame with rounded corners. Its border is the accent color at half strength, the warn color at half strength when `warn`, and the plain border when `locked`; a locked field is also dimmed to 55%.
- `TokenField` hides the text with dots unless the draft is revealed, uses a password keyboard with autocorrect off and a Done key, shows the placeholder `github_pat_…`, and has a quiet show or hide button named `토큰 보기` or `토큰 숨기기`. Done sends the token only when it is not blank; a locked field also disables the button (007-repo-connect: RC-8).
- `TokenHowTo` shows three guidance lines: type fine-grained token, scope this one repository, permission read-only contents. `TokenCreateButton` is a secondary `토큰 만들기` with an external-link icon (007-repo-connect: RC-8).
- `NoticeBox` is a warn-tinted box with an alert icon, a message and an optional second line, announced politely. An optional row of ways (buttons) sits under a divider, outside the announced text, and wraps to the next line when it does not fit (007-repo-connect: RC-9).
- `VerifyingLine` is a ring, a line of text and an optional trailing control, such as `취소` (007-repo-connect: RC-24).
- `ActionDialog` is a title (heading), a body and two buttons, `취소` and a confirm that is solid red when destructive and primary otherwise; the buttons are right-aligned and wrap to a second line when narrow, and long text scrolls.
- `GlassDialog` is the frame under `ActionDialog` and the device-note dialogs: inside the safe area with a 24 dp margin, a glass panel with radius 26 and width at most 420 dp, centred, with the content stacked inside padding. It does not sample a backdrop, so no glass samples itself.
- `openExternal(context, url)` opens a browser app: the default browser, else Chrome, else the first browser found; a default that is the system chooser is skipped so apps that claim the address are not offered. If starting that browser fails it falls back to a plain view intent; it returns false when nothing could open (007-repo-connect: RC-8) (012-feature-modules: FM-K13).
- Finding browsers needs the `<queries>` entry in this module's manifest; without it the browser pick silently becomes a plain open (012-feature-modules: FM-K13).
- `TOKEN_PAGE_URL` is the GitHub new fine-grained token page.
- `ConnectIcons` are seven hand-drawn stroke icons: folder, download, lock, external, chevron down, chevron up, alert.
### Public API
- `PrimaryButton` · `SecondaryButton` · `DestructiveButton` · `SolidDestructiveButton` · `QuietButton` · `CONTENT_MAX_WIDTH`.
- `KeepWordsText` (String and AnnotatedString forms) · `monoAnnotated(res, args)`.
- `ProgressRing` · `ProgressBar` · `FieldBox` · `TokenField` · `TokenHowTo` · `TokenCreateButton` · `NoticeBox` · `VerifyingLine`.
- `ActionDialog` · `GlassDialog` · `openExternal` · `TOKEN_PAGE_URL` · `ConnectIcons`.
### Guarantees
- Joiners only between Hangul syllables, plain accessibility text, and exact mono ranges are fixed by `KeepWordsTest`.
- The browser order (default, Chrome, first found, none) is fixed by `ExternalLinkTest` (007-repo-connect: RC-8).
### Not covered
- The token state, drafts and failure sentences — `ui:token`.
- Which screen shows which box or dialog — `connect`, `settings`, `notes`, `editor`.
- The glass material — `design-system:glass-nav`; the colour and radius tokens — `design-foundation:palette`.
- Model card tones and the preview frame — `ui:model-status`.

## bookmark-time — Bookmark time label
### Behaviour
- The label is chosen by calendar day in the device time zone: the same day is today, the day before is yesterday (also across New Year), the same year is this year, anything else is another year (004-bookmarks: BM-11).
- The short form: today `HH:mm`, yesterday `어제`, this year `M월 d일`, another year `yyyy. M. d.` (004-bookmarks: BM-11).
- The long form: today `오늘 HH:mm 북마크`, yesterday `어제 HH:mm 북마크`, this year `M월 d일 북마크`, another year `yyyy년 M월 d일 북마크` (004-bookmarks: BM-11).
- The card uses the long form and rows the short form (004-bookmarks: BM-11).
- Today is read once per composition and is not refreshed at midnight while the screen stays open.
- Formats come from string resources and use the Korean locale.
### Public API
- `rememberBookmarkTimeLabel(addedAt, long)` — the label for an epoch-millisecond time.
### Guarantees
- Today, yesterday, this year, another year, New Year's edge and time-zone dependence are fixed by `BookmarkTimeKindTest` (004-bookmarks: BM-11).
### Not covered
- Storing bookmarks — `database:bookmarks`; the repository contract — `domain:bookmarks`; ordering — `notes:bookmarks`.
- The bookmarks screen that draws the label — `notes:bookmarks`; the stats list reuses it — `stats:screen`.
