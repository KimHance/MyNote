---
units: [domain:stats, domain:insights]
plans: [005-stats, 013-local-notes]
decisions: [D-45, D-46, D-47]
---
## stats — Study statistics
### Behaviour
- Statistics are counted in code, not in the database: the repository hands over raw records and this unit computes every number, total and list. The thresholds live in one place (`StatsThresholds`).
- Three periods exist: this week, this month and all. A period gives a start, an end (now), an optional comparison span, a list of buckets for the bars, a first-week flag and a short-history day count. All dates and day starts use the time zone of the `now` value given, so a daylight-saving day is a 23-hour or 25-hour bucket and the same instant can fall on different days in different zones (005-stats: ST-10).
- This week starts Monday 00:00 and has seven day buckets, Monday to Sunday. It is the first week when the first record is on or after this Monday (005-stats: ST-10).
- The week comparison runs from last Monday 00:00 to the same weekday and time one week before now, so both sides cover the same stretch. There is none when the first record is later than last Monday, or when there are no records (005-stats: ST-9, ST-10).
- This month starts on day 1 and its buckets are the Monday-based weeks cut at the month borders, four to six of them. The comparison runs from the first day of last month to the same day of month and time; when today's day number does not exist last month, it runs to the end of last month. There is none when the first record is later than the first day of last month (005-stats: ST-9, ST-10).
- All has monthly buckets from the first record's month to this month, the latest 12 only, but its numbers cover the whole history from the first month; it has no comparison (005-stats: ST-10).
- Month and all report a short-history day count when the first record is less than 14 days old (today counted): 13 days is short, 14 is not. This week never does (005-stats: ST-10).
- A bucket is current when today is inside it; with no records every other bucket is before-record; otherwise it is future when it starts after today, before-record when it ends on or before the first record's date, and past in the other cases (005-stats: ST-10).
- Four numbers are computed for the period and, when a comparison exists, for the comparison span: notes read (distinct notes with a session started in the span), minutes, quizzes finished, and questions asked and answered right. The difference between the two is left to the screen (005-stats: ST-9).
- Minutes are the sum of active time divided by 60,000 and rounded down once for the sum, not per session. Accuracy is correct over asked as a whole percent rounded half up (2 of 3 is 67) and is none when nothing was asked (005-stats: ST-9).
- Deleted notes still count in the numbers; the lists below show only notes that still exist, so a deleted device note leaves them (005-stats: ST-14) (013-local-notes: LN-11).
- Records and notes whose keys carry a source prefix are counted like any others, because ids are opaque strings.
- The category axis is the union of the categories of the current notes and of the period's sessions, attempts and bookmark events (empty names left out), sorted by name. Each category keeps its axis index so colors stay stable between charts.
- Reading time per bar counts a session on the date it started. Category minutes are sorted by minutes, largest first, then axis order; their sum can be lower than the total because each category is rounded down on its own (005-stats: ST-11).
- Stalled notes: per note the latest session over all time (by its end time) counts when it started inside the period and reached less than 90 percent; newest first, at most 5, existing notes only (005-stats: ST-11).
- Accuracy by category sums asked and correct per category and sorts by accuracy, lowest first, categories with no answers last, ties by axis order. The lowest flag goes to every category sharing the minimum, and only when at least two categories have answers (005-stats: ST-12).
- Retried notes: a note with at least two attempts ever and one inside the period gives its first attempt's accuracy, its latest attempt's accuracy and the number of that latest attempt over all time; newest first, at most 5, existing notes only; it counts as improved when the latest is higher than the first (005-stats: ST-12).
- Bookmark bars count added and removed events per bucket by the event's date, and the period totals count the same events (005-stats: ST-13).
- Stale bookmarks ignore the period. With no first record there are none. For each existing bookmarked note, oldest added first, the base time is the end of its latest session, or when it was never read the later of its added time and the first record; it is listed when the base is more than 14 calendar days before today, at most 5 (005-stats: ST-13).
- A stale bookmark carries its reason: last opened N days ago; never read, added since the log began; or never read, added before the log began. Day counts are differences of calendar dates in the zone, never 24-hour units (005-stats: ST-13).
- Observing restarts when the first record time changes, reads records from the comparison start (or period start) with no upper bound, and recomputes when records, note facts, notes or bookmarks change. "Now" is given by the caller; the module does not tick over midnight.
### Public API
- `StatsPeriod` (`Week`, `Month`, `All`), `StatsPeriodRange.of(period, now, firstRecord)` giving `StatsRange`, `Bucket` (`contains`), `BucketState`.
- `StudyStatsCalculator.compute(range, now, firstRecordAt, window, facts, notes, bookmarks)` giving `StudyStats`, and its parts: `StatsTotals` (`accuracy`), `BucketMinutes`, `CategoryMinutes`, `StalledNote`, `CategoryAccuracy`, `RetryTrend` (`improved`), `BookmarkBucket`, `StaleBookmark`, `StaleReason`, `NoteRef`.
- `ObserveStudyStatsUseCase` — `(period, now)` gives a flow of `StudyStats`.
- `StatsThresholds` — the constants of this unit and of unit `insights`.
### Guarantees
- Period and comparison boundaries (Monday starts, month borders, leap February, year-crossing weeks, 13 versus 14 days, 13 months cut to 12, daylight-saving days, other zones) are covered by `StatsPeriodRangeTest`.
- Totals, rounding, lists, ordering, caps, the axis and the stale-bookmark reasons are covered by `StudyStatsCalculatorTest`, including that deleted notes leave the lists.
### Not covered
- Reading records from storage — `data` and `database`.
- How numbers, changes and wording are shown, the "first week" label and the short-history wording — the stats feature module.
- Keeping the aggregation attached only while the screen is on top — the stats feature module and `app`.

