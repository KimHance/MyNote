---
units: [stats:stats, stats:screen]
plans: [005-stats, 010-brand, 012-feature-modules]
decisions: [D-33, D-59, D-62]
---
## stats — Stats state and figures
### Behaviour
- The period (this week, this month, all) and the section (reading, quiz, bookmarks) start at week and reading and are kept in saved state, so they survive detours, rotation, folding and process death (005-stats: ST-9) (012-feature-modules: FM-K4).
- "Today" is fixed when the Stats or past-summaries screen is entered and does not tick over midnight while the screen stays open.
- The aggregation and the summary and pattern streams are attached only while Stats is on top of the back stack, and the history list only while past summaries is on top; otherwise nothing is observed, so no store query runs while a reader is open (005-stats: ST-15) (012-feature-modules: FM-K9).
- Returning to Stats attaches again and recomputes; figures then follow the store by themselves, with no refresh needed (005-stats: ST-8).
- Aggregation runs off the main thread; in debuggable builds each run logs its start and its duration under the tag `MyNoteStats`.
- Until the first aggregate arrives the state has no content and the screen draws nothing (005-stats: ST-14).
- An aggregation error is logged and the last figures stay on screen; there is no retry and no error message.
- The four numbers are notes read, minutes read, quizzes and accuracy; accuracy is empty when no question was asked (005-stats: ST-9).
- Change per number: with no record at all every change is a dash and the caption reads "기록이 쌓이면 지난주와 비교합니다" (005-stats: ST-14).
- Change on the all period is blank and the caption reads "<first record date>부터" (005-stats: ST-10).
- Change in the first week of records reads "첫 주" for the counts and "N문제" for accuracy (005-stats: ST-10).
- When the comparison start predates the first record the change is a dash and the caption reads "<first record date>부터" (005-stats: ST-10).
- Otherwise the change is the difference against the previous period up to the same moment, with a real minus sign, a plus sign, and "%p" for accuracy; the caption reads "지난주 월–<day> 대비" or "지난달 1–<d>일 대비" (005-stats: ST-9).
- A period note shows on month and all when the history is under 14 days: "<date>부터 N일 치 기록입니다.", plus "이번 주와 거의 같습니다." at 7 days or fewer (005-stats: ST-10).
- Chart buckets are days on week, Monday-based weeks on month and months on all; buckets before the first record and in the future are drawn as pending (005-stats: ST-10).
- The category total beside the reading charts is the sum of the legend minutes and can differ by a minute from the minutes number.
- A retried-quiz row is marked improved when the latest accuracy is above the first.
- Opening a note from a list opens the reader by destination key; the reader-opened signal then carries the resume point (stalled notes: the stopped percentage) or the quiz request (retried quizzes) to `notes`; a move dropped by the navigation throttle sends nothing (005-stats: ST-11, ST-12) (012-feature-modules: FM-6).
- The summary card's meta button pushes past summaries (005-stats: ST-21).
- An exception while handling an intent is logged through the trace.
### Public API
- `StatsViewModel` — Hilt view model obtained with the Activity as owner; state `StatsUiState`, input `StatsIntent`.
- `StatsUiState` — period, section, log-enabled flag, summary card, content, pattern and history.
- `StatsIntent` — `SelectPeriod`, `SelectSection`, `Entered`, `OpenHistory`, `OpenNote(noteId, resumeAt)`, `RetryQuiz(noteId)`.
- `StatsSection` — reading, quiz, bookmarks.
- `StatsContentUi` and its parts — `PeriodNoteUi`, `StatKind`, `StatNumberUi`, `DeltaUi`, `ComparisonUi`, `BucketLabel`, `TimeBarUi`, `CategoryMinutesUi`, `StalledNoteUi`, `ReadingUi`, `AccuracyRowUi`, `RetryUi`, `QuizStatsUi`, `BookmarkBucketUi`, `StaleReasonUi`, `StaleBookmarkUi`, `BookmarkStatsUi`.
- `SummaryLabel`, `SummaryCardUi`, `PatternUi`, `HistoryWeekUi` — screen models of the summary card, the pattern and one history week.
- `StatsTrace` — start, done and failure hooks; `LogStatsTrace` logs them (start and done only in debuggable builds).
- `StatsWork` — qualifier of the aggregation dispatcher; `StatsDispatcherModule` provides the default one.
### Guarantees
- The mapping of deltas, comparison captions and period notes over first record, first week and short history is pinned (`StatsUiMapperTest`).
- Aggregation time is measured with the injected clock and "today" comes from the injected wall clock (`StatsViewModelTest`).
- Opening a note or retrying a quiz from Stats opens the reader and, for a retry, the quiz setup (`FeatureSignalsTest` in `app`).
### Not covered
- Period ranges, totals, the stalled, accuracy, retry and stale-bookmark lists, the 14-day and 90-percent thresholds and the exclusion of deleted notes from lists — `domain`.
- Turning the setup request into an open quiz only when the model is ready — `notes` and `quiz`.
- Keeping aggregation alive after leaving Stats is deliberately not done; a lazily shared stream would keep querying until the app ends.
### Unconfirmed
- Whether figures resume updating after an aggregation error without recreating the view model was not traced; as read, the error ends the observed stream.

