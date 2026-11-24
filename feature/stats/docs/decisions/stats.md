# Decisions

### D-33 · 2026-10 · ST-9, ST-15
- decision: Statistics has its own ViewModel obtained with the Activity as owner; period and section live in the saved state; the aggregate flow is attached only while Stats is the top destination (past summaries only while that is on top) and is never shared eagerly.
- why: The notes ViewModel already carries the screens, the quiz and the snackbars; the Activity owner keeps period and section across a detour to the reader, and the stack gate plus on-demand sharing stops Room queries while another screen is shown.
- rejected: Extending the notes ViewModel; sharing the aggregate with an eager "lazily" policy (it would run until the app ends).
- evidence: `feature/stats/.../StatsViewModel.kt`, `feature/stats/AGENTS.md` ("집계는 통계가 맨 위일 때만 돈다"), `StatsViewModelTest`.

### D-62 · 2026-10 · ST-14
- decision: The screen has a loading state before the first aggregate and a log-off state with a notice; nothing is drawn while loading.
- why: Drawing empty values first flashes the "no data" hint; without a notice the owner cannot tell why nothing accumulates after switching off.
- rejected: Drawing empty values until data arrives.
- evidence: `StatsUiState.content` stays null until the first aggregate and `StatsScreen` draws nothing then; the log-off notice is `StatsNotice` in `StatsHeader.kt`. No test covers either state.
