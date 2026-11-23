---
units: [stats:summary, stats:history]
plans: [005-stats, 010-brand]
decisions: []
---
## summary — Weekly summary card
### Behaviour
- The card is absent when the model is not ready or no summary is cached; it is never a placeholder (005-stats: ST-14, ST-21).
- The label reads "이번 주 요약" for this week, "지난주 요약" for last week, and "<range> 요약" for any older week, the range shortened to "M월 d일 – d일" inside one month (005-stats: ST-21).
- The meta reads "오늘 정리됨", "어제 정리됨", "<M월 d일> 정리됨" for 2 to 9 days and "N일 전 정리됨" from 10 days; days are calendar-date differences in the device time zone, never 24-hour units (005-stats: ST-21).
- From 10 days a summary is stale: smaller and quieter type, no focus line, and the focus is not spoken (005-stats: ST-21).
- While the worker writes a new summary the card keeps the earlier content and adds a line with a spinning ring "새 기록으로 다시 정리하는 중입니다." (005-stats: ST-21).
- A failure line "요약을 새로 만들지 못했습니다. 다음 충전 때 다시 시도합니다." shows only when the last failure is newer than the summary and nothing is running; running wins over failure; a give-up alone is not a failure; there is no retry button (005-stats: ST-20, ST-21).
- The status line is a polite live region, so the change is announced.
- The card body is not tappable; only the meta button (44 dp touch height, read as "지난 요약 보기, <meta>") opens past summaries (005-stats: ST-21).
- A fresh summary with a focus category shows "초점" and the category chip under the detail; the legend of the reading section marks the focus only on the week period, for this week's summary and when not stale (005-stats: ST-21).
- The card comes in three forms: regular, wide (two columns) and short (low height: smaller type, headline and detail at most two lines each, no focus line).
- The card expands in over 240 ms and shrinks out over 180 ms, resizes over 300 ms and cross-fades its text over 200 ms when a new summary lands; all are instant with reduced motion, and the running ring stands still at half opacity.
- The card is read as one item: label, headline, detail, focus and status.
- The wrong-answer pattern ("틀린 문제 패턴": tags, one sentence and the generated meta) shows in the quiz section only when the model is ready and the pattern is less than 10 days old (005-stats: ST-23).
- No summary or pattern is shown without the downloaded model, even when rows are stored (005-stats: ST-14).
### Public API
- `SummaryCard(summary, form, formats, onOpenHistory)` — the card for one summary; public because the `app` title-font test draws it.
- `SummaryForm` — regular, wide and short layouts of the card.
- `GeneratedAgo` — today, yesterday, a recent date or stale days, with `GeneratedAgo.of(generatedAt, today, zone)`.
### Guarantees
- No card without model or cache, the three labels, the 9-day and 10-day border, running over failure, failure only when newer, give-up is not failure, legend focus rules and the pattern's visibility (`SummaryCardRulesTest`).
- Calendar-date differences and the time zone decide the meta (`GeneratedAgoTest`).
- The card keeps its line counts in the title font (`TitleFontScreensTest` in `app`) (010-brand: BR-8).
### Not covered
- Writing summaries and patterns, their guards (no digits, Korean, length) and when the worker runs — `ai` and `domain`.
- Rows from another model or output version are filtered before they reach the card — `database`.
- The pattern card's place in the quiz section — see `screen`.

## history — Past summaries
### Behaviour
- Past summaries opens from the summary card's meta button and is a back-stack screen; back pops it (005-stats: ST-22).
- The list starts with "주마다 마지막으로 정리한 요약 하나를 남깁니다." and then one row per week as the domain gives it: from the first record to this week, at most 52, newest first (005-stats: ST-22).
- A row shows the week's date range, an "이번 주" chip on the current week, the headline in at most two lines or "이 주에는 요약이 없습니다.", and the week's numbers "노트 N개 · M분 · 정답률 A%" or "노트 N개 · M분 · 퀴즈 0회" when no question was asked (005-stats: ST-22).
- The numbers are the week's own recomputed figures (this week so far), not the summary's (005-stats: ST-22).
- A row with a summary toggles open and closed (chevron turns over 200 ms, height animates over 240 ms, instant with reduced motion) and then shows the full headline, the detail, the focus chip and the generated date (005-stats: ST-22).
- A row without a summary is not a button.
- Open rows are remembered per week in saved state while the screen lives, so rotation and folding keep them.
- A row is read as one item: range and headline (or no summary), the opened parts, and the numbers, with the state "펼침" or "접힘" on rows that have a summary.
- Until the list arrives only the intro line shows.
- The list keeps its scroll position in the shell's scroll state for past summaries.
### Public API
- None — the screen is registered through `statsDestinations` in `screen`.
### Not covered
- Building the week rows and their figures — `domain`.
- The top bar with the back arrow, the title "지난 요약" and no refresh button — the `app` shell.
### Unconfirmed
- No automated test drives the past-summaries screen; the row rules above are read from the code.
