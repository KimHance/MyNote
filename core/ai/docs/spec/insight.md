---
units: [ai:insight-generation, ai:insight-worker]
plans: [005-stats]
decisions: [D-53, D-56, D-57]
---
## insight-generation — Insight generation
### Behaviour
- Two kinds of text are written: a weekly summary (headline, detail, optional focus category) from a digest, and a wrong-answer pattern (up to three tags and one sentence) from the learner's wrong quiz answers (005-stats: ST-19).
- Each call opens a fresh conversation: thinking off, greedy sampling, a system line and an English instruction with one worked example whose values are not in the app; the answer is asked in polite Korean. The output budget is 69 tokens for a summary and 50 for a pattern, about 1.5 times the measured p95 (46 and 33), so the budget is the real length limit (005-stats: ST-19).
- The summary input is a JSON digest built from `domain` figures. The model is told to write no digits and to say amounts in words using the digest's trend (more, less, same); the numbers shown on the card come from the app, never from the model (005-stats: ST-19).
- The model is asked for a headline of at most 40 characters and a detail of at most 80; the parser limits are looser: headline 48, detail 100, pattern sentence 120, tag 12 characters, at most 3 tags. A longer value is never cut; it is rejected as too long (005-stats: ST-19).
- Focus is accepted only when it equals one of the digest's category ids (any letter case, returned in the digest's spelling); anything else becomes no focus (005-stats: ST-19).
- The reply is read from the first `{` to the last `}`; text around it is ignored; line breaks and runs of spaces inside values become one space; nested objects are ignored. No object is a parse failure; a missing headline, detail, sentence or tags is a missing-field failure (005-stats: ST-19).
- Output with an unclosed brace outside strings is treated as cut off and rejected as too long, whatever else is wrong with it; it is judged by structure, never by token count, so it works in release builds (005-stats: ST-19).
- A tag longer than 12 characters is dropped and the others kept; if all are too long the output is too long (005-stats: ST-19).
- Summary checks run in this order and reject: no Hangul at all; a data key name (`least`, `top`, `stalled`, `reread`, `daysLeft`, `prev`, `change`, `trend`, `minutes`, `quizzes`, `accuracy`) in the sentence after note titles and category ids are removed; a copy of the example (the same text, or words that exist only in the example and not in the input); any digit left after titles and category ids are removed (005-stats: ST-19).
- Pattern checks reject: no Hangul; a number that is not in the wrong answers' text; a copy of the example; a sentence sharing no key word with the wrong answers (key words drop particles and common filler words) (005-stats: ST-19).
- Pattern tags are kept only if they are found in the wrong-answer text (category, note title, question, picked and correct choices) and also appear in the sentence. A Latin tag needs three or more letters and must match as a whole word; a one-syllable Korean tag must appear in both texts. If none remain, the sentence's own grounded key words of at most 12 characters (up to 3) become the tags; if still none, the output is rejected (005-stats: ST-19).
- The pattern prompt lists the wrong answers as numbered lines, each clipped (question 80, choice 40, note title 30 characters) and trimmed from the end to about 1,500 characters but never below 3 lines (005-stats: ST-19).
- A too-long, a digit, or a missing-field result gets exactly one more attempt with a one-line corrective instruction (shorter; no digits; complete); other rejections (not Korean, copied example, leaked key, ungrounded) are final with no retry. A second failure of a retryable kind ends as a parse failure "still … after retry" (005-stats: ST-19).
- An exception from the engine ends the run as an engine failure without retry; cancellation is rethrown.
- A run returns the value or the failure plus the list of attempts (raw reply, prompt size, time, tokens, verdict) for the evaluation tests and the log.
### Public API
- `InsightGenerator` — `summary(engine, digest)` and `pattern(engine, wrongAnswers, titles)` each return an `InsightRun`; budgets are `SUMMARY_TOKENS` 69 and `PATTERN_TOKENS` 50.
- `InsightRun` and `InsightAttempt` — the value or failure and the attempt records.
- `SummaryDraft`, `PatternDraft` — the accepted summary and pattern text.
- `Fix` — the corrective instruction added on the second attempt.
### Guarantees
- Length limits, lenient reading, truncation detection and missing fields are fixed by `InsightJsonParserTest`; digit, key-leak, example-copy, language and tag-grounding rules by `InsightGuardsTest`.
### Not covered
- Deciding which weeks and patterns to write, and building the digest — `domain` stats.
- Saving, overwriting and hiding results by model or output version — `database` and `domain`; the output version number lives in `ai:insight-worker`.
- Showing the card — `stats`.