## insights — Weekly summaries and patterns
### Behaviour
- The plan decides what the daily worker builds, from facts only, and runs before the engine is woken. It returns at most three targets in this order: last week's summary, this week's summary, the quiz pattern (005-stats: ST-16).
- A span has "enough" when it holds at least 3 started sessions or at least 2 finished quizzes (005-stats: ST-16).
- Last week's summary is planned only when last week had enough, and either none exists or the existing one was made before the week ended and has at least one event after its creation time in last week. A summary made after the week ended is final and not made again (005-stats: ST-16).
- This week's summary is planned as the first one when none exists and this week has enough. An existing one is refreshed only when at least 20 new events came after it and at least 24 hours passed; an event is a session end, a quiz finish or a bookmark event (005-stats: ST-16).
- The pattern is planned when there are at least 3 wrong answers in the last 28 days and either no pattern exists, or at least 3 new wrong answers came after it and at least 24 hours passed (005-stats: ST-16).
- The digest is the whole input of a summary and holds every number the model may say; the model copies and never calculates, and the output is checked against this set elsewhere.
- The digest covers the week from Monday to the end of the week or now, whichever is earlier, and compares it with the previous week over the same length. It has no previous figures when the first record came at or after the end of the previous week's first day.
- Digest figures are notes, minutes, quizzes and accuracy for this span and the previous, and the difference of each; accuracy differs only when both sides have one. Days with records counts distinct dates in the zone, days left counts the days after today in the week and is 0 for a finished week.
- Categories in the digest are the union of notes, sessions and attempts, sorted by name, with minutes, correct and asked. The top category has the most minutes (more than zero). The least category has the fewest minutes, ties by fewer questions then name, needs two or more categories and is none when it would equal the top.
- The trend is more, less or same from the sign of the minutes difference, and none without a comparison. Up to 3 stalled titles (latest session this week below 90 percent, newest first) and one re-read title (the note with most sessions, at least two, ties by minutes) are named; notes that no longer exist are left out.
- Past weeks list one row per week from the first record's week to this week, newest first, at most 52 (the first week is cut to 51 weeks before this one). A week without records is still a row (005-stats: ST-22).
- Each row recomputes its numbers from raw records: notes and minutes by the week a session started, quizzes and accuracy by the week an attempt finished, plus a current-week flag and the summary of that week when one exists. A week with none reads as having no summary (005-stats: ST-22).
- With no record at all the past-weeks list is empty; otherwise it follows both the records and the summaries (005-stats: ST-22).
- The latest-insight snapshot combines the latest summary, the latest pattern, the time of the last failure and a "generating" flag, and repeats nothing when equal. Summaries and patterns are requested with the scheduler's stamp (model commit and output version), so those of another model or output version are not shown (005-stats: ST-20).
- Failure kinds are parse, engine and timeout; the worker's last result is success, failure or skipped, and a skipped give-up is never shown as a failure. Failure types carry English diagnostics (005-stats: ST-20).
### Public API
- `InsightPlan.of(facts)` giving a list of `InsightTarget` (`Summary(weekStart, kind)`, `Pattern(weekStart)`), `InsightFacts`, `SummaryKind`.
- `WeeklyDigestBuilder.of(weekStart, now, window, notes, firstRecordAt)` giving `WeeklyDigest` (`top`, `least`, `trend`, `titles`, `categoryIds`), with `DigestTotals`, `DigestCategory`, `DigestTrend`.
- `WeeklyHistory` (`firstWeek`, `of`, `mondayOf`) and `HistoryWeek`.
- `ObserveInsightsUseCase`, `ObserveWeeklyHistoryUseCase`.
- `InsightStamp`, `WeeklySummary`, `QuizPattern`, `InsightResult`, `InsightFailureKind`, `InsightStatus`, `InsightWorkState`, `InsightSnapshot`, `WrongAnswer`, `InsightFailure` (`Parse`, `TooLong`, `HasDigits`, `MissingField`, `Engine`, `Timeout`).
- `InsightRepository` — summaries, pattern and status reads and writes, epoch, clear, prune; implemented in `data`. `InsightScheduler` — stamp, schedule, cancel, state; implemented in `ai`.
### Guarantees
- Thresholds, refresh conditions, the last-week finalising rule and the three-target order are covered by `InsightPlanTest`; digest figures and the end-of-week and no-comparison cases by `WeeklyDigestTest`; row order, the 52-week cut and a full run of weeks by `WeeklyHistoryTest`.
### Not covered
- Generating the text, the prompt, the output guards, retries and the worker — `ai`.
- Storing summaries, patterns and status — `data` and `database`; clearing and pruning — unit `study-log`.
- The summary card and the past-weeks screen — the stats feature module.