## screen — Stats screen
### Behaviour
- `statsDestinations` registers `Stats` and `StatsHistory`; both screens take the view model with the Activity as owner and the shell's scroll states, so positions survive folding and returning (005-stats: ST-24).
- One column: a list capped at the reading width with fading edges, holding in order the top item (log-off or intro notice, summary card, period segment), the period note, the numbers, the section chips, the section charts and the section lists.
- The summary card sits inside the top item, so a card that appears after a worker run does not scroll off above it (005-stats: ST-21).
- Two columns follow the shell's decision shared with Bookmarks: left and right split 1.15 to 1 with a 28 dp gap (24 dp on low height); the left holds the top item, numbers, chips and charts, the right the section lists (005-stats: ST-24).
- On low-height windows in two columns the chips, charts and lists all move to the right column under their own list keys, so the right list does not jump on a fold change; low height also uses the short chart and card forms (005-stats: ST-24).
- Switching between one and two columns cross-fades (160 ms out, 200 ms in; instant with reduced motion).
- When the study log is off, "학습 기록이 꺼져 있습니다. 설정에서 다시 켤 수 있습니다." shows on top; otherwise with no record at all and no summary card, "노트를 읽거나 퀴즈를 풀면 여기에 기록이 쌓입니다." (005-stats: ST-5, ST-14).
- The period segment and the section chips are always shown and selectable whatever the data; numbers follow only the period (005-stats: ST-9).
- The segment highlight slides over 220 ms and chip colours change over 160 ms, both instant with reduced motion; chips are 36 dp tall inside a 44 dp touch height.
- Charts and lists cross-fade over 150 ms when the section changes.
- Reading: time bars titled "요일별 · 주별 · 월별 읽은 시간", a category card with a donut, total and legend (a single stacked bar with the total in its header on low height), and "읽다 멈춘 노트" with up to 5 rows "category · N%에서 멈춤 · time" and a progress track (005-stats: ST-11).
- Category colours go by index: accent, accent 2, accent 3, text 2, text 3; from the sixth category and for zero minutes the swatch is hollow.
- The legend marks a category "초점" only when the summary card allows it and that category is in the legend (005-stats: ST-21).
- Quiz: "카테고리별 정답률" with the total question count and one bar per category (percentage, or a dash and "풀지 않음"), the lowest highlighted; the pattern card when present; then "다시 푼 퀴즈" ("첫 시도 → 최근") with rows "category · N회째" and "a → b%", the latest in accent when improved (005-stats: ST-12, ST-23).
- Bookmarks: "추가 · 해제" diverging bars with added and removed counts, then "오래 안 본 북마크" ("14일 넘게") with rows giving the reason (last opened N days ago, not read yet, not opened since logging began) and the age in days (005-stats: ST-13).
- Every empty list shows one line instead of rows ("읽다 멈춘 노트가 없습니다." and the like).
- Tapping a stalled row opens the reader near where it stopped; a retried-quiz row opens the reader and asks for the quiz setup; a stale bookmark opens the reader (005-stats: ST-11, ST-12, ST-13).
- Row times use the shared short time label of the bookmarks screen.
- Each number cell is one spoken item: label and value plus the change in words ("지난주보다 X 많음", "지난달과 같음" and the like) (005-stats: ST-25).
- Charts are spoken as text: bar values per bucket with "기록 전" and "아직 없음" buckets named, the category total and parts, bookmark counts per bucket; chart titles are headings; list rows read "N번째, title, subtitle" (005-stats: ST-25).
- The header number uses the system font at 24 sp with a 30 sp line, not the title font (010-brand: BR-8).
### Public API
- `statsDestinations()` — registers the `Stats` and `StatsHistory` destinations.
- `StatsFormats` and `rememberStatsFormats()` — Korean date, week and month formatters for stats labels.
### Guarantees
- The header number style keeps its pre-title-font shape (`StatsHeaderValueTest`).
- No automated UI test drives the Stats screen; `TitleFontScreensTest` in `app` only draws the summary card.
### Not covered
- The top bar title, refresh and back, the floating navigation and the two-column decision — the `app` shell and `ui`.
- The chart drawings and their progress animation (jump to the end with reduced motion) — `design-system`.
- Figures, deltas and captions — see `stats`; the summary card and pattern rules — see `summary`; past summaries — see `history`.
