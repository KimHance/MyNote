---
units: [quiz:session, quiz:overlay, quiz:cta]
plans: [002-quiz, 003-model-download, 005-stats, 007-repo-connect, 012-feature-modules]
decisions: [QZ-D6, QZ-D7, D-40]
---
## session — Quiz session
### Behaviour
- The quiz is in exactly one state: idle, setup, loading, solving, result or failed; it lives only in memory in the Activity-scoped view model (012-feature-modules: FM-7).
- Rotation, folding and window-class changes keep the quiz where it is (the activity is not recreated); process death loses it and the app opens on home (012-feature-modules: FM-K4).
- The setup offers 3, 4 or 5 questions and starts at 4; choosing a count changes only the setup (002-quiz: QZ-3).
- Opening the setup through `OpenSetup` (the reader-end card) always shows it, cancelling any generation, auto-advance timer and pending undo first.
- The setup request signal (stats retry, a reader opened with a quiz request) opens the setup only when the model is ready; otherwise it does nothing (002-quiz: QZ-4).
- A freshly created view model ignores setup requests and close signals raised before it existed (002-quiz: QZ-4) (012-feature-modules: FM-6).
- Create starts loading for the chosen note and maximum; while a generation is still running, another create or retry is ignored.
- Generation is cut after 60 seconds and fails as Timeout; a parse failure is Parse, an empty result is Empty, any other error is Engine (002-quiz: QZ-10, QZ-17).
- Fewer questions than asked still start solving with a partial flag; zero questions is an Empty failure (002-quiz: QZ-14).
- Retry repeats the failed note and maximum; retry-with-fewer asks for at most 3 questions; neither changes how the model samples, so a same-conditions retry gives nearly the same output (002-quiz: QZ-17).
- A pick is graded once per question: the first valid choice index is recorded as picked and marked right or wrong; later taps and indexes outside the choices change nothing (002-quiz: QZ-12).
- Next before an answer does nothing; next on the last answered question gives the result with one mark per question (002-quiz: QZ-12, QZ-16).
- A correct answer advances by itself after 3,000 ms when auto advance is enabled; a wrong answer always waits for the next button (002-quiz: QZ-12).
- An auto-advance timer only moves the question it was started for; a manual next cancels it, so a late timer never skips a question (012-feature-modules: FM-9).
- Auto advance is off while touch exploration (TalkBack) is on; the next button then always shows (002-quiz: QZ-13).
- Leaving while solving (close, back) goes idle and offers "퀴즈를 그만뒀습니다" with undo for 3.2 s; undo restores the same question, picks and marks and re-arms a running auto advance; after the window the progress is gone (002-quiz: QZ-15).
- Leaving any other state (setup, loading, result, failed) goes idle with no undo; leaving loading cancels the generation (002-quiz: QZ-11, QZ-16).
- The undo notice carries an id; once the shell reports it shown it does not reappear after recreation (012-feature-modules: FM-K11).
- A finished quiz is recorded once, at the move from the last question to the result: note id, the category passed when the setup opened, the time solving started, the finish time, and per question the text, choices, answer and picked index (005-stats: ST-3).
- Quitting, failures, setup and loading record nothing, nor does a quiz whose picks do not match its question count; once started the write is not cancelled by leaving (005-stats: ST-3).
- Every state change writes, in the same step, whether the quiz covers the reader (loading, solving, result and failed cover; idle and setup do not), so the reading session pauses in the same tick; the flag is reset to not covering when the view model is created and cleared (012-feature-modules: FM-6) (005-stats: ST-1).
- A close signal (a connection change that removed notes) cancels the work, drops the undo and goes idle (007-repo-connect: RC-23).
- The model status is exposed for the card; a failing status stream reads as missing.
- An exception while handling an intent is logged through the injected failure log and the state stays as it was.
- No saved quiz history or review, no per-question timer, no hints, no sharing, no network call (the model is on the device), no undo after stopping a generation.
### Public API
- `QuizViewModel` — Hilt view model; state `QuizOverlayState`, input `QuizIntent`.
- `QuizOverlayState` — the quiz state, the undo message and its id, and the model status.
- `QuizUiState` — sealed quiz states (`Idle`, `Setup`, `Loading`, `Solving`, `Result`, `Failed`) with the pure rules `Solving.pick` and `Solving.advance`, `coversReader`, `Result.score` and `rate`, `Failed.Kind` and `canRetryWithFewer`, and the constants `MIN_COUNT`, `DEFAULT_COUNT`, `MAX_COUNT`, `COUNT_CHOICES`, `AUTO_ADVANCE_MILLIS`.
- `QuizIntent` — `OpenSetup(noteId, category)`, `SetCount`, `Generate`, `PickChoice`, `NextQuestion(fromIndex)`, `Retry`, `RetryWithFewer`, `Dismiss`, `UndoExit`, `SignalShown`.
- `QuizQuestionUi` — one question as shown: text, choices, answer index, explanation.
- `QuizAutoAdvancePolicy` — whether auto advance may run; `AccessibilityAutoAdvancePolicy` turns it off under touch exploration.
### Guarantees
- Grading and progress are pure rules on the state: a second tap, an out-of-range choice and next before answering change nothing (`QuizGradingTest`).
- A late timer does not skip a question and undo works only inside its window, on virtual time (`QuizViewModelTest`).
- An attempt is built only on the solving-to-result step and never on quit, failure or mismatched picks (`QuizAttemptRecordTest`).
- A new view model does not replay old setup requests or closes (`FeatureSignalsTest` in this module); the covering flag pauses the reading session (`FeatureSignalsTest` in `app`).
- The auto-advance length is one constant that the timer and the on-screen ring both read.
### Not covered
- Building the note's plain text — `domain:quiz`; the prompt, running the model and parsing its reply — `ai:quiz`; sharing the engine with the stats worker — `ai:engine`.
- Whether an attempt is stored when the study log is off — the record use case in `domain:study-log`.
- Showing the undo snackbar and its action — `app:shell`; the wording "퀴즈를 그만뒀습니다" lives in `ui:notices`.
- Where back reaches the overlay in the back order — `app:shell` (`BackOrderTest`).
### Unconfirmed
- Whether choosing another note in the fixed list pane during a quiz keeps the quiz and only changes the title in the overlay was not checked on a device.

