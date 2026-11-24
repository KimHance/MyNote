# Decisions

### D-37 · 2026-10 · ST-1
- decision: A session is one visit with the reader on top and the app visible; stopping the app closes it and it is written then, with no periodic checkpoint; time the quiz covers the reader is not reading time.
- why: Closing on stop already saves before the screen turns off or the app is swiped away, so only a foreground crash loses a session; solving a quiz is not reading.
- rejected: Checkpoint writes during a session; counting quiz time as reading.
- evidence: `core/domain/.../model/ReadSessionTracker.kt`, `ReadSessionTrackerTest`.

### D-39 · 2026-10 · ST-2
- decision: Progress is the seen share ((value plus viewport) over (max plus viewport)); the maximum per session is held only by the tracker, and the shell sends the current share whenever it changes.
- why: Plain scroll position reads 0% for a short note that fits on one screen; a per-note maximum held in the shell made a resumed session save 0% when the app returned from the background.
- rejected: Scroll position over maximum; a shell-held per-note maximum sent only when it rises.
- evidence: `app/.../shell/ReaderScrollEffects.kt`, `ReadSessionTrackerTest` "보임 재개 세션은 그 노트의 마지막 비율에서 시작한다".

### D-41 · 2026-10 · ST-4, ST-13
- decision: Bookmark events are only user actions; undoing a removal deletes that removal event; sync cleanup writes nothing; bookmarks made before logging began are not back-filled, so earlier buckets are drawn dashed and the "not opened for long" list uses the later of the added time and the first record.
- why: Writing an "add" on undo would turn one slip into an add and a remove; no removal history exists for the past, so adding only past additions would skew the trend; a note read before logging began must not be called unread.
- rejected: Recording undo as an add; back-filling old bookmarks.
- evidence: `RecordBookmarkEventUseCase`, `StudyStatsCalculatorTest` "오래 안 본 북마크의 보조 줄 세 가지", `FeatureSignalsTest`.

### D-42 · 2026-10 · ST-5
- decision: The three record use cases check the "keep study log" setting in one place; turning it off keeps existing records and shows them with a notice.
- why: A check at each call site is eventually missed; switching off and deleting are different intents.
- rejected: Checking at every caller; deleting records when switched off.
- evidence: `StudyLogUseCaseTest` "기록이 꺼져 있으면 저장소를 한 번도 부르지 않는다", `RecordBookmarkEventUseCase`.

### D-43 · 2026-10 · ST-6
- decision: Clearing the log also deletes summaries, patterns and worker status, and raises an epoch so a run in flight drops its result; deleting the model does not clear the caches.
- why: Summaries derive from records, so keeping them would keep a sentence about deleted content; a re-downloaded model makes the same caches valid again.
- rejected: Clearing only the records.
- evidence: `ClearStudyLogUseCase`, `StudyLogUseCaseTest`.
