---
units: [domain:study-log]
plans: [005-stats]
decisions: [D-37, D-39, D-41, D-42, D-43]
---
## study-log — Study log
### Behaviour
- A reading session is one visit to a note while it is the top screen and the app is visible. The tracker is a state machine fed by the notes screen with four kinds of signal (top note, app visible, covered, reading progress) and a clock value that holds wall time and elapsed-realtime time (005-stats: ST-1).
- A session closes, and is handed back to the caller as a finished session, when the top note changes or the app stops being visible; the caller writes it. Closing hands back nothing when the session was too short (005-stats: ST-1).
- A finished session has the note id, the category as it was when the visit opened, the wall start and end times, the active time and the largest share seen (005-stats: ST-1).
- Active time counts running time only: while something covers the reader (a quiz) the clock pauses and resumes after; a visit that opens while covered starts paused. A clock that goes backwards counts that stretch as zero (005-stats: ST-1).
- Active time under 30 seconds is dropped and exactly 30 seconds is kept; active time over 60 minutes is cut to 60 minutes while the end time stays the real close time (005-stats: ST-1).
- The same note becoming top again is ignored and the session goes on; a note change closes the old visit and opens the next one with its own category. Becoming invisible and visible again makes two sessions, and a note that became top while invisible starts counting when it becomes visible (005-stats: ST-1).
- Progress is a whole percent clamped to 0 to 100 and is taken only for the current top note; a session keeps the largest value and never lowers it (005-stats: ST-2).
- The tracker remembers the top note's last percent, so a session reopened by the app becoming visible again starts from it instead of 0; the memory is dropped when the top note changes (005-stats: ST-2).
- Three record commands write a session, a quiz attempt and a bookmark event. Each reads the "keep study records" switch at the moment it is called, and when it is off the store is not called at all; existing records stay (005-stats: ST-5).
- A quiz attempt with no answers is not written. An answer is correct when the picked index equals the answer index; an attempt counts asked and correct from its answers (005-stats: ST-3).
- Recording a bookmark event returns the stored row id, or none when the switch is off. Cancelling by id deletes that row whether or not the switch is on; this is how undoing a bookmark removal deletes the removal event instead of adding one (005-stats: ST-4).
- Clearing records runs in this order: cancel the weekly worker, raise the epoch, delete all records and cached summaries and patterns, clear the worker status, and only then reschedule when the model is ready and the switch is on. The raised epoch is what stops a run in flight from saving its result (005-stats: ST-6).
- The rule "build summaries only when the model is ready and the switch is on" is one function used for rescheduling after a clear and by the app's schedule sync (`app:startup`) that schedules or cancels the worker (005-stats: ST-17).
- Pruning removes events older than 365 days, quiz question text older than 90 days (the attempts stay up to 365 days), and summaries and patterns of weeks older than the last 26, this week included. It uses the call-time clock and the device zone (005-stats: ST-7).
- This unit never calls pruning itself: the full prune (events, answer text, summaries and patterns) runs at app start (`app:startup`); after a save the worker (`ai:insight-worker`) prunes only summaries and patterns with the cutoff this unit gives (005-stats: ST-7).
### Public API
- `ReadSessionTracker` (`onTop`, `onVisible`, `onCovered`, `onProgress`; `MIN_ACTIVE_MILLIS`, `MAX_ACTIVE_MILLIS`) and `Moment`.
- `ReadSession`, `QuizAnswer` (`correct`), `QuizAttempt` (`asked`, `correct`), `QuizAttemptSummary`, `BookmarkEvent`, `BookmarkEventKind`, `StudyWindow`, `NoteFacts`.
- `StudyLogRepository` — record, `deleteBookmarkEvent`, observe a window, note facts and first-record time, wrong answers, count, clear, prune and prune answers; implemented in `data:records`.
- `RecordReadSessionUseCase`, `RecordQuizAttemptUseCase`, `RecordBookmarkEventUseCase` (`cancel(eventId)`).
- `ClearStudyLogUseCase` and `insightsWanted(status, studyLogEnabled)`.
- `PruneStudyLogUseCase` with `RETENTION_DAYS`, `ANSWER_RETENTION_DAYS` and `insightCutoff(at, zone)`.
### Guarantees
- Every duration, drop and cap rule, pausing, progress memory and the clock-back case are covered by `ReadSessionTrackerTest`.
- The switch gating, the empty attempt, the clear order and rescheduling conditions and the three retention cutoffs are covered by `StudyLogUseCaseTest`.
### Not covered
- Storing records — `database:study-log`; storing summaries and the worker status — `database:insights`; what the epoch does to a worker run — `ai:insight-worker`.
- Deciding what a screen sends to the tracker (top, visible, covered, progress) and writing sessions when they close — `notes:state`; forwarding the app's start and stop — `app:shell`; the reader progress — `app:shell-chrome`.
- Numbers and lists computed from the records — unit `stats`; weekly summaries — unit `insights`.
- The "keep study records" setting itself — unit `settings`.