## insight-worker — Insight worker
### Behaviour
- A daily periodic work named `study-insights` runs only while charging and with battery not low; a give-up (see below) is retried with a 30-minute linear backoff. It shows no notification and is not a foreground service (005-stats: ST-17).
- Scheduling is skipped when an unfinished work with the current spec tag is already queued, so starting the app does not cancel and re-register the job. Changing the request means raising the spec tag, which updates once (005-stats: ST-17).
- "Charging" means the battery level is rising: a low-current USB link may not count (005-stats: ST-17).
- Each run reads the last two weeks of records and the wrong-answer counts (a pattern is written from at most 8 wrong answers of the last 28 days), asks `domain` for a plan (finalize last week, write this week, find a pattern: at most three jobs in that order) and ends quietly with success when the plan is empty, without touching the engine (005-stats: ST-16).
- The worker never waits for the engine. It gives up with a retry, writes a skipped time and counts no failure when the app is in the foreground, a model download work is unfinished, or the engine lock is taken by a quiz (005-stats: ST-18, ST-20).
- The foreground check runs on the main thread against the process lifecycle (started or above) (005-stats: ST-18).
- Between jobs the foreground check is repeated; if the app came forward, the remaining jobs are not started, what was finished stays saved and the run is retried later (005-stats: ST-18).
- An engine the worker built itself is closed at the end when the app is in the background and kept when it is in the foreground; an already loaded engine is never closed by the worker (005-stats: ST-18).
- A run is limited to 3 minutes. On timeout the work already saved is kept; a failure of kind timeout is recorded against the target that was running, and a timeout during the pattern does not mark the summary as failed. Cancelling stops the native decode (005-stats: ST-18, ST-20).
- A generation failure (parse, too long, digits, missing field, engine, timeout) is stored with its time and kind (engine and timeout kept as such, the rest as parse) and the run still ends as success, so the next charge plans again; an engine failure stops the remaining jobs (005-stats: ST-20).
- A saved summary or pattern is written with the output-version stamp (model commit and output version 2); the result of a job overwrites that week's entry (005-stats: ST-20).
- If the user cleared the records while the run was in progress, the result is thrown away and nothing is written (005-stats: ST-6).
- After a save, old summaries are pruned by the cutoff `domain` defines. A run that saved something and also failed or was interrupted records the success time as well as the failure (005-stats: ST-7, ST-20).
- The output version is 2: summaries stored under any other output version are filtered out downstream and the week reads "no summary". Raise it whenever the output format or prompt contract changes (005-stats: ST-19, ST-20).
- The scheduler also reports whether a run is generating (running with the generating phase), which the stats screen shows as a running line.
- Deleting the model first cancels this work, so the engine lock is free within about a second instead of waiting up to the full run.
### Public API
- `StudyInsightWork` — the `InsightScheduler` implementation: `schedule()`, `cancel()`, `state()` (idle or generating) and the `stamp` (model commit and output version).
### Guarantees
- The lock and release rules the worker relies on are fixed by `EngineSlotTest`; cancel behaviour by `CancellableTest` (both in `ai:engine`). There is no unit test of the worker itself; its on-device check is `InsightEvalTest` in `:app` (synthetic digests against the real model).
### Not covered
- The plan thresholds (3 sessions, 20 events, 24 hours, 28 days) — `domain` stats (`InsightPlan`).
- Turning "model ready and records on" into schedule or cancel — `:app` (`InsightScheduleSync`), not in this module.
- Writing the text — `ai:insight-generation`; storing it — `database`.
- A retry button for a failed summary: none.