## overlay — Quiz overlay
### Behaviour
- The overlay is drawn by `app:shell` over the content area; it is not on the back stack and moves no screen (002-quiz: QZ-18).
- Back while the quiz is not idle closes it through the same path as the close button (002-quiz: QZ-18).
- Loading, solving, result and failed fill the content area with the app background inside the system-bar insets; with the fixed list pane open the shell starts the overlay after the pane so the pane stays visible (002-quiz: QZ-18).
- The setup dialog is a glass card at most 420 dp wide with a 26 dp radius, 22 dp from the sides and 40 dp above centre, over a scrim that fades in over 220 ms; the card flips in, its body rises in after 130 ms, and closing fades the scrim out over 250 ms and the card over 260 ms (002-quiz: QZ-3).
- The dialog shows "퀴즈 만들기", the note title with "· 기기 안에서 만듦" on one line, "최대 N문제", a three-way radio group "3문제 · 4문제 · 5문제", a hint that a short note may give fewer, and "취소" and "만들기" (002-quiz: QZ-3).
- A scrim tap, "취소" or back closes the setup; "만들기" is throttled to one press per 500 ms (002-quiz: QZ-3).
- Generating shows an orb, a phrase that advances every 900 ms through "노트를 읽는 중", "문제를 만드는 중", "계속 만드는 중", "아직 만드는 중" and stays on the last, the hint "처음 한 번은 10초쯤 걸립니다" and a stop button "그만두기" (002-quiz: QZ-11).
- Generating shows one slot per asked question; one more slot turns done every 2.4 s and the last slot never fills; phrases and slots are decoration not tied to the real progress (002-quiz: QZ-11).
- Stop and close while generating end the quiz with no undo (002-quiz: QZ-11).
- Solving shows a top bar with one progress cell per question (right, wrong, current, pending), "M / N" and a close button, then "문제 N", the question and four choice rows at least 56 dp high; content is at most 560 dp wide (002-quiz: QZ-12).
- After a pick the right choice turns green with a check, a wrong pick red with a cross, the rest dim, and the explanation appears under a "정답" or "오답" line; the view scrolls down to show it (002-quiz: QZ-12).
- While auto advancing the explanation line shows a filling ring and "다음 문제로"; otherwise a 52 dp button "다음 문제" or, on the last question, "결과 보기" appears, throttled to one press per 500 ms (002-quiz: QZ-12).
- A partial quiz shows the band "N문제만 만들었습니다" above every question (002-quiz: QZ-14).
- A new question scrolls to the top and slides in from 26 dp right with a fade over 320 ms; choice rows are keyed by question and choice number so no row flashes the previous question's colour.
- The result shows a 186 dp gauge (132 dp on short windows) that pops in over 420 ms and sweeps to the score over 900 ms, the score over the total, a verdict, one chip per question and one button "노트로 돌아가기" (002-quiz: QZ-16).
- Verdicts: all right "다 맞혔습니다", at least 60 percent "거의 다 맞혔습니다", below "한 번 더 읽어 보세요"; the all-right verdict has its own body line and the other two share one (002-quiz: QZ-16).
- The failure screen shows "문제를 만들지 못했습니다", one fixed reason per kind (Parse, Empty, Engine, Timeout), "다시 만들기" (throttled) and, unless the kind is Empty or the ask is already 3, "최대 3문제로 줄여서 다시"; "노트로 돌아가기" and close end the quiz (002-quiz: QZ-17).
- On short windows (height Compact) generating and result use two columns; solving only tightens its margins.
- With reduced motion the orb stays still, the slot shimmer is off, the advance ring does not fill and the gauge appears at once (002-quiz: QZ-19).
- Choices are radio buttons in one group with the state "정답" or "오답" once revealed; the progress reads "N문제 중 M번째"; the gauge reads "N문제 중 M문제 정답"; each chip reads "N번 정답" or "N번 오답"; right and wrong always pair colour with a mark and text (002-quiz: QZ-20).
### Public API
- `QuizOverlay` — `BoxScope` composable taking the quiz state, the note title, the intent callback, a start inset and an optional glass backdrop.
### Not covered
- Quiz state, grading, timers and undo — see `session`.
- Hiding the top bar on Compact, the start inset value and the overlay's place in the back order — `app:shell`.
- The card at the end of the reader — see `cta`.
### Unconfirmed
- Sizes of the result gauge and dialog come from the code; how they look at large text sizes was not re-checked here.

