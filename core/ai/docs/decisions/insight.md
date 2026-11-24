# Decisions

### D-53 · 2026-10 · ST-16, ST-17
- decision: The worker is a daily periodic job whose plan function, not the period, decides "once a week plus a data threshold"; it only checks facts on most days and wakes the engine only when something is to be written; it is scheduled by the app module while the model is ready and logging is on.
- why: A seven-day period could fire on a Monday morning when there is nothing to summarise.
- rejected: A seven-day period.
- evidence: `StudyInsightWork`, `InsightPlan`, `InsightPlanTest`, `app/.../InsightScheduleSync.kt`.

### D-56 · 2026-10 · ST-17, ST-18
- decision: No foreground service and no expedited work: an ordinary worker finishes inside the scheduler window with a 3-minute overall limit.
- why: On Android 12 and later a background app cannot start a foreground service, and expedited work cannot require charging; it also keeps results silent.
- rejected: A data-sync foreground service; expedited work.
- evidence: `StudyInsightWorker` (timeout), `StudyInsightWork` (constraints).

### D-57 · 2026-10 · ST-19
- decision: Output is one JSON object with fixed fields and lengths; over-length output is retried once with a shorter instruction and otherwise rejected, never truncated; fields the screen has no place for are not requested.
- why: A truncated sentence on a card is worse than the previous good summary; the retry changes the instruction because decoding is greedy.
- rejected: Cutting long text; asking for extra fields (mood, advice, note ids).
- evidence: `InsightJsonParser.kt`, `InsightJsonParserTest`.

### D-59 · 2026-10 · ai:insight-worker, ST-21
- decision: "Generating" is shown only when the unique work is running and has reported the generating phase just before taking the engine.
- why: The daily check also runs and ends within milliseconds; flashing "regenerating" for it would be false.
- rejected: Treating any running state as generating.
- evidence: `StudyInsightWork.state`, `InsightWork.PHASE_GENERATING`.
