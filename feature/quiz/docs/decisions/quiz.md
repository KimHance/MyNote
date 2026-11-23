# Decisions

### QZ-D6 · 2026-09 · QZ-1, QZ-2, QZ-18
- decision: The entry is a card at the end of the note body, not in the header; the card stays (without its shine, with a need-model line and a download chip) when the model is missing; the quiz is an overlay above the reader and not a navigation destination, and back is received once by the overlay.
- why: Right after reading is the one moment a person wants to be quizzed; hiding the card would leave no way to learn that a model is needed. An overlay never touches the navigator, so it is not on the stack and a stack pop never closes it by accident.
- rejected: A header button; hiding the entry without a model; a navigation destination for the quiz.
- evidence: `feature/quiz` `QuizCtaCard`, `QuizOverlay`; `BackOrderTest` (`FM-10 퀴즈 오버레이가 닫힌다`); `ModelStatusUiTest`.

### QZ-D7 · 2026-09 · QZ-11, QZ-12
- decision: The grading state (chosen answer, marks, picks) lives in the sealed quiz state with pure `pick()` and `advance()` that return `this` when nothing changes; the auto-advance timer lives in the view model and carries the index it was started for; the decoration timers of the loading screen live in that screen and the last slot is never filled; the advance time is one constant shared by timer and ring.
- why: Held in `rememberSaveable` the chosen answer broke on rotation, process death and back restore (an unanswered question showed "wrong"). A timer in a `LaunchedEffect` fires twice and a late one skips a question, which the index check prevents. The loading animation has no real progress, so there is no reason to rebuild app state every second, and filling the last slot would be a false progress bar. The advance time had been copied into two places and the ring drifted from the actual switch.
- rejected: Chosen answer in `rememberSaveable`; timer in the screen's `LaunchedEffect`; loading timers in the view model; filling every slot; a copy of the constant in the screen.
- evidence: `QuizUiState`, `QuizViewModel`, `QuizGeneratingScreen`; `QuizGradingTest`, `QuizViewModelTest` (`FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다`).

### D-40 · 2026-10 · ST-3
- decision: Only attempts that reach the result screen are recorded.
- why: Partial attempts would skew accuracy, and picked choice text is needed for patterns.
- rejected: Recording quit or failed attempts.
- evidence: `QuizAttemptRecordTest`.