## cta — Reader-end quiz card
### Behaviour
- One card closes every readable note: a divider, a sparkle tile, "이 노트로 퀴즈 풀기" and one subtitle line; the `notes:reader` leaves the slot empty for a note it could not read in full (002-quiz: QZ-1).
- The card is never hidden by the model state; the subtitle, the chip and the action follow the state (002-quiz: QZ-2) (003-model-download: MD-K9).
- Ready: subtitle "최대 5문제" (the number comes from the maximum count), a chevron, and the action opens the setup (002-quiz: QZ-2).
- Missing: "퀴즈 모델이 필요합니다 · 2.6GB" with the chip "받기"; the action asks for confirmation first (002-quiz: QZ-2).
- Paused: "<GB>GB 받다가 멈췄습니다" with "이어 받기"; failed: "퀴즈 모델을 받지 못했습니다" in the wrong colour with "다시 시도"; both start the download at once (002-quiz: QZ-2).
- Any working download state: "퀴즈 모델 받는 중 · NN%" with a chevron, and the action opens settings; the percentage is the received share while downloading or retrying, and 0 while waiting for Wi-Fi, preparing or verifying (003-model-download: MD-K9).
- The whole row is one button labelled "퀴즈 만들기", throttled to one press per 500 ms.
- An accent ring shines around the card only when the model is ready and motion is not reduced (002-quiz: QZ-19).
### Public API
- `QuizCtaCard` — the card for a `ModelStatus`, reporting the state's `CtaAction` on tap.
### Not covered
- Mapping a model state to subtitle, chip, action, shine and warning — the shared `quizCtaUi` in `ui:model-status`.
- Routing the action (setup, confirm dialog, start download, open settings) — `app:shell-state`; the notification-permission request — `app:shell`.
- The download confirm dialog — `settings:model` (`ModelConfirmDialog`).
- Deciding whether a note is readable — `notes:state`.
