# Decisions

### D-44 · 2026-10 · ST-7
- decision: Pruning runs at app start from the app module, and the worker prunes summaries and patterns again right after it saves; events are kept 365 days, quiz answer text 90 days, summaries and patterns 26 weeks.
- why: The worker does not run without a model, so leaving event pruning to it would let records of users without a model grow forever; the pattern view reads only the last 28 days of wrong answers, and answer text is the bulk of the storage.
- rejected: Pruning only inside the worker; one 365-day limit for answer text.
- evidence: `app/.../StudyLogMaintenance.kt`, `PruneStudyLogUseCase`, `StudyLogUseCaseTest` "문항 텍스트는 90일 넘은 것만 지우고 시도는 365일까지 남긴다".

### BR-D1 · 2026-10 · BR-1
- decision: The brand is direction A (a lit lamp): an m mark made of two arched windows, window frames in the app accent colour, a night background, and light (a daylight gradient) only in the right window.
- why: Owner decision. Of the three directions it leaves the fewest seams with the app's design tokens.
- rejected: Directions B (highlighter) and C (reading-aloud mark), the other two brand directions that were drawn.
- evidence: `labs/brand-design/assets/a-lamp`, `labs/brand-design/assets/b-highlighter`, `labs/brand-design/assets/c-hoedok`; `app/src/main/res/drawable/ic_launcher_foreground.xml`

### BR-D10 · 2026-10 · BR-4, BR-8
- decision: The splash is held only until the lamp animation ends (1000 ms or less). It does not wait for the start decision or for sync data, and the title font does not extend the wait past the lamp's end; if the first app frame comes later, the fonts that have arrived by then are used.
- why: So that starting does not feel slow.
- rejected: Holding the splash until data or the title font is ready.
- evidence: `app/src/main/java/com/hancekim/mynote/SplashExit.kt`; `app/src/androidTest/java/com/hancekim/mynote/SplashWaitTest.kt`

### BR-D12 · 2026-10 · BR-4
- decision: A splash for a start from outside the launcher (a notification, for example) follows the platform default. The setting that always shows the icon is not used.
- why: That setting does not exist at the minimum SDK (API 32).
- rejected: The platform setting that always shows the icon.
- evidence: `app/src/main/res/values/themes.xml`

### BR-D14 · 2026-10 · BR-4
- decision: On a warm start where the previous screen of the same task is still alive, the app does not lift the splash; the system's default lift takes over at the app's first frame, even when the lamp has not finished. In every other cold or warm start the app draws the lift.
- why: When the app handled it, the system took back the mark it had handed over as the previous screen was destroyed and only a blank night background remained; the system lift is better than that.
- rejected: The app drawing the lift in that warm start as well.
- evidence: `app/src/main/java/com/hancekim/mynote/MainActivity.kt`; `app/src/androidTest/java/com/hancekim/mynote/LiveTasksTest.kt`

### LN-D17 · 2026-10 · LN-8
- decision: A shared file is accepted only when its address scheme is literally `content` and does not point at this app's own provider; a filtered share is not read and is reported as the existing "unsupported kind" reason.
- why: It closes the way for another app to make this app read its own files with this app's permission (security convention).
- rejected: Accepting other schemes or this app's own provider, and adding a new reason kind for the filtered share.
- evidence: `app/src/main/java/com/hancekim/mynote/ShareReceiverActivity.kt`, `app/src/test/java/com/hancekim/mynote/ShareRequestTest.kt`, `EditorViewModelTest` (`LN-8 트램펄린이 거른 파일 공유는 …`).
